package com.example.paymentservice.service;

import com.example.paymentservice.service.impl.OrderServiceClientImpl;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderServiceClient {

    record ItemDetail(
            String title,
            BigDecimal price,
            Integer count,
            String code,
            String packageCode,
            Integer vatPercent,
            BigDecimal totalPrice,
            String category
    ) {}

    record OrderDetail(
            Long orderId,
            String orderNumber,
            BigDecimal totalAmount,
            BigDecimal deliveryAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String currency,
            String status,
            Long userId,
            String userName,
            String userPhone,
            String userEmail,
            String deliveryAddress,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<ItemDetail> items
    ) {}

    record OrderResponse(
            Long id,
            String orderNumber,
            BigDecimal totalAmount,
            BigDecimal deliveryAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String currency,
            String status,
            Long userId,
            String userName,
            String userPhone,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<ItemDetail> items
    ) {}

    record OrderExistsResponse(Boolean exists, String status) {}

    record OrderValidationResponse(
            Boolean valid,
            String message,
            BigDecimal amount,
            String currency,
            String status
    ) {}

    record StatusUpdateRequest(String status, String reason, String updatedBy) {}

    record CancelOrderRequest(String reason, String cancelledBy) {}

    record PaymentNotificationRequest(
            String paymentId,
            String transactionId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String paymentStatus,
            LocalDateTime paymentDate
    ) {}

    record PaymentNotificationResponse(
            Boolean success,
            String message,
            String orderStatus,
            String notificationId
    ) {}


    Mono<OrderDetail> getOrderDetailsForFiscalization(Long orderId);


    Mono<Boolean> checkOrderExists(Long orderId);


    Mono<OrderExistsResponse> checkOrderExistsWithDetails(Long orderId);


    Mono<OrderValidationResponse> validateOrderForPayment(Long orderId);


    Mono<OrderResponse> getOrderById(Long orderId);


    Mono<BigDecimal> getOrderAmount(Long orderId);


    Mono<Void> updateOrderStatus(Long orderId, StatusUpdateRequest status);


    Mono<Void> updateOrderStatus(Long orderId, String status);


    Mono<Void> cancelOrder(Long orderId, CancelOrderRequest cancelRequest);


    Mono<Void> cancelOrder(Long orderId, String reason);


    Mono<PaymentNotificationResponse> notifyPayment(Long orderId, PaymentNotificationRequest paymentRequest);


    Mono<PaymentNotificationResponse> confirmPayment(Long orderId, PaymentNotificationRequest paymentRequest);


    Mono<Void> refundOrder(Long orderId, String reason, BigDecimal amount);

    @PostExchange("/{orderId}/refund")
    Mono<Void> refundOrder(@PathVariable("orderId") Long orderId,
                           @RequestBody OrderServiceClientImpl.RefundRequest refundRequest);


    Mono<Boolean> canRefundOrder(Long orderId);


    Mono<List<OrderStatusHistory>> getOrderStatusHistory(Long orderId);

    record OrderStatusHistory(
            String status,
            String reason,
            String changedBy,
            LocalDateTime changedAt
    ) {}
}