package com.example.paymentservice.service.impl;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentProvider;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${payme.merchant.id}")
    private String paymeMerchantId;

    @Value("${click.merchant.id}")
    private String clickMerchantId;

    @Value("${click.service.id}")
    private String clickServiceId;

    @Value("${payme.checkout.url}")
    private String paymeCheckoutUrl;

    @Value("${click.checkout.url}")
    private String clickCheckoutUrl;

    @Value("${payment.idempotency.expiry.hours:24}")
    private int idempotencyExpiryHours;

    private final Map<String, IdempotencyRecord> idempotencyStore = new HashMap<>();

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request, String idempotencyKey) {
        log.info("Initiating payment for order: {}, provider: {}, idempotencyKey: {}",
                request.getOrderId(), request.getProvider(), idempotencyKey);

        // Check if payment already exists for this order with PENDING status
        paymentRepository.findByOrderIdAndStatus(request.getOrderId(), PaymentStatus.PENDING)
                .ifPresent(existingPayment -> {
                    log.warn("Pending payment already exists for order: {}", request.getOrderId());
                    throw new RuntimeException("Pending payment already exists for this order");
                });

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider(PaymentProvider.valueOf(request.getProvider().toUpperCase()));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "UZS");
        payment.setDescription(request.getDescription());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {} for order: {}", savedPayment.getId(), request.getOrderId());

        String paymentUrl = generatePaymentUrl(savedPayment, request.getProvider());

        PaymentInitiationResponse response = new PaymentInitiationResponse(
                paymentUrl,
                savedPayment.getId(),
                savedPayment.getStatus().toString()
        );

        // Store idempotency key if provided
        if (idempotencyKey != null) {
            storeIdempotencyKey(idempotencyKey, "PAYMENT_INITIATION", savedPayment.getId().toString());
        }

        log.info("Payment initiation completed for order: {}, paymentUrl: {}",
                request.getOrderId(), paymentUrl);

        return response;
    }

    @Override
    public void handlePaymentCallback(String provider, Map<String, Object> callbackData) {
        log.info("Handling payment callback for provider: {}, data: {}", provider, callbackData);
        // This method is now primarily for additional callback handling
        // Main callback logic is handled in specific merchant services
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatus(Long paymentId) {
        log.debug("Getting payment status for ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    log.warn("Payment not found with ID: {}", paymentId);
                    return new RuntimeException("Payment not found");
                });

        return createPaymentStatusResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentByOrderId(Long orderId) {
        log.debug("Getting payment for order ID: {}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("Payment not found for orderId: {}", orderId);
                    return new RuntimeException("Payment not found for orderId: " + orderId);
                });

        return createPaymentDetailResponse(payment);
    }

    @Override
    @Transactional
    public Map<String, Object> cancelPayment(Long paymentId, String reason) {
        log.info("Cancelling payment: {}, reason: {}", paymentId, reason);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    log.warn("Payment not found for cancellation: {}", paymentId);
                    return new RuntimeException("Payment not found");
                });

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Cannot cancel payment with status: {}", payment.getStatus());
            throw new RuntimeException("Cannot cancel payment with status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setCancellationReason(reason);

        Payment cancelledPayment = paymentRepository.save(payment);
        log.info("Payment cancelled successfully: {}", paymentId);

        return Map.of(
                "paymentId", cancelledPayment.getId(),
                "status", cancelledPayment.getStatus().toString(),
                "reason", reason,
                "cancelledAt", cancelledPayment.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentHistory(Long orderId) {
        log.debug("Getting payment history for order: {}", orderId);

        List<Payment> payments = paymentRepository.findAllByOrderId(
                orderId,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<Map<String, Object>> paymentHistory = payments.stream()
                .map(this::createPaymentHistoryItem)
                .collect(Collectors.toList());

        return Map.of(
                "orderId", orderId,
                "totalPayments", paymentHistory.size(),
                "payments", paymentHistory
        );
    }

    @Override
    @Transactional
    public Map<String, Object> refundPayment(Long paymentId, String reason, String amountStr, String idempotencyKey) {
        log.info("Processing refund for payment: {}, reason: {}, amount: {}, idempotencyKey: {}",
                paymentId, reason, amountStr, idempotencyKey);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    log.warn("Payment not found for refund: {}", paymentId);
                    return new RuntimeException("Payment not found");
                });

        // Validate payment status for refund
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("Cannot refund payment with status: {}", payment.getStatus());
            throw new RuntimeException("Can only refund successful payments");
        }

        BigDecimal refundAmount = parseAmount(amountStr);
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(payment.getAmount()) > 0) {
            log.warn("Invalid refund amount: {} for payment: {}", refundAmount, paymentId);
            throw new RuntimeException("Invalid refund amount");
        }

        // Store idempotency key if provided
        if (idempotencyKey != null) {
            storeIdempotencyKey(idempotencyKey, "PAYMENT_REFUND", paymentId.toString());
        }

        // Update payment status to REFUNDED
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setRefundReason(reason);
        payment.setRefundAmount(refundAmount);

        Payment refundedPayment = paymentRepository.save(payment);
        log.info("Payment refund processed successfully: {}", paymentId);

        // In production, here you would call the payment provider's refund API
        // For now, we just update the status

        return Map.of(
                "paymentId", refundedPayment.getId(),
                "status", refundedPayment.getStatus().toString(),
                "refundAmount", refundAmount,
                "reason", reason,
                "refundedAt", refundedPayment.getUpdatedAt()
        );
    }

    @Override
    public Map<String, Object> getHealthInfo() {
        log.debug("Generating health check information");

        long totalPayments = paymentRepository.count();
        long pendingPayments = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long successfulPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);

        LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
        long recentPayments = paymentRepository.countByCreatedAtAfter(lastHour);

        return Map.of(
                "status", "UP",
                "service", "payment-service",
                "timestamp", LocalDateTime.now().toString(),
                "database", "CONNECTED",
                "metrics", Map.of(
                        "totalPayments", totalPayments,
                        "pendingPayments", pendingPayments,
                        "successfulPayments", successfulPayments,
                        "recentPaymentsLastHour", recentPayments
                ),
                "idempotencyKeys", idempotencyStore.size()
        );
    }

    @Override
    public Map<String, Object> getAvailableProviders() {
        log.debug("Getting available payment providers");

        return Map.of(
                "providers", List.of(
                        Map.of(
                                "code", "PAYME",
                                "name", "Payme",
                                "supportedCurrencies", List.of("UZS", "USD"),
                                "features", List.of("ONE_STEP", "REFUNDS", "SUBSCRIPTIONS")
                        ),
                        Map.of(
                                "code", "CLICK",
                                "name", "Click",
                                "supportedCurrencies", List.of("UZS"),
                                "features", List.of("TWO_STEP", "REFUNDS")
                        )
                ),
                "defaultCurrency", "UZS",
                "supportedOperations", List.of("PAYMENT", "REFUND", "STATUS_CHECK")
        );
    }

    @Override
    public boolean validateIdempotencyKey(String key, String operationType) {
        log.debug("Validating idempotency key: {} for operation: {}", key, operationType);

        IdempotencyRecord record = idempotencyStore.get(key);
        if (record == null) {
            return true; // Key doesn't exist, valid for new request
        }

        // Check if key has expired
        if (record.getCreatedAt().plusHours(idempotencyExpiryHours).isBefore(LocalDateTime.now())) {
            idempotencyStore.remove(key);
            return true; // Expired key, valid for new request
        }

        // Key exists and is valid - this is a duplicate request
        log.warn("Duplicate request detected with idempotency key: {}", key);
        return false;
    }

    @Override
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredIdempotencyKeys() {
        log.debug("Cleaning up expired idempotency keys");

        LocalDateTime expiryTime = LocalDateTime.now().minusHours(idempotencyExpiryHours);
        int initialSize = idempotencyStore.size();

        idempotencyStore.entrySet().removeIf(entry ->
                entry.getValue().getCreatedAt().isBefore(expiryTime)
        );

        log.info("Cleaned up {} expired idempotency keys", initialSize - idempotencyStore.size());
    }

    @Override
    public Map<String, Object> getPaymentMetrics() {
        log.debug("Generating payment metrics");

        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime weekAgo = today.minus(7, ChronoUnit.DAYS);

        long todayCount = paymentRepository.countByCreatedAtAfter(today);
        long weekCount = paymentRepository.countByCreatedAtAfter(weekAgo);

        BigDecimal todayVolume = paymentRepository.sumAmountByCreatedAtAfter(today)
                .orElse(BigDecimal.ZERO);
        BigDecimal weekVolume = paymentRepository.sumAmountByCreatedAtAfter(weekAgo)
                .orElse(BigDecimal.ZERO);

        Map<PaymentStatus, Long> statusCounts = (Map<PaymentStatus, Long>) paymentRepository.countByStatusGroup();

        return Map.of(
                "timeframe", Map.of(
                        "today", Map.of("count", todayCount, "volume", todayVolume),
                        "last7Days", Map.of("count", weekCount, "volume", weekVolume)
                ),
                "statusBreakdown", statusCounts,
                "providerBreakdown", paymentRepository.countByProviderGroup(),
                "timestamp", LocalDateTime.now().toString()
        );
    }

    Map<PaymentStatus, Long> statusCounts = paymentRepository.countByStatusGroup()
            .stream()
            .collect(Collectors.toMap(
                    arr -> (PaymentStatus) arr[0],
                    arr -> (Long) arr[1]
            ));


    private String generatePaymentUrl(Payment payment, String provider) {
        return switch (PaymentProvider.valueOf(provider.toUpperCase())) {
            case PAYME -> generatePaymeUrl(payment);
            case CLICK -> generateClickUrl(payment);
        };
    }

    private String generatePaymeUrl(Payment payment) {
        String params = String.format("m=%s;ac.order_id=%d;a=%d;c=%s",
                paymeMerchantId,
                payment.getOrderId(),
                payment.getAmount().multiply(new BigDecimal(100)).longValue(),
                "https://example.com/callback/payme"
        );
        String encodedParams = Base64.getEncoder().encodeToString(params.getBytes());
        return paymeCheckoutUrl + "/" + encodedParams;
    }

    private String generateClickUrl(Payment payment) {
        // Click typically uses different URL generation logic
        // This is a simplified version
        return String.format("%s?merchant_id=%s&amount=%s&order_id=%d",
                clickCheckoutUrl,
                clickMerchantId,
                payment.getAmount().toString(),
                payment.getOrderId()
        );
    }

    private void storeIdempotencyKey(String key, String operationType, String resourceId) {
        idempotencyStore.put(key, new IdempotencyRecord(operationType, resourceId, LocalDateTime.now()));
    }

    private Map<String, Object> createPaymentStatusResponse(Payment payment) {
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", payment.getId());
        response.put("orderId", payment.getOrderId());
        response.put("status", payment.getStatus().toString());
        response.put("amount", payment.getAmount());
        response.put("currency", payment.getCurrency());
        response.put("provider", payment.getProvider().toString());
        response.put("createdAt", payment.getCreatedAt());

        if (payment.getUpdatedAt() != null) {
            response.put("updatedAt", payment.getUpdatedAt());
        }

        return response;
    }

    private Map<String, Object> createPaymentDetailResponse(Payment payment) {
        Map<String, Object> response = createPaymentStatusResponse(payment);
        response.put("description", payment.getDescription());

        if (payment.getCancellationReason() != null) {
            response.put("cancellationReason", payment.getCancellationReason());
        }

        if (payment.getRefundReason() != null) {
            response.put("refundReason", payment.getRefundReason());
            response.put("refundAmount", payment.getRefundAmount());
        }

        return response;
    }

    private Map<String, Object> createPaymentHistoryItem(Payment payment) {
        Map<String, Object> item = new HashMap<>();
        item.put("paymentId", payment.getId());
        item.put("status", payment.getStatus().toString());
        item.put("amount", payment.getAmount());
        item.put("provider", payment.getProvider().toString());
        item.put("createdAt", payment.getCreatedAt());
        item.put("updatedAt", payment.getUpdatedAt());
        return item;
    }

    private BigDecimal parseAmount(String amountStr) {
        try {
            return new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid amount format: " + amountStr);
        }
    }

    // Inner class for idempotency record tracking
    private static class IdempotencyRecord {
        private final String operationType;
        private final String resourceId;
        private final LocalDateTime createdAt;

        public IdempotencyRecord(String operationType, String resourceId, LocalDateTime createdAt) {
            this.operationType = operationType;
            this.resourceId = resourceId;
            this.createdAt = createdAt;
        }

        public String getOperationType() { return operationType; }
        public String getResourceId() { return resourceId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}