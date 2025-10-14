package com.example.paymentservice.service.impl;

import com.example.paymentservice.config.RabbitMQConfig;
import com.example.paymentservice.dto.click.ClickError;
import com.example.paymentservice.dto.click.ClickRequestDto;
import com.example.paymentservice.dto.click.ClickResponseDto;
import com.example.paymentservice.dto.rabbit.PaymentSuccessEvent;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.ClickMerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickMerchantServiceImpl implements ClickMerchantService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${click.merchant.service_id}")
    private String serviceId;

    @Value("${click.merchant.secret_key}")
    private String secretKey;

    @Override
    @Transactional
    public ClickResponseDto prepare(ClickRequestDto request) {
        if (!isSignatureValid(request, false)) {
            log.warn("Invalid signature for prepare request: {}", request);
            return ClickError.SIGN_CHECK_FAILED.asResponse();
        }

        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(Long.valueOf(request.getMerchantTransId()));
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for orderId: {}", request.getMerchantTransId());
            return ClickError.TRANSACTION_NOT_FOUND.asResponse();
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return ClickError.TRANSACTION_CANCELLED.asResponse();
        }
        if (payment.getAmount().compareTo(request.getAmount()) != 0) {
            return ClickError.INVALID_AMOUNT.asResponse();
        }

        payment.setProviderTransactionId(String.valueOf(request.getClickTransId()));
        paymentRepository.save(payment);

        return new ClickResponseDto(
                request.getClickTransId(),
                request.getMerchantTransId(),
                payment.getId(),
                null,
                0,
                "Success"
        );
    }

    @Override
    @Transactional
    public ClickResponseDto complete(ClickRequestDto request) {
        if (!isSignatureValid(request, true)) {
            log.warn("Invalid signature for complete request: {}", request);
            return ClickError.SIGN_CHECK_FAILED.asResponse();
        }

        Optional<Payment> paymentOpt = paymentRepository.findById(request.getMerchantPrepareId());
        if (paymentOpt.isEmpty()) {
            log.warn("Transaction not found for merchant_prepare_id: {}", request.getMerchantPrepareId());
            return ClickError.TRANSACTION_NOT_FOUND.asResponse();
        }

        Payment payment = paymentOpt.get();

        if (payment.getAmount().compareTo(request.getAmount()) != 0) {
            return ClickError.INVALID_AMOUNT.asResponse();
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return successResponse(request, payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return ClickError.TRANSACTION_CANCELLED.asResponse();
        }

        if (request.getError() != 0) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return ClickError.TRANSACTION_CANCELLED.asResponse();
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        Payment savedPayment = paymentRepository.save(payment);

        // ИСПРАВЛЕНО: Теперь мы передаем Long, а не String
        PaymentSuccessEvent event = new PaymentSuccessEvent(savedPayment.getOrderId(), savedPayment.getId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY, event);
        log.info("Payment successful for orderId: {}. Event published.", savedPayment.getOrderId());

        return successResponse(request, savedPayment);
    }

    private ClickResponseDto successResponse(ClickRequestDto request, Payment payment) {
        return new ClickResponseDto(
                request.getClickTransId(),
                request.getMerchantTransId(),
                payment.getId(),
                payment.getId(),
                0,
                "Success"
        );
    }

    private boolean isSignatureValid(ClickRequestDto request, boolean isComplete) {
        String signStringTemplate = isComplete
                ? "{click_trans_id}{service_id}{secret_key}{merchant_trans_id}{merchant_prepare_id}{amount}{action}{sign_time}"
                : "{click_trans_id}{service_id}{secret_key}{merchant_trans_id}{amount}{action}{sign_time}";

        String signString = signStringTemplate
                .replace("{click_trans_id}", String.valueOf(request.getClickTransId()))
                .replace("{service_id}", serviceId)
                .replace("{secret_key}", secretKey)
                .replace("{merchant_trans_id}", request.getMerchantTransId())
                .replace("{merchant_prepare_id}", isComplete ? String.valueOf(request.getMerchantPrepareId()) : "")
                .replace("{amount}", request.getAmount().toString())
                .replace("{action}", String.valueOf(request.getAction()))
                .replace("{sign_time}", request.getSignTime());

        String generatedSign = DigestUtils.md5Hex(signString);

        return generatedSign.equals(request.getSignString());
    }
}

