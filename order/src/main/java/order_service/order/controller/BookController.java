package order_service.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import order_service.order.dto.BookRequestDto;
import order_service.order.dto.BookResponseDto;
import order_service.order.model.BookAvailabilityStatus;
import order_service.order.services.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BookResponseDto> createBook(@Valid @RequestBody BookRequestDto bookRequestDto) {
        BookResponseDto createdBook = bookService.createBook(bookRequestDto);
        return new ResponseEntity<>(createdBook, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<BookResponseDto>> getAllBooks(Pageable pageable) {
        Page<BookResponseDto> books = bookService.getAllBooks(pageable);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> getBookById(@PathVariable Long id) {
        BookResponseDto book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BookResponseDto> updateBook(@PathVariable Long id, @Valid @RequestBody BookRequestDto bookRequestDto) {
        BookResponseDto updatedBook = bookService.updateBook(id, bookRequestDto);
        return ResponseEntity.ok(updatedBook);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deactivateBook(@PathVariable Long id) {
        bookService.updateAvailabilityStatus(id, BookAvailabilityStatus.INACTIVE);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> activateBook(@PathVariable Long id) {
        bookService.updateAvailabilityStatus(id, BookAvailabilityStatus.ACTIVE);
        return ResponseEntity.ok().build();
    }
}