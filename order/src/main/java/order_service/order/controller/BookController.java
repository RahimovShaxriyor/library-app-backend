package order_service.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.order.dto.BookRequestDto;
import order_service.order.dto.BookResponseDto;
import order_service.order.dto.BookStatistics;
import order_service.order.model.BookAvailabilityStatus;
import order_service.order.services.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Book Management", description = "APIs for managing the book catalog")
public class BookController {

    private final BookService bookService;

    @GetMapping("/search")
    @Operation(summary = "Search books", description = "Search books by title or author.")
    public Page<BookResponseDto> searchBooks(
            @Parameter(description = "Search query for title or author") @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        log.debug("Searching books with query: {}", query);
        return bookService.searchBooks(query, pageable);
    }

    @GetMapping
    @Operation(summary = "Get all available books", description = "Get a paginated list of all books available for ordering.")
    public Page<BookResponseDto> getAllBooks(
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        log.debug("Fetching all available books");
        return bookService.getAllBooks(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Get details for a specific book by its ID.")
    public BookResponseDto getBookById(
            @Parameter(description = "ID of the book to be retrieved") @PathVariable Long id) {
        log.debug("Fetching book by ID: {}", id);
        return bookService.getBookById(id);
    }

    // ==================== ADMIN ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create a new book (Admin)", description = "Create a new book in the catalog.")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponseDto createBook(@Valid @RequestBody BookRequestDto bookRequestDto) {
        log.info("Admin creating new book: {}", bookRequestDto.getTitle());
        return bookService.createBook(bookRequestDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update a book (Admin)", description = "Update the details of an existing book.")
    public BookResponseDto updateBook(
            @Parameter(description = "ID of the book to be updated") @PathVariable Long id,
            @Valid @RequestBody BookRequestDto bookRequestDto) {
        log.info("Admin updating book with ID: {}", id);
        return bookService.updateBook(id, bookRequestDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Soft delete a book (Admin)", description = "Soft delete a book, marking it as DELETED.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@Parameter(description = "ID of the book to be deleted") @PathVariable Long id) {
        log.info("Admin soft deleting book with ID: {}", id);
        bookService.deleteBook(id);
    }

    @PatchMapping("/{id}/status/{status}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update book availability status (Admin)", description = "Update the availability status of a book (e.g., ACTIVE, INACTIVE).")
    public ResponseEntity<Void> updateBookStatus(
            @Parameter(description = "ID of the book") @PathVariable Long id,
            @Parameter(description = "New availability status") @PathVariable BookAvailabilityStatus status) {
        log.info("Admin updating status for book ID: {} to {}", id, status);
        bookService.updateAvailabilityStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get books by status (Admin)", description = "Get a paginated list of books filtered by a specific availability status.")
    public Page<BookResponseDto> getBooksByStatus(
            @Parameter(description = "The status to filter by") @PathVariable BookAvailabilityStatus status,
            Pageable pageable) {
        log.debug("Admin fetching books by status: {}", status);
        return bookService.getBooksByStatus(status, pageable);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get book catalog statistics (Admin)", description = "Get overall statistics for the book catalog.")
    public BookStatistics getBookStatistics() {
        log.debug("Admin requesting book statistics");
        return bookService.getBookStatistics();
    }
}
