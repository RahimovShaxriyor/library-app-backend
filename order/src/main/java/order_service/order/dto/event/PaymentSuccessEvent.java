package order_service.order.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PaymentSuccessEvent(
        @NotNull(message = "Order ID cannot be null")
        @Positive(message = "Order ID must be positive")
        Long orderId,

        @NotBlank(message = "Payment ID cannot be blank")
        @Size(max = 50, message = "Payment ID cannot exceed 50 characters")
        String paymentId,

        @NotNull(message = "Payment timestamp cannot be null")
        @PastOrPresent(message = "Payment timestamp cannot be in the future")
        LocalDateTime paymentTimestamp,

        @Size(max = 100, message = "Transaction reference cannot exceed 100 characters")
        String transactionReference,

        @Positive(message = "Amount must be positive")
        Double amount
) {

    public PaymentSuccessEvent {
        if (paymentTimestamp == null) {
            paymentTimestamp = LocalDateTime.now();
        }

        if (orderId != null && orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }

        if (amount != null && amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (paymentId != null) {
            paymentId = paymentId.trim();
            if (paymentId.isEmpty()) {
                throw new IllegalArgumentException("Payment ID cannot be empty");
            }
        }

        if (transactionReference != null) {
            transactionReference = transactionReference.trim();
        }
    }

//    public PaymentSuccessEvent(Long orderId, String paymentId) {
//        this(orderId, paymentId, LocalDateTime.now(), null, null);
//    }
//
//    public PaymentSuccessEvent(Long orderId, String paymentId, LocalDateTime paymentTimestamp) {
//        this(orderId, paymentId, paymentTimestamp, null, null);
//    }
//
//    public PaymentSuccessEvent(Long orderId, String paymentId, LocalDateTime paymentTimestamp, String transactionReference) {
//        this(orderId, paymentId, paymentTimestamp, transactionReference, null);
//    }
//
//    public boolean isRecentPayment() {
//        return paymentTimestamp.isAfter(LocalDateTime.now().minusMinutes(30));
//    }

    public boolean hasTransactionReference() {
        return transactionReference != null && !transactionReference.trim().isEmpty();
    }

    public boolean hasAmount() {
        return amount != null && amount > 0;
    }

//    public boolean isHighValuePayment() {
//        return hasAmount() && amount > 100000.0; // 100,000 units
//    }
//
//    // Static factory methods
//    public static PaymentSuccessEvent of(Long orderId, String paymentId) {
//        return new PaymentSuccessEvent(orderId, paymentId);
//    }
//
//    public static PaymentSuccessEvent withReference(Long orderId, String paymentId, String transactionReference) {
//        return new PaymentSuccessEvent(orderId, paymentId, LocalDateTime.now(), transactionReference);
//    }
//
//    public static PaymentSuccessEvent withAmount(Long orderId, String paymentId, Double amount) {
//        return new PaymentSuccessEvent(orderId, paymentId, LocalDateTime.now(), null, amount);
//    }
//
//    public static PaymentSuccessEvent full(Long orderId, String paymentId, String transactionReference, Double amount) {
//        return new PaymentSuccessEvent(orderId, paymentId, LocalDateTime.now(), transactionReference, amount);
//    }
//
//    public void validate() {
//        if (orderId == null) {
//            throw new IllegalArgumentException("Order ID cannot be null");
//        }
//        if (paymentId == null || paymentId.trim().isEmpty()) {
//            throw new IllegalArgumentException("Payment ID cannot be null or empty");
//        }
//        if (paymentTimestamp == null) {
//            throw new IllegalArgumentException("Payment timestamp cannot be null");
//        }
//        if (paymentTimestamp.isAfter(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Payment timestamp cannot be in the future");
//        }
//        if (amount != null && amount <= 0) {
//            throw new IllegalArgumentException("Amount must be positive");
//        }
//    }


//
//    public String toJsonString() {
//        return String.format(
//                "{\"orderId\": %d, \"paymentId\": \"%s\", \"timestamp\": \"%s\", \"hasReference\": %s, \"hasAmount\": %s}",
//                orderId, paymentId, paymentTimestamp, hasTransactionReference(), hasAmount()
//        );
//    }
//
//    public boolean requiresVerification() {
//        return isHighValuePayment() || !hasTransactionReference();
//    }

    @Override
    public String toString() {
        return String.format(
                "PaymentSuccessEvent[orderId=%d, paymentId=%s, timestamp=%s, hasReference=%s, hasAmount=%s]",
                orderId, paymentId, paymentTimestamp, hasTransactionReference(), hasAmount()
        );
    }
}