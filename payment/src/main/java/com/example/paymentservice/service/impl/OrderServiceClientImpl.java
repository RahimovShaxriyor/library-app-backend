package com.example.paymentservice.service.impl;

import com.example.paymentservice.service.OrderServiceClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@HttpExchange(url = "/api/orders", accept = "application/json", contentType = "application/json")
public interface OrderServiceClientImpl extends OrderServiceClient {

    @GetExchange("/{orderId}/fiscalization")
    @Override
    Mono<OrderDetail> getOrderDetailsForFiscalization(@PathVariable("orderId") Long orderId);

    @GetExchange("/{orderId}/exists")
    @Override
    Mono<Boolean> checkOrderExists(@PathVariable("orderId") Long orderId);

    @GetExchange("/{orderId}/exists-details")
    @Override
    Mono<OrderExistsResponse> checkOrderExistsWithDetails(@PathVariable("orderId") Long orderId);

    @GetExchange("/{orderId}/validate-payment")
    @Override
    Mono<OrderValidationResponse> validateOrderForPayment(@PathVariable("orderId") Long orderId);

    @GetExchange("/{orderId}")
    @Override
    Mono<OrderResponse> getOrderById(@PathVariable("orderId") Long orderId);

    @GetExchange("/{orderId}/amount")
    @Override
    Mono<BigDecimal> getOrderAmount(@PathVariable("orderId") Long orderId);

    @PostExchange("/{orderId}/status")
    @Override
    Mono<Void> updateOrderStatus(@PathVariable("orderId") Long orderId,
                                 @RequestBody StatusUpdateRequest status);

    @PostExchange("/{orderId}/status-simple")
    Mono<Void> updateOrderStatus(@PathVariable("orderId") Long orderId,
                                 @RequestBody String status);

    @PostExchange("/{orderId}/cancel")
    @Override
    Mono<Void> cancelOrder(@PathVariable("orderId") Long orderId,
                           @RequestBody CancelOrderRequest cancelRequest);

    @PostExchange("/{orderId}/cancel-simple")
    Mono<Void> cancelOrder(@PathVariable("orderId") Long orderId,
                           @RequestBody String reason);

    @PostExchange("/{orderId}/payment-notification")
    @Override
    Mono<PaymentNotificationResponse> notifyPayment(@PathVariable("orderId") Long orderId,
                                                    @RequestBody PaymentNotificationRequest paymentRequest);

    @PostExchange("/{orderId}/payment-confirmation")
    @Override
    Mono<PaymentNotificationResponse> confirmPayment(@PathVariable("orderId") Long orderId,
                                                     @RequestBody PaymentNotificationRequest paymentRequest);

    @GetExchange("/{orderId}/can-refund")
    @Override
    Mono<Boolean> canRefundOrder(@PathVariable("orderId") Long orderId);

    @GetExchange("/{orderId}/status-history")
    @Override
    Mono<List<OrderStatusHistory>> getOrderStatusHistory(@PathVariable("orderId") Long orderId);

    record RefundRequest(String reason, BigDecimal amount, String initiatedBy) {}
}