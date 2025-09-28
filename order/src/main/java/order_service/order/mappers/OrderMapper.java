package order_service.order.mappers;

import order_service.order.dto.OrderResponseDto;
import order_service.order.dto.OrderItemResponseDto;
import order_service.order.model.Order;
import order_service.order.model.OrderItem;
import org.mapstruct.Mapper;


import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {


    OrderResponseDto toOrderResponseDto(Order order);


    List<OrderItemResponseDto> toOrderItemResponseDtoList(List<OrderItem> items);
}