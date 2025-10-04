package order_service.order.repositories;

import order_service.order.model.Book;
import order_service.order.model.BookAvailabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Блокировка для конкурентного доступа
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    // Нативные запросы для Oracle
    @Query(value = "SELECT * FROM books WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Book> findByIdForUpdateNative(@Param("id") Long id);

    // Поиск по названию и автору (для проверки дубликатов)
    boolean existsByTitleAndAuthor(String title, String author);

    boolean existsByTitleAndAuthorAndIdNot(String title, String author, Long id);

    // Поиск по статусу доступности
    Page<Book> findByAvailabilityStatus(BookAvailabilityStatus status, Pageable pageable);

    Page<Book> findByAvailabilityStatusNot(BookAvailabilityStatus status, Pageable pageable);

    // Поиск активных книг (не удаленных и активных)
    Page<Book> findByAvailabilityStatusIn(List<BookAvailabilityStatus> statuses, Pageable pageable);

    // Поиск по автору
    Page<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);

    List<Book> findByAuthor(String author);

    // Поиск по названию
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    List<Book> findByTitle(String title);

    // Комбинированный поиск по названию и автору
    @Query("SELECT b FROM Book b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> findByTitleOrAuthorContainingIgnoreCase(@Param("query") String query, Pageable pageable);

    // Поиск с исключением статуса
    @Query("SELECT b FROM Book b WHERE " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) " +
            "AND b.availabilityStatus != :excludedStatus")
    Page<Book> findByTitleOrAuthorContainingIgnoreCaseAndAvailabilityStatusNot(
            @Param("title") String title,
            @Param("author") String author,
            @Param("excludedStatus") BookAvailabilityStatus excludedStatus,
            Pageable pageable);

    Page<Book> findByQuantityGreaterThan(Integer quantity, Pageable pageable);

    Page<Book> findByAvailabilityStatusAndQuantityGreaterThan(
            BookAvailabilityStatus status, Integer quantity, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :minPrice AND :maxPrice")
    Page<Book> findByPriceBetween(
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.price > :minPrice")
    Page<Book> findExpensiveBooks(@Param("minPrice") Double minPrice, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.quantity <= :lowStockThreshold AND b.availabilityStatus = :status")
    Page<Book> findLowStockBooks(
            @Param("lowStockThreshold") Integer lowStockThreshold,
            @Param("status") BookAvailabilityStatus status,
            Pageable pageable);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.availabilityStatus = :status")
    long countByAvailabilityStatus(@Param("status") BookAvailabilityStatus status);

    @Query("SELECT SUM(b.quantity) FROM Book b WHERE b.availabilityStatus = :status")
    Long sumQuantityByAvailabilityStatus(@Param("status") BookAvailabilityStatus status);

    @Query("SELECT AVG(b.price) FROM Book b WHERE b.availabilityStatus = :status")
    Double findAveragePriceByAvailabilityStatus(@Param("status") BookAvailabilityStatus status);

    @Query("SELECT DISTINCT b.author FROM Book b WHERE b.availabilityStatus != :excludedStatus")
    List<String> findDistinctAuthors(@Param("excludedStatus") BookAvailabilityStatus excludedStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id IN :bookIds")
    List<Book> findAllByIdForUpdate(@Param("bookIds") List<Long> bookIds);

    // Обновление количества через @Modifying
    @Query("UPDATE Book b SET b.quantity = b.quantity - :quantity WHERE b.id = :id AND b.quantity >= :quantity")
    int decreaseQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Query("UPDATE Book b SET b.quantity = b.quantity + :quantity WHERE b.id = :id")
    int increaseQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    // Проверка существования книги с достаточным количеством
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Book b WHERE b.id = :id AND b.quantity >= :quantity AND b.availabilityStatus = :status")
    boolean existsByIdAndQuantityGreaterThanEqualAndAvailabilityStatus(
            @Param("id") Long id,
            @Param("quantity") Integer quantity,
            @Param("status") BookAvailabilityStatus status);

    @Query("SELECT b FROM Book b LEFT JOIN OrderItem oi ON b.id = oi.bookId " +
            "WHERE b.availabilityStatus = :status " +
            "GROUP BY b.id " +
            "ORDER BY COUNT(oi.id) DESC")
    Page<Book> findPopularBooks(
            @Param("status") BookAvailabilityStatus status,
            Pageable pageable);
}