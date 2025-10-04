package order_service.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.order.dto.BookRequestDto;
import order_service.order.dto.BookResponseDto;
import order_service.order.model.BookAvailabilityStatus;
import order_service.order.services.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Book Management", description = "APIs for managing books")
public class BookController {

    private final BookService bookService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create a new book", description = "Create a new book (Admin only)")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponseDto createBook(@Valid @RequestBody BookRequestDto bookRequestDto) {
        log.info("📘 Creating new book: {}", bookRequestDto.getTitle());
        return bookService.createBook(bookRequestDto);
    }

    @GetMapping
    @Operation(summary = "Get all books", description = "Get paginated list of all available books")
    public Page<BookResponseDto> getAllBooks(Pageable pageable) {
        log.debug("📚 Fetching all books");
        return bookService.getAllBooks(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Get book details by ID")
    public BookResponseDto getBookById(@PathVariable Long id) {
        log.debug("🔍 Fetching book by ID: {}", id);
        return bookService.getBookById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update book", description = "Update book details (Admin only)")
    public BookResponseDto updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequestDto bookRequestDto) {
        log.info("✏️ Updating book with ID: {}", id);
        return bookService.updateBook(id, bookRequestDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete book", description = "Delete book (Admin only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id) {
        log.info("🗑️ Deleting book with ID: {}", id);
        bookService.deleteBook(id);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Deactivate book", description = "Deactivate book (Admin only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateBook(@PathVariable Long id) {
        log.info("⏸️ Deactivating book with ID: {}", id);
        bookService.updateAvailabilityStatus(id, BookAvailabilityStatus.INACTIVE);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Activate book", description = "Activate book (Admin only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateBook(@PathVariable Long id) {
        log.info("▶️ Activating book with ID: {}", id);
        bookService.updateAvailabilityStatus(id, BookAvailabilityStatus.ACTIVE);
    }

    @GetMapping("/search")
    @Operation(summary = "Search books", description = "Search books by title or author")
    public Page<BookResponseDto> searchBooks(
            @RequestParam String query,
            Pageable pageable) {
        log.debug("🔎 Searching books with query: {}", query);
        return bookService.searchBooks(query, pageable);
    }
}