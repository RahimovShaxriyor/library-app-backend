package order_service.order.repositories;

import order_service.order.model.Order;
import order_service.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByStatusAndUserId(OrderStatus status, String userId);

    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.createdAt BETWEEN :startDate AND :endDate")
    Page<Order> findByUserIdAndCreatedAtBetween(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :cutoffDate")
    List<Order> findOldOrdersByStatus(
            @Param("status") OrderStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = :status")
    long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.totalPrice > :minAmount ORDER BY o.totalPrice DESC")
    Page<Order> findOrdersWithTotalPriceGreaterThan(
            @Param("minAmount") Double minAmount,
            Pageable pageable);

    // Для админских отчетов
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByUserId(@Param("userId") String userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByIdAndUserId(Long id, String userId);

    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}