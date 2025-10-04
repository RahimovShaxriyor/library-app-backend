package order_service.order.services;

import order_service.order.dto.BookRequestDto;
import order_service.order.dto.BookResponseDto;
import order_service.order.model.BookAvailabilityStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {

    @CacheEvict(value = {"books", "booksPage"}, allEntries = true)
    BookResponseDto createBook(BookRequestDto bookRequestDto);

    @Cacheable(value = "books", key = "#id")
    BookResponseDto getBookById(Long id);

    @Cacheable(value = "booksPage", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    Page<BookResponseDto> getAllBooks(Pageable pageable);

    @CacheEvict(value = {"books", "booksPage"}, allEntries = true)
    BookResponseDto updateBook(Long id, BookRequestDto bookRequestDto);

    @CacheEvict(value = {"books", "booksPage"}, allEntries = true)
    void deleteBook(Long id);

    @CacheEvict(value = {"books", "booksPage"}, allEntries = true)
    void updateAvailabilityStatus(Long id, BookAvailabilityStatus status);

    // Новые методы с кешированием
    @Cacheable(value = "booksSearch", key = "#query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    Page<BookResponseDto> searchBooks(String query, Pageable pageable);

    @Cacheable(value = "booksByStatus", key = "#status + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    Page<BookResponseDto> getBooksByStatus(BookAvailabilityStatus status, Pageable pageable);

    @Cacheable(value = "booksByAuthor", key = "#author + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    Page<BookResponseDto> getBooksByAuthor(String author, Pageable pageable);

    // Методы для очистки кеша
    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor"}, allEntries = true)
    void clearAllBooksCache();

    @CacheEvict(value = "books", key = "#id")
    void clearBookCache(Long id);
}