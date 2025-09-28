package order_service.order.dto;

import order_service.order.model.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
public class OrderResponseDto {
    private Long id;
    private String userId;
    private Double totalPrice;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDto> items;
}