package com.example.paymentservice.service.impl;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentProvider;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${payme.merchant.id}")
    private String merchantId;

    @Value("${payme.checkout.url}")
    private String checkoutUrl;

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider(PaymentProvider.valueOf(request.getProvider().toUpperCase()));
        payment.setCreatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        String params = String.format("m=%s;ac.order_id=%d;a=%d;c=https://example.com/success",
                merchantId,
                savedPayment.getOrderId(),
                savedPayment.getAmount().multiply(new java.math.BigDecimal(100)).longValue()
        );

        String encodedParams = Base64.getEncoder().encodeToString(params.getBytes());
        String paymentUrl = checkoutUrl + "/" + encodedParams;

        return new PaymentInitiationResponse(paymentUrl, savedPayment.getId());
    }

    @Override
    public void handlePaymentCallback(String provider, Map<String, Object> callbackData) {
        // For Payme, all logic is handled by PaymeMerchantService.
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return Map.of("paymentId", payment.getId(), "status", payment.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for orderId: " + orderId));
        return Map.of("paymentId", payment.getId(), "status", payment.getStatus(), "amount", payment.getAmount());
    }

    @Override
    @Transactional
    public Map<String, Object> cancelPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        return Map.of("paymentId", payment.getId(), "status", "CANCELLED", "reason", reason);
    }

    @Override
    public Map<String, Object> getPaymentHistory(Long orderId) {
        // Logic to retrieve payment history
        return Map.of();
    }

    @Override
    public Map<String, Object> refundPayment(Long paymentId, String reason, String amountStr) {
        // Logic for processing refunds
        return Map.of();
    }
}
