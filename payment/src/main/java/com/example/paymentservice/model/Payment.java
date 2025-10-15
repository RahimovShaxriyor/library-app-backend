package com.example.paymentservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "orderId"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_created_at", columnList = "createdAt"),
        @Index(name = "idx_payment_provider_transaction", columnList = "providerTransactionId"),
        @Index(name = "idx_payment_provider_status", columnList = "provider, status")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq")
    @SequenceGenerator(name = "payment_seq", sequenceName = "PAYMENT_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentProvider provider;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(length = 3)
    private String currency = "UZS";

    @Column(length = 500)
    private String description;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @Column(name = "refund_reason", length = 1000)
    private String refundReason;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "success_url", length = 500)
    private String successUrl;

    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    @Column(name = "metadata", columnDefinition = "CLOB")
    private String metadata;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "max_attempts")
    private Integer maxAttempts = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Конструкторы
    public Payment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Payment(Long orderId, BigDecimal amount, PaymentProvider provider) {
        this();
        this.orderId = orderId;
        this.amount = amount;
        this.provider = provider;
        this.status = PaymentStatus.PENDING;
    }

    // Бизнес-методы
    public boolean canBeCancelled() {
        return status == PaymentStatus.PENDING;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.SUCCESS && refundAmount == null;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean shouldRetry() {
        return status == PaymentStatus.FAILED &&
                attemptCount < maxAttempts &&
                (nextRetryAt == null || LocalDateTime.now().isAfter(nextRetryAt));
    }

    public void markAsSuccess(String providerTransactionId) {
        this.status = PaymentStatus.SUCCESS;
        this.providerTransactionId = providerTransactionId;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void markAsFailed(String errorCode, String errorMessage) {
        this.status = PaymentStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.attemptCount++;
        this.updatedAt = LocalDateTime.now();

        if (this.attemptCount < this.maxAttempts) {
            this.nextRetryAt = LocalDateTime.now().plusMinutes(5 * this.attemptCount); // Exponential backoff
        }
    }

    public void markAsCancelled(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsRefunded(BigDecimal amount, String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundAmount = amount;
        this.refundReason = reason;
        this.refundedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Методы для установки времени истечения
    public void setExpirationInMinutes(int minutes) {
        this.expiresAt = LocalDateTime.now().plusMinutes(minutes);
    }

    public void setExpirationInHours(int hours) {
        this.expiresAt = LocalDateTime.now().plusHours(hours);
    }

    // Getters with null-safety
    public BigDecimal getRefundAmount() {
        return refundAmount != null ? refundAmount : BigDecimal.ZERO;
    }

    public Integer getAttemptCount() {
        return attemptCount != null ? attemptCount : 0;
    }

    public Integer getMaxAttempts() {
        return maxAttempts != null ? maxAttempts : 3;
    }
}