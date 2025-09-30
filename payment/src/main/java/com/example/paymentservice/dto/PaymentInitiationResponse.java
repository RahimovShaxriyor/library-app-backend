package com.example.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiationResponse {
    private Long paymentId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String paymentUrl;
    private LocalDateTime expiresAt;
    private String message;

    public PaymentInitiationResponse(String paymentUrl, Long paymentId) {
        this.paymentUrl = paymentUrl;
        this.paymentId = paymentId;
    }
}

