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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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

    @Value("${click.allowed.ips:}")
    private String allowedIps;

    // In-memory storage for duplicate request tracking (in production use Redis)
    private final Map<String, Long> requestCache = new HashMap<>();

    @Override
    @Transactional
    public ClickResponseDto prepare(ClickRequestDto request, String clientIp) {
        log.info("Click Prepare request from {}: {}", clientIp, request);

        if (!validateIpAddress(clientIp)) {
            log.warn("Blocked Click Prepare request from unauthorized IP: {}", clientIp);
            return ClickError.SIGN_CHECK_FAILED.asResponse();
        }

        if (!verifySignature(request)) {
            log.warn("Invalid signature for prepare request: {}", request);
            return ClickError.SIGN_CHECK_FAILED.asResponse();
        }

        if (isDuplicateRequest(String.valueOf(request.getClickTransId()), Long.parseLong(request.getSignTime()))) {
            log.warn("Duplicate Click Prepare request: {}", request.getClickTransId());
            return ClickError.ACTION_ALREADY_DONE.asResponse();
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

        log.info("Click Prepare successful for orderId: {}", request.getMerchantTransId());

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
    public ClickResponseDto complete(ClickRequestDto request, String clientIp) {
        log.info("Click Complete request from {}: {}", clientIp, request);

        if (!validateIpAddress(clientIp)) {
            log.warn("Blocked Click Complete request from unauthorized IP: {}", clientIp);
            return ClickError.SIGN_CHECK_FAILED.asResponse();
        }

        if (!verifySignature(request)) {
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

        PaymentSuccessEvent event = new PaymentSuccessEvent(savedPayment.getOrderId(), savedPayment.getId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY, event);
        log.info("Payment successful for orderId: {}. Event published.", savedPayment.getOrderId());

        return successResponse(request, savedPayment);
    }

    @Override
    @Transactional
    public ClickResponseDto refund(ClickRequestDto request, String clientIp) {
        log.info("Click Refund request from {}: {}", clientIp, request);

        if (!validateIpAddress(clientIp)) {
            log.warn("Blocked Click Refund request from unauthorized IP: {}", clientIp);
            return ClickError.SIGN_CHECK_FAILED.asResponse();
        }

        if (!verifySignature(request)) {
            log.warn("Invalid signature for refund request: {}", request);
            return ClickError.SIGN_CHECK_FAILED.asResponse();
        }

        Optional<Payment> paymentOpt = paymentRepository.findByProviderTransactionId(String.valueOf(request.getClickTransId()));
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for click_trans_id: {}", request.getClickTransId());
            return ClickError.TRANSACTION_NOT_FOUND.asResponse();
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            return ClickError.TRANSACTION_CANCELLED.asResponse();
        }

        if (payment.getAmount().compareTo(request.getAmount()) != 0) {
            return ClickError.INVALID_AMOUNT.asResponse();
        }

        // Mark as refunded
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(request.getAmount());
        payment.setRefundReason("Refund requested via Click");
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Click Refund successful for payment: {}", payment.getId());

        return new ClickResponseDto(
                request.getClickTransId(),
                request.getMerchantTransId(),
                payment.getId(),
                payment.getId(),
                0,
                "Refund successful"
        );
    }

    @Override
    public boolean verifySignature(ClickRequestDto request) {
        String signStringTemplate = request.getMerchantPrepareId() != null
                ? "{click_trans_id}{service_id}{secret_key}{merchant_trans_id}{merchant_prepare_id}{amount}{action}{sign_time}"
                : "{click_trans_id}{service_id}{secret_key}{merchant_trans_id}{amount}{action}{sign_time}";

        String signString = signStringTemplate
                .replace("{click_trans_id}", String.valueOf(request.getClickTransId()))
                .replace("{service_id}", serviceId)
                .replace("{secret_key}", secretKey)
                .replace("{merchant_trans_id}", request.getMerchantTransId())
                .replace("{merchant_prepare_id}", request.getMerchantPrepareId() != null ? String.valueOf(request.getMerchantPrepareId()) : "")
                .replace("{amount}", request.getAmount().toString())
                .replace("{action}", String.valueOf(request.getAction()))
                .replace("{sign_time}", request.getSignTime());

        String generatedSign = DigestUtils.md5Hex(signString);
        boolean isValid = generatedSign.equals(request.getSignString());

        if (!isValid) {
            log.warn("Signature validation failed. Expected: {}, Actual: {}", generatedSign, request.getSignString());
        }

        return isValid;
    }

    @Override
    public boolean validateIpAddress(String clientIp) {
        if (allowedIps == null || allowedIps.isEmpty()) {
            log.warn("No allowed IPs configured for Click, allowing all requests");
            return true;
        }

        String[] allowedIpRanges = allowedIps.split(",");
        for (String ipRange : allowedIpRanges) {
            if (isIpInRange(clientIp, ipRange.trim())) {
                return true;
            }
        }

        log.warn("IP address not in allowed range: {}", clientIp);
        return false;
    }

    @Override
    public Map<String, Object> getHealthInfo() {
        long totalPayments = paymentRepository.count();
        long pendingPayments = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long successfulPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);

        return Map.of(
                "status", "UP",
                "service", "click-merchant-service",
                "timestamp", LocalDateTime.now().toString(),
                "payments", Map.of(
                        "total", totalPayments,
                        "pending", pendingPayments,
                        "successful", successfulPayments
                ),
                "security", Map.of(
                        "ipValidation", true,
                        "signatureVerification", true,
                        "duplicateCheck", true
                ),
                "cache", Map.of(
                        "requestCacheSize", requestCache.size()
                )
        );
    }

    @Override
    public Map<String, Object> getTransactionStatus(String clickTransId) {
        Optional<Payment> paymentOpt = paymentRepository.findByProviderTransactionId(clickTransId);
        if (paymentOpt.isEmpty()) {
            return Map.of("found", false, "error", "Transaction not found");
        }

        Payment payment = paymentOpt.get();
        return Map.of(
                "found", true,
                "clickTransId", clickTransId,
                "orderId", payment.getOrderId(),
                "status", payment.getStatus().toString(),
                "amount", payment.getAmount(),
                "createdAt", payment.getCreatedAt(),
                "updatedAt", payment.getUpdatedAt()
        );
    }

    @Override
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupOldPrepareRecords() {
        log.debug("Cleaning up old Click prepare records");
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        int initialSize = requestCache.size();

        requestCache.entrySet().removeIf(entry -> {
            long signTime = entry.getValue();
            return signTime < threshold.minusHours(24).toEpochSecond(java.time.ZoneOffset.UTC);
        });

        log.info("Cleaned up {} old Click prepare records", initialSize - requestCache.size());
    }

    @Override
    public Map<String, Object> getClickStatistics(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        Long totalTransactions = paymentRepository.countByCreatedAtAfter(startDate);
        Long successfulTransactions = paymentRepository.countByCreatedAtAfterAndStatus(startDate, PaymentStatus.SUCCESS);

        return Map.of(
                "period", Map.of("days", days, "startDate", startDate.toString()),
                "transactions", Map.of(
                        "total", totalTransactions,
                        "successful", successfulTransactions,
                        "successRate", totalTransactions > 0 ?
                                (double) successfulTransactions / totalTransactions * 100 : 0
                ),
                "cache", Map.of(
                        "currentSize", requestCache.size()
                )
        );
    }

    @Override
    public boolean isDuplicateRequest(String clickTransId, Long signTime) {
        String cacheKey = clickTransId + "_" + signTime;
        if (requestCache.containsKey(cacheKey)) {
            return true;
        }

        // Store for 24 hours
        requestCache.put(cacheKey, signTime);
        return false;
    }

    // Private helper methods
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

    private boolean isIpInRange(String ip, String range) {
        // Простая реализация проверки IP диапазона
        // В продакшене используйте библиотеку для проверки CIDR
        return ip.equals(range) || range.contains(ip);
    }
}