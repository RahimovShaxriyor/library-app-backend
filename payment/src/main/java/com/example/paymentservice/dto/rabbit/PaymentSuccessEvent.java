package com.example.paymentservice.dto.rabbit;

import java.io.Serializable;

public class PaymentSuccessEvent implements Serializable {
    private Long orderId;

    public PaymentSuccessEvent() {
    }

    public PaymentSuccessEvent(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
