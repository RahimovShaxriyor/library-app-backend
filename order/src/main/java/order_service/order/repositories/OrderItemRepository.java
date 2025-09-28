package order_service.order.repositories;

import order_service.order.model.OrderItem;
import order_service.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.bookId = :bookId AND oi.order.status IN :statuses")
    boolean existsByBookIdAndActiveOrderStatus(@Param("bookId") Long bookId, @Param("statuses") List<OrderStatus> statuses);
}