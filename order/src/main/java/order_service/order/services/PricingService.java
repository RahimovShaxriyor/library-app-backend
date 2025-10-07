package order_service.order.services;

import order_service.order.model.OrderItem;
import java.math.BigDecimal;
import java.util.List;

public interface PricingService {
    BigDecimal calculateTotalPrice(List<OrderItem> items);
}
