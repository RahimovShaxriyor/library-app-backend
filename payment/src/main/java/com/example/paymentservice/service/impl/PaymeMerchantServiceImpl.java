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

    // In-memory storage for request audit (in production use Redis)
    private final Map<String, AuditRecord> requestAudit = new HashMap<>();

    // ... остальные методы остаются без изменений ...

    // РЕАЛИЗАЦИЯ МЕТОДОВ ОБРАБОТКИ PAYME ЗАПРОСОВ

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

            // Проверяем существование заказа через OrderService
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

            // Проверяем, не существует ли уже транзакция с таким providerTransactionId
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

            // Отправляем событие об успешном платеже
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

            // Отправляем событие об отмене платежа
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

            // Конвертируем timestamp в LocalDateTime
            LocalDateTime fromDate = LocalDateTime.ofEpochSecond(from / 1000, 0, java.time.ZoneOffset.UTC);
            LocalDateTime toDate = LocalDateTime.ofEpochSecond(to / 1000, 0, java.time.ZoneOffset.UTC);

            // Получаем транзакции за период
            List<Payment> payments = paymentRepository.findByCreatedAtBetween(fromDate, toDate, null).getContent();

            // Формируем список транзакций для ответа
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
                    transaction.put("state", 2); // SUCCESS
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

    // ... остальные методы (isValidAuthorization, isIpInRange, AuditRecord) остаются без изменений ...
}