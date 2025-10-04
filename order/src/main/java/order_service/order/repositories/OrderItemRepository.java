package order_service.order.repositories;

import order_service.order.model.OrderItem;
import order_service.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Существующий метод
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.bookId = :bookId AND oi.order.status IN :statuses")
    boolean existsByBookIdAndOrder_StatusIn(@Param("bookId") Long bookId, @Param("statuses") List<OrderStatus> statuses);

    // Альтернативная версия для удобства
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
            "FROM OrderItem oi WHERE oi.bookId = :bookId AND oi.order.status IN :statuses")
    boolean hasActiveOrdersForBook(@Param("bookId") Long bookId, @Param("statuses") List<OrderStatus> statuses);

    // Поиск всех OrderItems по bookId
    List<OrderItem> findByBookId(Long bookId);

    Page<OrderItem> findByBookId(Long bookId, Pageable pageable);

    // Поиск OrderItems по статусу заказа
    List<OrderItem> findByOrder_Status(OrderStatus status);

    Page<OrderItem> findByOrder_Status(OrderStatus status, Pageable pageable);

    // Поиск OrderItems по bookId и статусу заказа
    List<OrderItem> findByBookIdAndOrder_Status(Long bookId, OrderStatus status);

    Page<OrderItem> findByBookIdAndOrder_Status(Long bookId, OrderStatus status, Pageable pageable);

    List<OrderItem> findByOrder_Id(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId ORDER BY oi.id")
    List<OrderItem> findOrderItemsByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order WHERE oi.bookId = :bookId")
    List<OrderItem> findByBookIdWithOrder(@Param("bookId") Long bookId);

    @Query("SELECT oi.bookId, SUM(oi.quantity) FROM OrderItem oi " +
            "WHERE oi.order.status = :status " +
            "GROUP BY oi.bookId")
    List<Object[]> findBookSalesByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.bookId = :bookId AND oi.order.status = :status")
    Long countTotalSoldQuantityByBookIdAndStatus(@Param("bookId") Long bookId, @Param("status") OrderStatus status);

    // Получение самых популярных книг
    @Query("SELECT oi.bookId, oi.name, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi " +
            "WHERE oi.order.status = :status " +
            "GROUP BY oi.bookId, oi.name " +
            "ORDER BY totalSold DESC")
    Page<Object[]> findTopSellingBooks(@Param("status") OrderStatus status, Pageable pageable);

    // Получение общей выручки по книге
    @Query("SELECT SUM(oi.price * oi.quantity) FROM OrderItem oi " +
            "WHERE oi.bookId = :bookId AND oi.order.status = :status")
    BigDecimal calculateTotalRevenueByBookIdAndStatus(@Param("bookId") Long bookId, @Param("status") OrderStatus status);

    // Поиск OrderItems по диапазону дат
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.createdAt BETWEEN :startDate AND :endDate")
    List<OrderItem> findOrderItemsByOrderDateRange(@Param("startDate") java.time.LocalDateTime startDate,
                                                   @Param("endDate") java.time.LocalDateTime endDate);

    // Получение OrderItem с блокировкой для обновления
    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :id")
    Optional<OrderItem> findByIdForUpdate(@Param("id") Long id);

    // Проверка существования OrderItem для книги в конкретном заказе
    boolean existsByBookIdAndOrder_Id(Long bookId, Long orderId);

    // Получение количества OrderItems для книги во всех заказах
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.bookId = :bookId")
    long countByBookId(@Param("bookId") Long bookId);

    List<OrderItem> findByQuantityGreaterThan(Integer quantity);

    Page<OrderItem> findByQuantityGreaterThan(Integer quantity, Pageable pageable);

    // Поиск OrderItems по цене
    @Query("SELECT oi FROM OrderItem oi WHERE oi.price BETWEEN :minPrice AND :maxPrice")
    List<OrderItem> findByPriceBetween(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    // Получение средней цены OrderItem по книге
    @Query("SELECT AVG(oi.price) FROM OrderItem oi WHERE oi.bookId = :bookId")
    Double findAveragePriceByBookId(@Param("bookId") Long bookId);

    // Получение OrderItems с пагинацией и сортировкой
    @Query("SELECT oi FROM OrderItem oi ORDER BY oi.order.createdAt DESC")
    Page<OrderItem> findAllOrderItems(Pageable pageable);

    // Удаление OrderItems по ID заказа
    void deleteByOrder_Id(Long orderId);

    // Подсчет общего количества товаров в заказах по статусу
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.order.status = :status")
    Long getTotalItemsQuantityByOrderStatus(@Param("status") OrderStatus status);
}