package com.example.paymentservice.dto.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent implements Serializable {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String providerTransactionId;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;

//    public PaymentSuccessEvent(Long orderId) {
//        this.orderId = orderId;
//        this.paymentDate = LocalDateTime.now();
//        this.createdAt = LocalDateTime.now();
//    }

    public PaymentSuccessEvent(Long orderId, Long paymentId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.paymentDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public PaymentSuccessEvent(Long paymentId, Long orderId, BigDecimal amount, String currency,
                               String providerTransactionId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.providerTransactionId = providerTransactionId;
        this.paymentMethod = "PAYME";
        this.paymentDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // Статический фабричный метод для удобства создания
    public static PaymentSuccessEvent of(Long orderId, Long paymentId) {
        return new PaymentSuccessEvent(paymentId, orderId);
    }

    public static PaymentSuccessEvent of(Long orderId, Long paymentId, BigDecimal amount,
                                         String currency, String providerTransactionId) {
        return new PaymentSuccessEvent(paymentId, orderId, amount, currency, providerTransactionId);
    }
}