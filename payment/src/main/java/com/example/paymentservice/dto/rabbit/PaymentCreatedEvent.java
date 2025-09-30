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
public class PaymentCreatedEvent implements Serializable {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt;

    public PaymentCreatedEvent(Long orderId, BigDecimal amount, String currency) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = "PAYME";
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public static PaymentCreatedEvent of(Long orderId, Long paymentId, BigDecimal amount, String currency) {
        PaymentCreatedEvent event = new PaymentCreatedEvent();
        event.setOrderId(orderId);
        event.setPaymentId(paymentId);
        event.setAmount(amount);
        event.setCurrency(currency);
        event.setPaymentMethod("PAYME");
        event.setStatus("PENDING");
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}