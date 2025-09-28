package order_service.order.services.impl;

import order_service.order.model.OrderItem;
import order_service.order.services.PricingService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PricingServiceImpl implements PricingService {

    @Override
    public double calculateTotalPrice(List<OrderItem> items) {
        double totalPrice = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();


        return totalPrice;
    }
}