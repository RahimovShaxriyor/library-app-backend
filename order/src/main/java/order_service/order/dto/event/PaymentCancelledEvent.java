package order_service.order.dto.event;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record PaymentCancelledEvent(
        @NotNull(message = "Order ID cannot be null")
        @Positive(message = "Order ID must be positive")
        Long orderId,

        @NotBlank(message = "Payment ID cannot be blank")
        @Size(max = 50, message = "Payment ID cannot exceed 50 characters")
        String paymentId,

        @NotBlank(message = "Cancellation reason cannot be blank")
        @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
        String reason,

        @NotNull(message = "Cancellation timestamp cannot be null")
        @PastOrPresent(message = "Cancellation timestamp cannot be in the future")
        LocalDateTime cancellationTimestamp
) {

    public PaymentCancelledEvent {
        // Compact constructor - validation and initialization
        if (cancellationTimestamp == null) {
            cancellationTimestamp = LocalDateTime.now();
        }

        if (orderId != null && orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }

        // Trim and validate strings
        if (paymentId != null) {
            paymentId = paymentId.trim();
            if (paymentId.isEmpty()) {
                throw new IllegalArgumentException("Payment ID cannot be empty");
            }
        }

        if (reason != null) {
            reason = reason.trim();
            if (reason.isEmpty()) {
                throw new IllegalArgumentException("Cancellation reason cannot be empty");
            }
        }
    }

    // Convenience constructor without timestamp
    public PaymentCancelledEvent(Long orderId, String paymentId, String reason) {
        this(orderId, paymentId, reason, LocalDateTime.now());
    }

    // Utility methods
    public boolean isRecentCancellation() {
        return cancellationTimestamp.isAfter(LocalDateTime.now().minusHours(24));
    }

    public boolean isSystemInitiated() {
        return reason != null && (
                reason.toLowerCase().contains("system") ||
                        reason.toLowerCase().contains("timeout") ||
                        reason.toLowerCase().contains("error")
        );
    }

    public boolean isUserInitiated() {
        return reason != null && (
                reason.toLowerCase().contains("user") ||
                        reason.toLowerCase().contains("customer") ||
                        reason.toLowerCase().contains("changed mind")
        );
    }

    // Static factory methods
    public static PaymentCancelledEvent systemCancellation(Long orderId, String paymentId, String systemReason) {
        return new PaymentCancelledEvent(
                orderId,
                paymentId,
                "System: " + systemReason,
                LocalDateTime.now()
        );
    }

    public static PaymentCancelledEvent userCancellation(Long orderId, String paymentId, String userReason) {
        return new PaymentCancelledEvent(
                orderId,
                paymentId,
                "User: " + userReason,
                LocalDateTime.now()
        );
    }

    public static PaymentCancelledEvent timeoutCancellation(Long orderId, String paymentId) {
        return new PaymentCancelledEvent(
                orderId,
                paymentId,
                "Payment timeout - no response from payment provider",
                LocalDateTime.now()
        );
    }

    public static PaymentCancelledEvent insufficientFundsCancellation(Long orderId, String paymentId) {
        return new PaymentCancelledEvent(
                orderId,
                paymentId,
                "Insufficient funds",
                LocalDateTime.now()
        );
    }

    // Validation method for service layer
    public void validate() {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }
        if (cancellationTimestamp == null) {
            throw new IllegalArgumentException("Cancellation timestamp cannot be null");
        }
        if (cancellationTimestamp.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cancellation timestamp cannot be in the future");
        }
    }

    // JSON representation for logging
    public String toJsonString() {
        return String.format(
                "{\"orderId\": %d, \"paymentId\": \"%s\", \"reason\": \"%s\", \"timestamp\": \"%s\"}",
                orderId, paymentId, reason, cancellationTimestamp
        );
    }

    @Override
    public String toString() {
        return String.format(
                "PaymentCancelledEvent[orderId=%d, paymentId=%s, reason=%s, timestamp=%s]",
                orderId, paymentId, reason, cancellationTimestamp
        );
    }
}