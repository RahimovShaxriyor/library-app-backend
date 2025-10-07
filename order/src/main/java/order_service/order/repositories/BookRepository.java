package order_service.order.repositories;

import order_service.order.model.Book;
import order_service.order.model.BookAvailabilityStatus;
import order_service.order.model.StockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Book> findByIdForUpdate(Long id);

    boolean existsByTitleAndAuthor(String title, String author);

    boolean existsByTitleAndAuthorAndIdNot(String title, String author, Long id);

    Page<Book> findByAvailabilityStatusNot(BookAvailabilityStatus status, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND b.availabilityStatus != :excludedStatus")
    Page<Book> findByNameContainingIgnoreCaseOrAuthorContainingIgnoreCaseAndAvailabilityStatusNot(
            @Param("query") String query,
            @Param("excludedStatus") BookAvailabilityStatus excludedStatus,
            Pageable pageable);

    Page<Book> findByAvailabilityStatus(BookAvailabilityStatus status, Pageable pageable);

    Page<Book> findByAuthorContainingIgnoreCaseAndAvailabilityStatusNot(
            String author, BookAvailabilityStatus excludedStatus, Pageable pageable);

    Page<Book> findByAvailabilityStatusAndQuantityLessThanEqual(
            BookAvailabilityStatus status, int quantity, Pageable pageable);

    Page<Book> findByAvailabilityStatusAndQuantityGreaterThan(
            BookAvailabilityStatus status, int quantity, Pageable pageable);

    long countByAvailabilityStatus(BookAvailabilityStatus status);

    long countByAvailabilityStatusAndStockStatus(BookAvailabilityStatus availabilityStatus, StockStatus stockStatus);

    long countByAvailabilityStatusAndQuantityLessThanEqual(BookAvailabilityStatus status, int quantity);
}

