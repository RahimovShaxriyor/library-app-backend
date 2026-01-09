package com.example.paymentservice.dto.payme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymeParams {
    private String id;
    private Long time;
    private Integer reason;
    private Long from;
    private Long to;

    @JsonProperty("amount")
    private Amount amount;

    @JsonProperty("account")
    private Account account;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private Long value;
        private String currency;

        // Конструкторы для удобства
        public Amount() {}

        public Amount(Long value, String currency) {
            this.value = value;
            this.currency = currency;
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Account {
        @JsonProperty("order_id")
        private String orderId;

        private String phone;
        private String email;
        private String user_id;

        public Account() {}

        public Account(String orderId) {
            this.orderId = orderId;
        }
    }

    // Вспомогательные методы
    public String getOrderId() {
        return account != null ? account.getOrderId() : null;
    }

    public Long getAmountValue() {
        return amount != null ? amount.getValue() : null;
    }

    public String getAmountCurrency() {
        return amount != null ? amount.getCurrency() : null;
    }

    public boolean hasValidAccount() {
        return account != null && account.getOrderId() != null && !account.getOrderId().trim().isEmpty();
    }

    public boolean hasValidAmount() {
        return amount != null && amount.getValue() != null && amount.getValue() > 0;
    }

    public boolean hasValidId() {
        return id != null && !id.trim().isEmpty();
    }
}