package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;

import java.util.Map;

public interface PaymentService {

    PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request);

    void handlePaymentCallback(String provider, Map<String, Object> callbackData);

    Map<String, Object> getPaymentStatus(Long paymentId);

    Map<String, Object> getPaymentByOrderId(Long orderId);

    Map<String, Object> cancelPayment(Long paymentId, String reason);

    Map<String, Object> getPaymentHistory(Long orderId);

    Map<String, Object> refundPayment(Long paymentId, String reason, String amountStr);
}

