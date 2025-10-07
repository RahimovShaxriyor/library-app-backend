package order_service.order.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookMapper bookMapper;

    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.PAID,
            OrderStatus.SHIPPED
    );

    private static final int LOW_STOCK_THRESHOLD = 5;

    @Override
    @Transactional
    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor"}, allEntries = true)
    public BookResponseDto createBook(BookRequestDto bookRequestDto) {
        log.info("📘 Creating new book: {}", bookRequestDto.getTitle());

        validateBookRequest(bookRequestDto);

        if (bookRepository.existsByTitleAndAuthor(bookRequestDto.getTitle(), bookRequestDto.getAuthor())) {
            throw new ValidationException("Book with title '" + bookRequestDto.getTitle() +
                    "' by author '" + bookRequestDto.getAuthor() + "' already exists");
        }

        Book book = bookMapper.toEntity(bookRequestDto);
        book.setAvailabilityStatus(BookAvailabilityStatus.ACTIVE);

        Book savedBook = bookRepository.save(book);
        log.info("✅ Book created successfully with ID: {}", savedBook.getId());

        return bookMapper.toDto(savedBook);
    }

    @Override
    @Cacheable(value = "books", key = "#id")
    public BookResponseDto getBookById(Long id) {
        log.debug("🔍 Fetching book by ID: {}", id);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (book.getAvailabilityStatus() == BookAvailabilityStatus.DELETED) {
            log.warn("⚠️ Attempted to access deleted book with ID: {}", id);
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }

        return bookMapper.toDto(book);
    }

    @Override
    @Cacheable(value = "booksPage", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<BookResponseDto> getAllBooks(Pageable pageable) {
        log.debug("📚 Fetching all books with pagination");

        Page<Book> booksPage = bookRepository.findByAvailabilityStatusNot(
                BookAvailabilityStatus.DELETED, pageable);

        return booksPage.map(bookMapper::toDto);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor"}, allEntries = true)
    public BookResponseDto updateBook(Long id, BookRequestDto bookRequestDto) {
        log.info("✏️ Updating book with ID: {}", id);

        validateBookRequest(bookRequestDto);

        Book bookToUpdate = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (bookRepository.existsByTitleAndAuthorAndIdNot(
                bookRequestDto.getTitle(), bookRequestDto.getAuthor(), id)) {
            throw new ValidationException("Another book with title '" + bookRequestDto.getTitle() +
                    "' by author '" + bookRequestDto.getAuthor() + "' already exists");
        }

        if (!bookToUpdate.getPrice().equals(bookRequestDto.getPrice())) {
            boolean hasActiveOrders = orderItemRepository.existsByBookIdAndOrder_StatusIn(id, (List<OrderStatus>) ACTIVE_ORDER_STATUSES);
            if (hasActiveOrders) {
                throw new ValidationException(
                        "Cannot update price for book '" + bookToUpdate.getTitle() +
                                "' because it is part of active orders. Current price: " + bookToUpdate.getPrice() +
                                ", New price: " + bookRequestDto.getPrice()
                );
            }
        }

        if (bookRequestDto.getQuantity() < 0) {
            throw new ValidationException("Book quantity cannot be negative");
        }

        bookMapper.updateEntityFromDto(bookRequestDto, bookToUpdate);
        Book updatedBook = bookRepository.save(bookToUpdate);

        log.info("✅ Book updated successfully with ID: {}", id);
        return bookMapper.toDto(updatedBook);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor"}, allEntries = true)
    public void deleteBook(Long id) {
        log.info("🗑️ Soft deleting book with ID: {}", id);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        boolean hasActiveOrders = orderItemRepository.existsByBookIdAndOrder_StatusIn(id, (List<OrderStatus>) ACTIVE_ORDER_STATUSES);
        if (hasActiveOrders) {
            throw new ValidationException(
                    "Cannot delete book '" + book.getTitle() +
                            "' because it is part of active orders"
            );
        }

        book.setAvailabilityStatus(BookAvailabilityStatus.DELETED);
        bookRepository.save(book);

        log.info("✅ Book soft deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor"}, allEntries = true)
    public void updateAvailabilityStatus(Long id, BookAvailabilityStatus status) {
        log.info("🔄 Updating availability status for book ID: {} to {}", id, status);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (status == BookAvailabilityStatus.INACTIVE) {
            boolean hasActiveOrders = orderItemRepository.existsByBookIdAndOrder_StatusIn(id, (List<OrderStatus>) ACTIVE_ORDER_STATUSES);
            if (hasActiveOrders) {
                throw new ValidationException(
                        "Cannot deactivate book '" + book.getTitle() +
                                "' because it is part of active orders"
                );
            }
        }

        book.setAvailabilityStatus(status);
        bookRepository.save(book);

        log.info("✅ Availability status updated successfully for book ID: {}", id);
    }

    // ==================== ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ====================

    @Override
    @Cacheable(value = "booksSearch", key = "#query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<BookResponseDto> searchBooks(String query, Pageable pageable) {
        log.debug("🔎 Searching books with query: {}", query);

        if (query == null || query.trim().isEmpty()) {
            return getAllBooks(pageable);
        }

        String searchQuery = "%" + query.trim().toLowerCase() + "%";
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseAndAvailabilityStatusNot(
                        searchQuery, searchQuery, BookAvailabilityStatus.DELETED, pageable)
                .map(bookMapper::toDto);
    }

    @Override
    @Cacheable(value = "booksByStatus", key = "#status + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<BookResponseDto> getBooksByStatus(BookAvailabilityStatus status, Pageable pageable) {
        log.debug("📊 Fetching books by status: {}", status);
        return bookRepository.findByAvailabilityStatus(status, pageable)
                .map(bookMapper::toDto);
    }

    @Override
    @Cacheable(value = "booksByAuthor", key = "#author + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<BookResponseDto> getBooksByAuthor(String author, Pageable pageable) {
        log.debug("👤 Fetching books by author: {}", author);

        if (author == null || author.trim().isEmpty()) {
            throw new ValidationException("Author name cannot be empty");
        }

        return bookRepository.findByAuthorContainingIgnoreCaseAndAvailabilityStatusNot(
                        author, BookAvailabilityStatus.DELETED, pageable)
                .map(bookMapper::toDto);
    }

    @Override
    @CacheEvict(value = {"books", "booksPage", "booksSearch", "booksByStatus", "booksByAuthor"}, allEntries = true)
    public void clearAllBooksCache() {
        log.debug("🧹 Clearing all books cache");
    }

    @Override
    @CacheEvict(value = "books", key = "#id")
    public void clearBookCache(Long id) {
        log.debug("🧹 Clearing cache for book ID: {}", id);
    }

    // ==================== НОВЫЕ МЕТОДЫ ДЛЯ БИЗНЕС-ЛОГИКИ ====================

    @Transactional(readOnly = true)
    public Page<BookResponseDto> getLowStockBooks(Pageable pageable) {
        log.debug("📉 Fetching low stock books");

        return bookRepository.findByAvailabilityStatusAndQuantityLessThanEqual(
                        BookAvailabilityStatus.ACTIVE, LOW_STOCK_THRESHOLD, pageable)
                .map(bookMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BookResponseDto> getAvailableBooks(Pageable pageable) {
        log.debug("🟢 Fetching available books (in stock)");

        return bookRepository.findByAvailabilityStatusAndQuantityGreaterThan(
                        BookAvailabilityStatus.ACTIVE, 0, pageable)
                .map(bookMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = {"books", "booksPage"}, key = "#id")
    public BookResponseDto updateBookQuantity(Long id, Integer newQuantity) {
        log.info("📦 Updating quantity for book ID: {} to {}", id, newQuantity);

        if (newQuantity < 0) {
            throw new ValidationException("Book quantity cannot be negative");
        }

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        book.setQuantity(newQuantity);
        Book updatedBook = bookRepository.save(book);

        log.info("✅ Book quantity updated successfully for ID: {}", id);
        return bookMapper.toDto(updatedBook);
    }

    @Transactional(readOnly = true)
    public BookStatistics getBookStatistics() {
        log.debug("📈 Calculating book statistics");

        long totalBooks = bookRepository.count();
        long activeBooks = bookRepository.countByAvailabilityStatus(BookAvailabilityStatus.ACTIVE);
        long inactiveBooks = bookRepository.countByAvailabilityStatus(BookAvailabilityStatus.INACTIVE);
        long outOfStockBooks = bookRepository.countByAvailabilityStatusAndQuantity(BookAvailabilityStatus.ACTIVE, 0);
        long lowStockBooks = bookRepository.countByAvailabilityStatusAndQuantityLessThanEqual(
                BookAvailabilityStatus.ACTIVE, LOW_STOCK_THRESHOLD);

        return new BookStatistics(totalBooks, activeBooks, inactiveBooks, outOfStockBooks, lowStockBooks);
    }

    @Transactional
    @CacheEvict(value = {"books", "booksPage"}, allEntries = true)
    public void bulkUpdateStatus(List<Long> bookIds, BookAvailabilityStatus status) {
        log.info("🔄 Bulk updating status for {} books to {}", bookIds.size(), status);

        List<Book> books = bookRepository.findAllById(bookIds);

        for (Book book : books) {
            if (status == BookAvailabilityStatus.INACTIVE) {
                boolean hasActiveOrders = orderItemRepository.existsByBookIdAndOrder_StatusIn(
                        book.getId(), (List<OrderStatus>) ACTIVE_ORDER_STATUSES);
                if (hasActiveOrders) {
                    log.warn("⚠️ Skipping book {} due to active orders", book.getId());
                    continue;
                }
            }
            book.setAvailabilityStatus(status);
        }

        bookRepository.saveAll(books);
        log.info("✅ Bulk status update completed for {} books", books.size());
    }

    private void validateBookRequest(BookRequestDto bookRequestDto) {
        if (bookRequestDto.getTitle() == null || bookRequestDto.getTitle().trim().isEmpty()) {
            throw new ValidationException("Book title is required");
        }

        if (bookRequestDto.getAuthor() == null || bookRequestDto.getAuthor().trim().isEmpty()) {
            throw new ValidationException("Book author is required");
        }

        if (bookRequestDto.getPrice() == null || bookRequestDto.getPrice().doubleValue() <= 0) {
            throw new ValidationException("Book price must be positive");
        }

        if (bookRequestDto.getQuantity() == null || bookRequestDto.getQuantity() < 0) {
            throw new ValidationException("Book quantity cannot be negative");
        }

        if (bookRequestDto.getTitle().length() > 255) {
            throw new ValidationException("Book title cannot exceed 255 characters");
        }

        if (bookRequestDto.getAuthor().length() > 255) {
            throw new ValidationException("Book author cannot exceed 255 characters");
        }

        if (bookRequestDto.getDescription() != null && bookRequestDto.getDescription().length() > 1000) {
            throw new ValidationException("Book description cannot exceed 1000 characters");
        }
    }


    public record BookStatistics(
            long totalBooks,
            long activeBooks,
            long inactiveBooks,
            long outOfStockBooks,
            long lowStockBooks
    ) {}
}