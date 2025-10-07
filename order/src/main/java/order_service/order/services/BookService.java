package order_service.order.services;

import order_service.order.dto.BookRequestDto;
import order_service.order.dto.BookResponseDto;
import order_service.order.dto.BookStatistics;
import order_service.order.model.BookAvailabilityStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {

    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor", "bookStatistics"}, allEntries = true)
    BookResponseDto createBook(BookRequestDto bookRequestDto);

    @Cacheable(value = "books", key = "#id")
    BookResponseDto getBookById(Long id);

    @Cacheable("booksPage")
    Page<BookResponseDto> getAllBooks(Pageable pageable);

    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor", "bookStatistics"}, allEntries = true)
    BookResponseDto updateBook(Long id, BookRequestDto bookRequestDto);

    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor", "bookStatistics"}, allEntries = true)
    void deleteBook(Long id);

    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor", "bookStatistics"}, allEntries = true)
    void updateAvailabilityStatus(Long id, BookAvailabilityStatus status);

    @Cacheable("booksSearch")
    Page<BookResponseDto> searchBooks(String query, Pageable pageable);

    @Cacheable("booksByStatus")
    Page<BookResponseDto> getBooksByStatus(BookAvailabilityStatus status, Pageable pageable);

    @Cacheable("booksByAuthor")
    Page<BookResponseDto> getBooksByAuthor(String author, Pageable pageable);

    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor"}, allEntries = true)
    void clearAllBooksCache();

    @CacheEvict(value = "books", key = "#id")
    void clearBookCache(Long id);

    Page<BookResponseDto> getLowStockBooks(Pageable pageable);

    Page<BookResponseDto> getAvailableBooks(Pageable pageable);

    BookResponseDto updateBookQuantity(Long id, Integer newQuantity);

    @Cacheable("bookStatistics")
    BookStatistics getBookStatistics();

    void bulkUpdateStatus(java.util.List<Long> bookIds, BookAvailabilityStatus status);
}

