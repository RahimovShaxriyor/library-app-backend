package com.example.paymentservice.service.impl;

import com.example.paymentservice.config.RabbitMQConfig;
import com.example.paymentservice.dto.payme.PaymeRequest;
import com.example.paymentservice.dto.payme.PaymeResponse;
import com.example.paymentservice.dto.payme.PaymeResponseMessage;
import com.example.paymentservice.dto.rabbit.PaymentSuccessEvent;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.OrderServiceClient;
import com.example.paymentservice.service.PaymeMerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymeMerchantServiceImpl implements PaymeMerchantService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OrderServiceClient orderServiceClient;

    @Value("${payme.merchant.key}")
    private String merchantKey;

    @Value("${payme.allowed.ips:}")
    private String allowedIps;

    private final Map<String, AuditRecord> requestAudit = new HashMap<>();

    @Override
    @Transactional
    public PaymeResponse handleRequest(String authorization, PaymeRequest request, String clientIp, String requestId) {
        log.info("Processing Payme request [{}] from {}: method={}, id={}",
                requestId, clientIp, request.getMethod(), request.getId());

        if (!validateIpAddress(clientIp)) {
            log.warn("Blocked Payme request from unauthorized IP: {}", clientIp);
            auditRequest(requestId, request, clientIp, "INCOMING", "BLOCKED_IP");
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.INVALID_AUTHORIZATION);
        }

        if (!verifyMerchantCredentials(authorization)) {
            log.warn("Invalid authorization header from IP: {}", clientIp);
            auditRequest(requestId, request, clientIp, "INCOMING", "INVALID_AUTH");
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.INVALID_AUTHORIZATION);
        }

        auditRequest(requestId, request, clientIp, "INCOMING", "PROCESSING");

        try {
            PaymeResponse response = switch (request.getMethod()) {
                case "CheckPerformTransaction" -> checkPerformTransaction(request);
                case "CreateTransaction" -> createTransaction(request);
                case "PerformTransaction" -> performTransaction(request);
                case "CancelTransaction" -> cancelTransaction(request);
                case "CheckTransaction" -> checkTransaction(request);
                case "GetStatement" -> getStatement(request);
                default -> PaymeResponse.error(request.getId(), PaymeResponseMessage.METHOD_NOT_FOUND);
            };

            auditResponse(requestId, response, "OUTGOING", "COMPLETED");
            log.info("Payme request [{}] completed successfully", requestId);
            return response;

        } catch (Exception e) {
            log.error("Error processing Payme request [{}] method: {}", requestId, request.getMethod(), e);
            auditResponse(requestId, null, "OUTGOING", "ERROR");
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.SYSTEM_ERROR);
        }
    }

    @Override
    public void auditRequest(String requestId, PaymeRequest request, String clientIp, String direction) {
        auditRequest(requestId, request, clientIp, direction, "RECEIVED");
    }

    @Override
    public void auditResponse(String requestId, PaymeResponse response, String direction) {
        auditResponse(requestId, response, direction, "SENT");
    }

    public void auditRequest(String requestId, PaymeRequest request, String clientIp, String direction, String status) {
        AuditRecord record = new AuditRecord(requestId, request.getMethod(), clientIp, direction, status);
        record.setRequestPayload(request.toString());
        requestAudit.put(requestId, record);
        log.debug("Audit request [{}]: {} - {}", requestId, direction, status);
    }

    public void auditResponse(String requestId, PaymeResponse response, String direction, String status) {
        AuditRecord record = requestAudit.get(requestId);
        if (record != null) {
            record.setResponsePayload(response != null ? response.toString() : "ERROR");
            record.setStatus(status);
            record.setCompletedAt(LocalDateTime.now());
            log.debug("Audit response [{}]: {} - {}", requestId, direction, status);
        }
    }

    @Override
    public Map<String, Object> getHealthInfo() {
        long totalRequests = requestAudit.size();
        long successfulRequests = requestAudit.values().stream()
                .filter(r -> "COMPLETED".equals(r.getStatus()))
                .count();

        long pendingPayments = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long successfulPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);

        return Map.of(
                "status", "UP",
                "service", "payme-merchant-service",
                "timestamp", LocalDateTime.now().toString(),
                "requests", Map.of(
                        "total", totalRequests,
                        "successful", successfulRequests,
                        "pending", requestAudit.values().stream()
                                .filter(r -> "PROCESSING".equals(r.getStatus()))
                                .count()
                ),
                "payments", Map.of(
                        "pending", pendingPayments,
                        "successful", successfulPayments
                ),
                "features", Map.of(
                        "ipValidation", true,
                        "requestAudit", true,
                        "retryMechanism", true
                )
        );
    }

    @Override
    public Map<String, Object> getStatistics(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        Long totalTransactions = paymentRepository.countByCreatedAtAfter(startDate);
        BigDecimal totalVolume = paymentRepository.sumAmountByCreatedAtAfter(startDate)
                .orElse(BigDecimal.ZERO);

        Long successfulTransactions = paymentRepository.countByCreatedAtAfterAndStatus(
                startDate, PaymentStatus.SUCCESS);

        List<Map<String, Object>> hourlyStats = paymentRepository.getHourlyStats(startDate);

        return Map.of(
                "period", Map.of("days", days, "startDate", startDate.toString()),
                "transactions", Map.of(
                        "total", totalTransactions,
                        "successful", successfulTransactions,
                        "successRate", totalTransactions > 0 ?
                                (double) successfulTransactions / totalTransactions * 100 : 0
                ),
                "volume", Map.of(
                        "total", totalVolume,
                        "currency", "UZS"
                ),
                "hourlyDistribution", hourlyStats,
                "audit", Map.of(
                        "totalRequests", requestAudit.size(),
                        "recentRequests", requestAudit.values().stream()
                                .filter(r -> r.getCreatedAt().isAfter(startDate))
                                .count()
                )
        );
    }

    @Override
    public boolean validateIpAddress(String clientIp) {
        if (allowedIps == null || allowedIps.isEmpty()) {
            log.warn("No allowed IPs configured, allowing all requests");
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
    public boolean verifyMerchantCredentials(String authorizationHeader) {
        return isValidAuthorization(authorizationHeader);
    }

    @Override
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredTransactions() {
        log.debug("Cleaning up expired Payme transactions");
        LocalDateTime now = LocalDateTime.now();

        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(now);
        for (Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setCancellationReason("Expired");
            payment.setCancelledAt(now);
            paymentRepository.save(payment);
            log.info("Expired payment cancelled: {}", payment.getId());
        }

        LocalDateTime auditThreshold = now.minusHours(24);
        int initialSize = requestAudit.size();
        requestAudit.entrySet().removeIf(entry ->
                entry.getValue().getCreatedAt().isBefore(auditThreshold)
        );

        log.info("Cleanup completed: {} expired payments, {} old audit records",
                expiredPayments.size(), initialSize - requestAudit.size());
    }

    @Override
    @Transactional
    public Map<String, Object> retryFailedTransaction(String transactionId) {
        log.info("Retrying failed Payme transaction: {}", transactionId);

        Optional<Payment> paymentOpt = paymentRepository.findByProviderTransactionId(transactionId);
        if (paymentOpt.isEmpty()) {
            return Map.of("success", false, "error", "Transaction not found");
        }

        Payment payment = paymentOpt.get();
        if (payment.getStatus() != PaymentStatus.FAILED) {
            return Map.of("success", false, "error", "Transaction is not in failed state");
        }

        if (!payment.shouldRetry()) {
            return Map.of("success", false, "error", "Max retry attempts reached");
        }

        payment.setStatus(PaymentStatus.PENDING);
        payment.setNextRetryAt(null);
        paymentRepository.save(payment);

        log.info("Transaction {} marked for retry", transactionId);
        return Map.of(
                "success", true,
                "transactionId", transactionId,
                "attempt", payment.getAttemptCount() + 1,
                "status", "PENDING"
        );
    }

    @Override
    public Map<String, Object> getTransactionDetails(String transactionId) {
        Optional<Payment> paymentOpt = paymentRepository.findByProviderTransactionId(transactionId);
        if (paymentOpt.isEmpty()) {
            return Map.of("found", false, "error", "Transaction not found");
        }

        Payment payment = paymentOpt.get();
        return Map.of(
                "found", true,
                "transactionId", transactionId,
                "orderId", payment.getOrderId(),
                "status", payment.getStatus().toString(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency(),
                "createdAt", payment.getCreatedAt(),
                "updatedAt", payment.getUpdatedAt(),
                "provider", payment.getProvider().toString()
        );
    }

    @Override
    public Map<String, Object> getTransactionsByOrder(Long orderId) {
        List<Payment> payments = paymentRepository.findAllByOrderId(orderId, null);

        List<Map<String, Object>> transactionList = new ArrayList<>();
        for (Payment payment : payments) {
            transactionList.add(Map.of(
                    "paymentId", payment.getId(),
                    "transactionId", payment.getProviderTransactionId(),
                    "status", payment.getStatus().toString(),
                    "amount", payment.getAmount(),
                    "provider", payment.getProvider().toString(),
                    "createdAt", payment.getCreatedAt()
            ));
        }

        return Map.of(
                "orderId", orderId,
                "totalTransactions", transactionList.size(),
                "transactions", transactionList
        );
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            Map<String, Object> healthInfo = getHealthInfo();
            return "UP".equals(healthInfo.get("status"));
        } catch (Exception e) {
            log.error("Service availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getPerformanceMetrics() {
        LocalDateTime lastHour = LocalDateTime.now().minusHours(1);

        Long requestsLastHour = requestAudit.values().stream()
                .filter(r -> r.getCreatedAt().isAfter(lastHour))
                .count();

        Long successfulRequestsLastHour = requestAudit.values().stream()
                .filter(r -> r.getCreatedAt().isAfter(lastHour) && "COMPLETED".equals(r.getStatus()))
                .count();

        return Map.of(
                "timeframe", "last_hour",
                "requests", Map.of(
                        "total", requestsLastHour,
                        "successful", successfulRequestsLastHour,
                        "successRate", requestsLastHour > 0 ?
                                (double) successfulRequestsLastHour / requestsLastHour * 100 : 0
                ),
                "cache", Map.of(
                        "auditRecords", requestAudit.size()
                ),
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @Override
    public Map<String, Object> forceCancelTransaction(String transactionId, String reason) {
        log.info("Force cancelling Payme transaction: {}, reason: {}", transactionId, reason);

        Optional<Payment> paymentOpt = paymentRepository.findByProviderTransactionId(transactionId);
        if (paymentOpt.isEmpty()) {
            return Map.of("success", false, "error", "Transaction not found");
        }

        Payment payment = paymentOpt.get();
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return Map.of("success", false, "error", "Can only force cancel PENDING transactions");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancellationReason(reason);
        payment.setCancelledAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Transaction {} force cancelled", transactionId);
        return Map.of(
                "success", true,
                "transactionId", transactionId,
                "status", "CANCELLED",
                "reason", reason
        );
    }

    private PaymeResponse checkPerformTransaction(PaymeRequest request) {
        try {
            String orderIdStr = request.getParams().getAccount().getOrderId();
            Long orderId = Long.valueOf(orderIdStr);
            BigDecimal amountInTiyin = new BigDecimal(request.getParams().getAmount().getValue().toString());

            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
            if (paymentOpt.isEmpty()) {
                log.warn("CheckPerformTransaction failed: Order not found for orderId: {}", orderId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.ORDER_NOT_FOUND);
            }

            Payment payment = paymentOpt.get();
            BigDecimal amountInTiyinExpected = payment.getAmount().multiply(new BigDecimal(100));

            if (amountInTiyinExpected.compareTo(amountInTiyin) != 0) {
                log.warn("CheckPerformTransaction failed: Invalid amount for orderId: {}. Expected: {}, Actual: {}",
                        orderId, amountInTiyinExpected, amountInTiyin);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.INVALID_AMOUNT);
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.warn("CheckPerformTransaction failed: Order is not in PENDING state for orderId: {}", orderId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.CANNOT_PERFORM_OPERATION);
            }

            Boolean orderExists = orderServiceClient.checkOrderExists(orderId).block();
            if (orderExists == null || !orderExists) {
                log.warn("CheckPerformTransaction failed: Order not found in order service for orderId: {}", orderId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.ORDER_NOT_FOUND);
            }

            return PaymeResponse.allow(request.getId());

        } catch (NumberFormatException e) {
            log.error("Invalid order ID format in CheckPerformTransaction", e);
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.INVALID_ORDER_ID);
        } catch (Exception e) {
            log.error("Error in CheckPerformTransaction", e);
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.SYSTEM_ERROR);
        }
    }

    private PaymeResponse createTransaction(PaymeRequest request) {
        try {
            String paymeTransactionId = request.getParams().getId();
            String orderIdStr = request.getParams().getAccount().getOrderId();
            Long orderId = Long.valueOf(orderIdStr);

            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElse(null);

            if (payment == null) {
                log.warn("CreateTransaction failed: Payment not found for orderId: {}", orderId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.TRANSACTION_NOT_FOUND);
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.warn("CreateTransaction failed: Payment is not in PENDING state for orderId: {}", orderId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.CANNOT_PERFORM_OPERATION);
            }

            if (payment.getProviderTransactionId() != null) {
                log.warn("CreateTransaction failed: Transaction already exists for orderId: {}", orderId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.TRANSACTION_ALREADY_EXISTS);
            }

            payment.setProviderTransactionId(paymeTransactionId);
            Payment savedPayment = paymentRepository.save(payment);

            log.info("Transaction created successfully for orderId: {}, paymeTransactionId: {}",
                    orderId, paymeTransactionId);

            return PaymeResponse.successCreate(
                    request.getId(),
                    System.currentTimeMillis(),
                    savedPayment.getProviderTransactionId(),
                    1
            );

        } catch (Exception e) {
            log.error("Error in CreateTransaction", e);
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.SYSTEM_ERROR);
        }
    }

    private PaymeResponse performTransaction(PaymeRequest request) {
        try {
            String paymeTransactionId = request.getParams().getId();

            Payment payment = paymentRepository.findByProviderTransactionId(paymeTransactionId)
                    .orElse(null);

            if (payment == null) {
                log.warn("PerformTransaction failed: Transaction not found for paymeTransactionId: {}", paymeTransactionId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.TRANSACTION_NOT_FOUND);
            }

            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.info("Transaction already performed for paymeTransactionId: {}", paymeTransactionId);
                return PaymeResponse.successPerform(
                        request.getId(),
                        System.currentTimeMillis(),
                        payment.getProviderTransactionId(),
                        2
                );
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.warn("PerformTransaction failed: Transaction is not in PENDING state for paymeTransactionId: {}",
                        paymeTransactionId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.CANNOT_PERFORM_OPERATION);
            }

            payment.setStatus(PaymentStatus.SUCCESS);
            Payment savedPayment = paymentRepository.save(payment);

            PaymentSuccessEvent event = new PaymentSuccessEvent(savedPayment.getOrderId(), savedPayment.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY,
                    event
            );
            log.info("Payment successful for orderId: {}. Event published to routing key: {}",
                    savedPayment.getOrderId(), RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY);

            return PaymeResponse.successPerform(
                    request.getId(),
                    System.currentTimeMillis(),
                    savedPayment.getProviderTransactionId(),
                    2
            );

        } catch (Exception e) {
            log.error("Error in PerformTransaction", e);
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.SYSTEM_ERROR);
        }
    }

    private PaymeResponse cancelTransaction(PaymeRequest request) {
        try {
            String paymeTransactionId = request.getParams().getId();
            Integer reason = request.getParams().getReason();

            Payment payment = paymentRepository.findByProviderTransactionId(paymeTransactionId)
                    .orElse(null);

            if (payment == null) {
                log.warn("CancelTransaction failed: Transaction not found for paymeTransactionId: {}", paymeTransactionId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.TRANSACTION_NOT_FOUND);
            }

            if (payment.getStatus() == PaymentStatus.CANCELLED) {
                log.info("Transaction already cancelled for paymeTransactionId: {}", paymeTransactionId);
                return PaymeResponse.successCancel(
                        request.getId(),
                        System.currentTimeMillis(),
                        payment.getProviderTransactionId(),
                        -1
                );
            }

            if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.SUCCESS) {
                log.warn("CancelTransaction failed: Invalid state for cancellation, paymeTransactionId: {}",
                        paymeTransactionId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.CANNOT_PERFORM_OPERATION);
            }

            payment.setStatus(PaymentStatus.CANCELLED);
            Payment savedPayment = paymentRepository.save(payment);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.PAYMENT_CANCELLED_ROUTING_KEY,
                    savedPayment
            );
            log.info("Payment cancelled for orderId: {}. Event published to routing key: {}",
                    savedPayment.getOrderId(), RabbitMQConfig.PAYMENT_CANCELLED_ROUTING_KEY);

            return PaymeResponse.successCancel(
                    request.getId(),
                    System.currentTimeMillis(),
                    savedPayment.getProviderTransactionId(),
                    -1,
                    reason
            );

        } catch (Exception e) {
            log.error("Error in CancelTransaction", e);
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.SYSTEM_ERROR);
        }
    }

    private PaymeResponse checkTransaction(PaymeRequest request) {
        try {
            String paymeTransactionId = request.getParams().getId();

            Payment payment = paymentRepository.findByProviderTransactionId(paymeTransactionId)
                    .orElse(null);

            if (payment == null) {
                log.warn("CheckTransaction failed: Transaction not found for paymeTransactionId: {}", paymeTransactionId);
                return PaymeResponse.error(request.getId(), PaymeResponseMessage.TRANSACTION_NOT_FOUND);
            }

            int state = switch (payment.getStatus()) {
                case PENDING -> 1;
                case SUCCESS -> 2;
                case CANCELLED -> -1;
                case FAILED -> -2;
                default -> 0;
            };

            return PaymeResponse.successCheck(
                    request.getId(),
                    payment.getProviderTransactionId(),
                    payment.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
                    payment.getStatus() == PaymentStatus.SUCCESS ? payment.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : 0,
                    payment.getStatus() == PaymentStatus.CANCELLED ? payment.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : 0,
                    state
            );

        } catch (Exception e) {
            log.error("Error in CheckTransaction", e);
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.SYSTEM_ERROR);
        }
    }

    private PaymeResponse getStatement(PaymeRequest request) {
        try {
            Long from = request.getParams().getFrom();
            Long to = request.getParams().getTo();

            LocalDateTime fromDate = LocalDateTime.ofEpochSecond(from / 1000, 0, java.time.ZoneOffset.UTC);
            LocalDateTime toDate = LocalDateTime.ofEpochSecond(to / 1000, 0, java.time.ZoneOffset.UTC);

            List<Payment> payments = paymentRepository.findByCreatedAtBetween(fromDate, toDate, null).getContent();

            List<Map<String, Object>> transactions = new ArrayList<>();
            for (Payment payment : payments) {
                if (payment.getProviderTransactionId() != null && payment.getStatus() == PaymentStatus.SUCCESS) {
                    Map<String, Object> transaction = new HashMap<>();
                    transaction.put("id", payment.getProviderTransactionId());
                    transaction.put("time", payment.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);

                    Map<String, Object> amountMap = new HashMap<>();
                    amountMap.put("value", payment.getAmount().multiply(new BigDecimal(100)).longValue());
                    amountMap.put("currency", "UZS");
                    transaction.put("amount", amountMap);

                    Map<String, String> accountMap = new HashMap<>();
                    accountMap.put("order_id", String.valueOf(payment.getOrderId()));
                    transaction.put("account", accountMap);

                    transaction.put("create_time", payment.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
                    transaction.put("perform_time", payment.getCompletedAt() != null ?
                            payment.getCompletedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : 0);
                    transaction.put("cancel_time", 0);
                    transaction.put("transaction", payment.getId().toString());
                    transaction.put("state", 2);
                    transaction.put("reason", null);

                    transactions.add(transaction);
                }
            }

            log.info("GetStatement request from: {} to: {}, found {} transactions", from, to, transactions.size());
            return PaymeResponse.successStatement(request.getId(), transactions);

        } catch (Exception e) {
            log.error("Error in GetStatement", e);
            return PaymeResponse.error(request.getId(), PaymeResponseMessage.SYSTEM_ERROR);
        }
    }

    private boolean isValidAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return false;
        }
        try {
            String base64Credentials = authorization.substring("Basic ".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded);
            final String[] values = credentials.split(":", 2);
            String username = values[0];
            String password = values[1];
            return "Paycom".equals(username) && Objects.equals(this.merchantKey, password);
        } catch (Exception e) {
            log.error("Error decoding authorization header", e);
            return false;
        }
    }

    private boolean isIpInRange(String ip, String range) {
        return ip.equals(range) || range.contains(ip);
    }

    private static class AuditRecord {
        private final String requestId;
        private final String method;
        private final String clientIp;
        private final String direction;
        private String status;
        private String requestPayload;
        private String responsePayload;
        private final LocalDateTime createdAt;
        private LocalDateTime completedAt;

        public AuditRecord(String requestId, String method, String clientIp, String direction, String status) {
            this.requestId = requestId;
            this.method = method;
            this.clientIp = clientIp;
            this.direction = direction;
            this.status = status;
            this.createdAt = LocalDateTime.now();
        }

        public String getRequestId() { return requestId; }
        public String getMethod() { return method; }
        public String getClientIp() { return clientIp; }
        public String getDirection() { return direction; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRequestPayload() { return requestPayload; }
        public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }
        public String getResponsePayload() { return responsePayload; }
        public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }
}