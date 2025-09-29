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
    public PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider(PaymentProvider.valueOf(request.getProvider().toUpperCase()));

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
    public void handlePaymentCallback(String provider, Map<String, String> callbackData) {
        // This method can be used for other providers or simple callbacks.
        // For Payme, all logic is handled by PaymeMerchantService.
    }
}
