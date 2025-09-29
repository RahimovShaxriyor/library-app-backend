package order_service.order.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.order.dto.event.PaymentCancelledEvent;
import order_service.order.dto.event.PaymentSuccessEvent;
import order_service.order.services.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;
    public static final String PAYMENT_SUCCESS_QUEUE = "payment-success-queue";
    public static final String PAYMENT_CANCELLED_QUEUE = "payment-cancelled-queue";

    @RabbitListener(queues = PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent for orderId: {}", event.orderId());
        try {
            orderService.processPayment(event.orderId());
            log.info("Order {} status updated to PAID.", event.orderId());
        } catch (Exception e) {
            log.error("Error processing payment success for orderId: {}", event.orderId(), e);
        }
    }

    @RabbitListener(queues = PAYMENT_CANCELLED_QUEUE)
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        log.info("Received PaymentCancelledEvent for orderId: {}", event.orderId());
        try {
            orderService.cancelOrder(event.orderId());
            log.info("Order {} status updated to CANCELLED.", event.orderId());
        } catch (Exception e) {
            log.error("Error processing payment cancellation for orderId: {}", event.orderId(), e);
        }
    }
}
