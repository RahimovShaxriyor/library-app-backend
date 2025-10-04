package order_service.order.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.order.dto.event.PaymentCancelledEvent;
import order_service.order.dto.event.PaymentSuccessEvent;
import order_service.order.exceptions.ResourceNotFoundException;
import order_service.order.exceptions.ValidationException;
import order_service.order.services.OrderService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;

    public static final String PAYMENT_SUCCESS_QUEUE = "payment-success-queue";
    public static final String PAYMENT_CANCELLED_QUEUE = "payment-cancelled-queue";
    public static final String PAYMENT_DLQ = "payment-dlq";

    // Системный пользователь для обработки событий
    private static final String SYSTEM_USER = "payment-service";
    private final UserDetails systemUser = User.builder()
            .username(SYSTEM_USER)
            .password("") // пароль не используется
            .authorities(Collections.emptyList())
            .build();

    @Transactional
    @RabbitListener(queues = PAYMENT_SUCCESS_QUEUE)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("🎯 Received PaymentSuccessEvent for orderId: {}, paymentId: {}",
                event.orderId(), event.paymentId());

        try {
            // Валидация входящего события
            validatePaymentEvent(event);

            // Используем системного пользователя для обработки платежа
            orderService.processPayment(event.orderId(), systemUser);

            log.info("✅ Order {} successfully processed to PAID status. PaymentId: {}",
                    event.orderId(), event.paymentId());

        } catch (ResourceNotFoundException e) {
            log.error("❌ Order not found for PaymentSuccessEvent. OrderId: {}, Error: {}",
                    event.orderId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Order not found: " + e.getMessage(), e);

        } catch (ValidationException e) {
            log.error("⚠️ Validation failed for PaymentSuccessEvent. OrderId: {}, Error: {}",
                    event.orderId(), e.getMessage());
            // Не повторяем для бизнес-логических ошибок
            throw new AmqpRejectAndDontRequeueException("Validation failed: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("💥 Unexpected error processing PaymentSuccessEvent for orderId: {}. Error: {}",
                    event.orderId(), e.getMessage(), e);
            throw e; // Повторяем для технических ошибок
        }
    }

    @Transactional
    @RabbitListener(queues = PAYMENT_CANCELLED_QUEUE)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        log.info("🎯 Received PaymentCancelledEvent for orderId: {}, paymentId: {}, reason: {}",
                event.orderId(), event.paymentId(), event.reason());

        try {
            // Валидация входящего события
            validatePaymentCancellationEvent(event);

            // Используем системного пользователя для отмены заказа
            orderService.cancelOrder(event.orderId(), systemUser);

            log.info("✅ Order {} successfully cancelled. Reason: {}, PaymentId: {}",
                    event.orderId(), event.reason(), event.paymentId());

        } catch (ResourceNotFoundException e) {
            log.error("❌ Order not found for PaymentCancelledEvent. OrderId: {}, Error: {}",
                    event.orderId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Order not found: " + e.getMessage(), e);

        } catch (ValidationException e) {
            log.warn("⚠️ Cannot cancel order. OrderId: {}, Current status might be already finalized. Error: {}",
                    event.orderId(), e.getMessage());
            // Логируем, но не повторяем - возможно заказ уже обработан
            throw new AmqpRejectAndDontRequeueException("Cannot cancel order: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("💥 Unexpected error processing PaymentCancelledEvent for orderId: {}. Error: {}",
                    event.orderId(), e.getMessage(), e);
            throw e;
        }
    }

    // Dead Letter Queue handler для обработки неудачных сообщений
    @RabbitListener(queues = PAYMENT_DLQ)
    public void handleFailedPaymentMessages(Object failedMessage) {
        log.error("💀 Received failed payment message in DLQ: {}", failedMessage);
        // Здесь можно добавить логику для уведомления администратора
        // или сохранения в базу для дальнейшего анализа

        // Пример: сохранение в лог для последующего анализа
        logFailedPaymentMessage(failedMessage);
    }

    // Вспомогательные методы валидации
    private void validatePaymentEvent(PaymentSuccessEvent event) {
        if (event.orderId() == null) {
            throw new ValidationException("Order ID cannot be null in PaymentSuccessEvent");
        }
        if (event.paymentId() == null || event.paymentId().isBlank()) {
            throw new ValidationException("Payment ID cannot be null or empty in PaymentSuccessEvent");
        }
        if (event.orderId() <= 0) {
            throw new ValidationException("Order ID must be positive");
        }
    }

    private void validatePaymentCancellationEvent(PaymentCancelledEvent event) {
        if (event.orderId() == null) {
            throw new ValidationException("Order ID cannot be null in PaymentCancelledEvent");
        }
        if (event.paymentId() == null || event.paymentId().isBlank()) {
            throw new ValidationException("Payment ID cannot be null or empty in PaymentCancelledEvent");
        }
        if (event.reason() == null || event.reason().isBlank()) {
            throw new ValidationException("Cancellation reason cannot be null or empty");
        }
        if (event.orderId() <= 0) {
            throw new ValidationException("Order ID must be positive");
        }
    }

    // Метод для ручного подтверждения обработки (если нужно)
    public void manualPaymentSuccess(Long orderId, String paymentId) {
        PaymentSuccessEvent event = new PaymentSuccessEvent(orderId, paymentId);
        handlePaymentSuccess(event);
    }

    public void manualPaymentCancellation(Long orderId, String paymentId, String reason) {
        PaymentCancelledEvent event = new PaymentCancelledEvent(orderId, paymentId, reason);
        handlePaymentCancelled(event);
    }

    // Вспомогательные методы
    private void logFailedPaymentMessage(Object failedMessage) {
        try {
            // Здесь можно добавить логику для отправки уведомления
            // или сохранения в базу данных для последующего анализа
            log.warn("Failed payment message requires manual intervention: {}", failedMessage);

            // Пример: можно отправить email уведомление администратору
            // notificationService.sendAdminAlert("Failed payment message", failedMessage.toString());

        } catch (Exception e) {
            log.error("Error processing failed payment message: {}", e.getMessage(), e);
        }
    }

    // Метод для получения информации о системном пользователе
    public String getSystemUserId() {
        return SYSTEM_USER;
    }
}