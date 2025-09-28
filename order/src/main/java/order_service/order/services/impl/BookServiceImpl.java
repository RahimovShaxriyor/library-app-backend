package order_service.order.services.impl;

import lombok.RequiredArgsConstructor;
import order_service.order.dto.BookRequestDto;
import order_service.order.dto.BookResponseDto;
import order_service.order.exceptions.ResourceNotFoundException;
import order_service.order.exceptions.ValidationException;
import order_service.order.mappers.BookMapper;
import order_service.order.model.Book;
import order_service.order.model.BookAvailabilityStatus;
import order_service.order.model.OrderStatus;
import order_service.order.repositories.BookRepository;
import order_service.order.repositories.OrderItemRepository;
import order_service.order.services.BookService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookMapper bookMapper;

    @Override
    @Transactional
    @CacheEvict(value = "books", allEntries = true)
    public BookResponseDto createBook(BookRequestDto bookRequestDto) {
        Book book = bookMapper.toEntity(bookRequestDto);
        book.setAvailabilityStatus(BookAvailabilityStatus.ACTIVE);

        Book savedBook = bookRepository.save(book);

        return bookMapper.toDto(savedBook);
    }

    @Override
    @Cacheable(value = "books", key = "#id")
    public BookResponseDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return bookMapper.toDto(book);
    }

    @Override
    @Cacheable("books")
    public Page<BookResponseDto> getAllBooks(Pageable pageable) {
        Page<Book> booksPage = bookRepository.findAll(pageable);
        return booksPage.map(bookMapper::toDto);
    }

    @Override
    @Transactional
    @CacheEvict(value = "books", allEntries = true)
    public BookResponseDto updateBook(Long id, BookRequestDto bookRequestDto) {
        Book bookToUpdate = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (!bookToUpdate.getPrice().equals(bookRequestDto.getPrice())) {
            List<OrderStatus> activeStatuses = List.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.SHIPPED);
            if (orderItemRepository.existsByBookIdAndActiveOrderStatus(id, activeStatuses)) {
                throw new ValidationException("Cannot update the price for a book that is part of an active order.");
            }
        }

        bookMapper.updateEntityFromDto(bookRequestDto, bookToUpdate);

        Book updatedBook = bookRepository.save(bookToUpdate);
        return bookMapper.toDto(updatedBook);
    }

    @Override
    @CacheEvict(value = "books", allEntries = true)
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        book.setAvailabilityStatus(BookAvailabilityStatus.DELETED);
        bookRepository.save(book);
    }

    @Override
    @Transactional
    @CacheEvict(value = "books", allEntries = true)
    public void updateAvailabilityStatus(Long id, BookAvailabilityStatus status) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        book.setAvailabilityStatus(status);
        bookRepository.save(book);
    }
}