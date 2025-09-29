package com.example.paymentservice.dto;

public class PaymentInitiationResponse {

    private String paymentUrl;
    private Long paymentId;

    public PaymentInitiationResponse(String paymentUrl, Long paymentId) {
        this.paymentUrl = paymentUrl;
        this.paymentId = paymentId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }
}
