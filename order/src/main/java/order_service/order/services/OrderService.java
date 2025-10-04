package order_service.order.services;


import order_service.order.dto.CreateOrderRequestDto;
import order_service.order.dto.OrderResponseDto;
import order_service.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

public interface OrderService {
    OrderResponseDto createOrder(CreateOrderRequestDto request, UserDetails currentUser);
    Page<OrderResponseDto> getOrdersForUser(UserDetails currentUser, Pageable pageable);
    OrderResponseDto processPayment(Long orderId, UserDetails currentUser);
    OrderResponseDto shipOrder(Long orderId);
    OrderResponseDto deliverOrder(Long orderId);
    OrderResponseDto cancelOrder(Long orderId, UserDetails currentUser);
    OrderResponseDto getOrderDetails(Long orderId, UserDetails currentUser);

    // Admin methods
    Page<OrderResponseDto> getAllOrders(Pageable pageable);
    Page<OrderResponseDto> getOrdersByStatus(OrderStatus status, Pageable pageable);
}