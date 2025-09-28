package order_service.order.services;

import order_service.order.model.OrderItem;
import java.util.List;

public interface PricingService {
    double calculateTotalPrice(List<OrderItem> items);
}