package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;
import com.example.paymentservice.dto.payme.PaymeRequest;
import com.example.paymentservice.dto.payme.PaymeResponse;
import com.example.paymentservice.service.PaymeMerchantService;
import com.example.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymeMerchantService paymeMerchantService;

    /**
     * Инициация платежа через внутренний API
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(@Valid @RequestBody PaymentInitiationRequest request) {
        log.info("Initiating payment for order: {}, amount: {}", request.getOrderId(), request.getAmount());
        PaymentInitiationResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Обработка callback от платежных провайдеров
     */
    @PostMapping("/callback/{provider}")
    public ResponseEntity<Void> handleCallback(@PathVariable String provider,
                                               @RequestBody Map<String, Object> callbackData) {
        log.info("Received callback from provider: {}, data: {}", provider, callbackData);
        paymentService.handlePaymentCallback(provider, callbackData);
        return ResponseEntity.ok().build();
    }

    /**
     * Payme Merchant API endpoint
     * Основной endpoint для обработки запросов от Payme
     */
    @PostMapping("/payme")
    public ResponseEntity<PaymeResponse> handlePaymeRequest(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody PaymeRequest request) {

        log.info("Received Payme request: method={}, id={}", request.getMethod(), request.getId());

        PaymeResponse response = paymeMerchantService.handleRequest(authorization, request);

        log.info("Sending Payme response for method: {}, id: {}", request.getMethod(), request.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Получение статуса платежа
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable Long paymentId) {
        log.info("Getting payment status for: {}", paymentId);
        Map<String, Object> status = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(status);
    }

    /**
     * Получение информации о платеже по orderId
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentByOrderId(@PathVariable Long orderId) {
        log.info("Getting payment for order: {}", orderId);
        Map<String, Object> paymentInfo = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(paymentInfo);
    }

    /**
     * Отмена платежа
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) Map<String, String> cancelRequest) {

        String reason = cancelRequest != null ? cancelRequest.get("reason") : "Cancelled by user";
        log.info("Cancelling payment: {}, reason: {}", paymentId, reason);

        Map<String, Object> result = paymentService.cancelPayment(paymentId, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint для платежного сервиса
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        log.debug("Health check requested");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "payment-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    /**
     * Получение истории платежей по orderId
     */
    @GetMapping("/order/{orderId}/history")
    public ResponseEntity<Map<String, Object>> getPaymentHistory(@PathVariable Long orderId) {
        log.info("Getting payment history for order: {}", orderId);
        Map<String, Object> history = paymentService.getPaymentHistory(orderId);
        return ResponseEntity.ok(history);
    }

    /**
     * Refund endpoint для возврата средств
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Map<String, Object>> refundPayment(
            @PathVariable Long paymentId,
            @RequestBody Map<String, String> refundRequest) {

        String reason = refundRequest.get("reason");
        String amountStr = refundRequest.get("amount");

        log.info("Processing refund for payment: {}, reason: {}, amount: {}",
                paymentId, reason, amountStr);

        Map<String, Object> result = paymentService.refundPayment(paymentId, reason, amountStr);
        return ResponseEntity.ok(result);
    }
}