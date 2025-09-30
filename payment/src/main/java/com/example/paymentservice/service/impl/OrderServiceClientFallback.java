package com.example.paymentservice.service.impl;

import com.example.paymentservice.service.OrderServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceClientFallback implements OrderServiceClient {

    private final OrderServiceClientImpl orderServiceClient;

    @Override
    public Mono<OrderDetail> getOrderDetailsForFiscalization(Long orderId) {
        return orderServiceClient.getOrderDetailsForFiscalization(orderId)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Order not found for fiscalization: {}", orderId);
                    return Mono.empty();
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error getting order details for fiscalization, orderId: {}", orderId, e);
                    return Mono.just(createFallbackOrderDetail(orderId));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error getting order details, orderId: {}", orderId, e);
                    return Mono.just(createFallbackOrderDetail(orderId));
                });
    }

    @Override
    public Mono<Boolean> checkOrderExists(Long orderId) {
        return orderServiceClient.checkOrderExists(orderId)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Order not found: {}", orderId);
                    return Mono.just(false);
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error checking order existence, orderId: {}", orderId, e);
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<OrderExistsResponse> checkOrderExistsWithDetails(Long orderId) {
        return orderServiceClient.checkOrderExistsWithDetails(orderId)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Order not found with details: {}", orderId);
                    return Mono.just(new OrderExistsResponse(false, "NOT_FOUND"));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error checking order existence with details, orderId: {}", orderId, e);
                    return Mono.just(new OrderExistsResponse(false, "ERROR"));
                })
                .defaultIfEmpty(new OrderExistsResponse(false, "NOT_FOUND"));
    }

    @Override
    public Mono<OrderValidationResponse> validateOrderForPayment(Long orderId) {
        return orderServiceClient.validateOrderForPayment(orderId)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Order not found for validation: {}", orderId);
                    return Mono.just(new OrderValidationResponse(false, "Order not found", BigDecimal.ZERO, "UZS", "NOT_FOUND"));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error validating order for payment, orderId: {}", orderId, e);
                    return Mono.just(new OrderValidationResponse(false, "Validation error", BigDecimal.ZERO, "UZS", "ERROR"));
                })
                .defaultIfEmpty(new OrderValidationResponse(false, "Order not found", BigDecimal.ZERO, "UZS", "NOT_FOUND"));
    }

    @Override
    public Mono<OrderResponse> getOrderById(Long orderId) {
        return orderServiceClient.getOrderById(orderId)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Order not found by ID: {}", orderId);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error getting order by ID: {}", orderId, e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<BigDecimal> getOrderAmount(Long orderId) {
        return orderServiceClient.getOrderAmount(orderId)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Order amount not found for orderId: {}", orderId);
                    return Mono.just(BigDecimal.ZERO);
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error getting order amount, orderId: {}", orderId, e);
                    return Mono.just(BigDecimal.ZERO);
                })
                .defaultIfEmpty(BigDecimal.ZERO);
    }

    @Override
    public Mono<Void> updateOrderStatus(Long orderId, StatusUpdateRequest status) {
        return orderServiceClient.updateOrderStatus(orderId, status)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error updating order status, orderId: {}, status: {}", orderId, status.status(), e);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error updating order status, orderId: {}", orderId, e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> updateOrderStatus(Long orderId, String status) {
        return orderServiceClient.updateOrderStatus(orderId, status)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error updating order status (simple), orderId: {}, status: {}", orderId, status, e);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error updating order status (simple), orderId: {}", orderId, e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> cancelOrder(Long orderId, CancelOrderRequest cancelRequest) {
        return orderServiceClient.cancelOrder(orderId, cancelRequest)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error cancelling order, orderId: {}, reason: {}", orderId, cancelRequest.reason(), e);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error cancelling order, orderId: {}", orderId, e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> cancelOrder(Long orderId, String reason) {
        return orderServiceClient.cancelOrder(orderId, reason)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error cancelling order (simple), orderId: {}, reason: {}", orderId, reason, e);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error cancelling order (simple), orderId: {}", orderId, e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<PaymentNotificationResponse> notifyPayment(Long orderId, PaymentNotificationRequest paymentRequest) {
        return orderServiceClient.notifyPayment(orderId, paymentRequest)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error notifying payment, orderId: {}, paymentId: {}", orderId, paymentRequest.paymentId(), e);
                    return Mono.just(new PaymentNotificationResponse(false, "Notification failed", "PENDING", null));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error notifying payment, orderId: {}", orderId, e);
                    return Mono.just(new PaymentNotificationResponse(false, "Notification failed", "PENDING", null));
                })
                .defaultIfEmpty(new PaymentNotificationResponse(false, "Notification failed", "PENDING", null));
    }

    @Override
    public Mono<PaymentNotificationResponse> confirmPayment(Long orderId, PaymentNotificationRequest paymentRequest) {
        return orderServiceClient.confirmPayment(orderId, paymentRequest)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error confirming payment, orderId: {}, paymentId: {}", orderId, paymentRequest.paymentId(), e);
                    return Mono.just(new PaymentNotificationResponse(false, "Confirmation failed", "PENDING", null));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error confirming payment, orderId: {}", orderId, e);
                    return Mono.just(new PaymentNotificationResponse(false, "Confirmation failed", "PENDING", null));
                })
                .defaultIfEmpty(new PaymentNotificationResponse(false, "Confirmation failed", "PENDING", null));
    }

    @Override
    public Mono<Void> refundOrder(Long orderId, String reason, BigDecimal amount) {
        return orderServiceClient.refundOrder(orderId,
                        new OrderServiceClientImpl.RefundRequest(reason, amount, "payment-service"))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error refunding order, orderId: {}, amount: {}", orderId, amount, e);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error refunding order, orderId: {}", orderId, e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> refundOrder(Long orderId, OrderServiceClientImpl.RefundRequest refundRequest) {
        return null;
    }

    @Override
    public Mono<Boolean> canRefundOrder(Long orderId) {
        return orderServiceClient.canRefundOrder(orderId)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error checking refund possibility, orderId: {}", orderId, e);
                    return Mono.just(false);
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error checking refund possibility, orderId: {}", orderId, e);
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<List<OrderStatusHistory>> getOrderStatusHistory(Long orderId) {
        return orderServiceClient.getOrderStatusHistory(orderId)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error getting order status history, orderId: {}", orderId, e);
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error getting order status history, orderId: {}", orderId, e);
                    return Mono.just(Collections.emptyList());
                })
                .defaultIfEmpty(Collections.emptyList());
    }

    // Вспомогательные методы
    private OrderDetail createFallbackOrderDetail(Long orderId) {
        return new OrderDetail(
                orderId,
                "FALLBACK-" + orderId,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "UZS",
                "UNKNOWN",
                -1L,
                "Unknown User",
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Collections.emptyList()
        );
    }

    // Дополнительные утилитные методы
    public Mono<Boolean> validateOrderWithFallback(Long orderId) {
        return validateOrderForPayment(orderId)
                .map(OrderValidationResponse::valid)
                .defaultIfEmpty(false);
    }

    public Mono<Void> markOrderAsPaidWithFallback(Long orderId, String paymentId, String transactionId, BigDecimal amount) {
        PaymentNotificationRequest paymentRequest = new PaymentNotificationRequest(
                paymentId,
                transactionId,
                amount,
                "UZS",
                "PAYME",
                "SUCCESS",
                LocalDateTime.now()
        );

        return confirmPayment(orderId, paymentRequest)
                .then(updateOrderStatus(orderId, "PAID"))
                .onErrorResume(e -> {
                    log.error("Error marking order as paid, orderId: {}", orderId, e);
                    return Mono.empty();
                });
    }

    public Mono<Void> cancelOrderWithFallback(Long orderId, String reason) {
        return cancelOrder(orderId,
                new CancelOrderRequest(reason, "payment-service"))
                .onErrorResume(e -> {
                    log.error("Error cancelling order with fallback, orderId: {}", orderId, e);
                    return Mono.empty();
                });
    }
}