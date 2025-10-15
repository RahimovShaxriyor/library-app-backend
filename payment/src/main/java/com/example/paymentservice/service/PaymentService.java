package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;

import java.util.Map;

public interface PaymentService {

    PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request, String idempotencyKey);

    void handlePaymentCallback(String provider, Map<String, Object> callbackData);

    Map<String, Object> getPaymentStatus(Long paymentId);

    Map<String, Object> getPaymentByOrderId(Long orderId);

    Map<String, Object> cancelPayment(Long paymentId, String reason);

    Map<String, Object> getPaymentHistory(Long orderId);

    Map<String, Object> refundPayment(Long paymentId, String reason, String amountStr, String idempotencyKey);

    Map<String, Object> getHealthInfo();

    Map<String, Object> getAvailableProviders();

    boolean validateIdempotencyKey(String key, String operationType);

    void cleanupExpiredIdempotencyKeys();

    Map<String, Object> getPaymentMetrics();
}