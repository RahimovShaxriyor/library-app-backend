package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;
import java.util.Map;

public interface  PaymentService  {
PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request);

void handlePaymentCallback(String provider, Map<String, String> callbackData);
}
