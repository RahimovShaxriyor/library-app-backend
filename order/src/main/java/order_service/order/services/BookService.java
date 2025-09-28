package order_service.order.services;

import order_service.order.dto.BookRequestDto;
import order_service.order.dto.BookResponseDto;
import order_service.order.model.BookAvailabilityStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {

    @CacheEvict(value = "books", allEntries = true)
    BookResponseDto createBook(BookRequestDto bookRequestDto);

    @Cacheable(value = "books", key = "#id")
    BookResponseDto getBookById(Long id);

    @Cacheable("books")
    Page<BookResponseDto> getAllBooks(Pageable pageable);

    @CacheEvict(value = "books", allEntries = true)
    BookResponseDto updateBook(Long id, BookRequestDto bookRequestDto);

    @CacheEvict(value = "books", allEntries = true)
    void deleteBook(Long id);


    @CacheEvict(value = "books", allEntries = true)
    void updateAvailabilityStatus(Long id, BookAvailabilityStatus status);
}