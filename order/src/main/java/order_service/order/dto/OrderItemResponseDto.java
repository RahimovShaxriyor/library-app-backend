package order_service.order.dto;

import lombok.Value;


@Value
public class OrderItemResponseDto {
    Long bookId;
    String name;
    Double price;
    Integer quantity;
}