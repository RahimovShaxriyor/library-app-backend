package com.example.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO для ответа инициации платежа")
public class PaymentInitiationResponse {

    @Schema(description = "ID платежа в системе", example = "12345")
    private Long paymentId;

    @Schema(description = "Статус платежа", example = "PENDING")
    private String status;

    @Schema(description = "Сумма платежа", example = "100000.00")
    private BigDecimal amount;

    @Schema(description = "Валюта платежа", example = "UZS")
    private String currency;

    @Schema(description = "URL для перенаправления на страницу оплаты",
            example = "https://checkout.payme.uz/abc123")
    private String paymentUrl;

    @Schema(description = "Время истечения срока действия платежной сессии")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    @Schema(description = "Сообщение для пользователя", example = "Payment initiated successfully")
    private String message;

    @Schema(description = "Платежный провайдер", example = "PAYME")
    private String provider;

    @Schema(description = "ID заказа", example = "67890")
    private Long orderId;

    @Schema(description = "QR код для оплаты (если поддерживается провайдером)")
    private String qrCode;

    @Schema(description = "Дополнительные параметры для платежа")
    private Object additionalParams;

    @Schema(description = "Время создания платежа")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // Конструктор для обратной совместимости
    public PaymentInitiationResponse(String paymentUrl, Long paymentId) {
        this.paymentUrl = paymentUrl;
        this.paymentId = paymentId;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    // Конструктор с основными полями
    public PaymentInitiationResponse(String paymentUrl, Long paymentId, String status) {
        this.paymentUrl = paymentUrl;
        this.paymentId = paymentId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // Полный конструктор
    public PaymentInitiationResponse(Long paymentId, String status, BigDecimal amount, String currency,
                                     String paymentUrl, LocalDateTime expiresAt, String message,
                                     String provider, Long orderId) {
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.paymentUrl = paymentUrl;
        this.expiresAt = expiresAt;
        this.message = message;
        this.provider = provider;
        this.orderId = orderId;
        this.createdAt = LocalDateTime.now();
    }

    // Builder pattern методы
    public static PaymentInitiationResponseBuilder builder() {
        return new PaymentInitiationResponseBuilder();
    }

    public static class PaymentInitiationResponseBuilder {
        private Long paymentId;
        private String status;
        private BigDecimal amount;
        private String currency;
        private String paymentUrl;
        private LocalDateTime expiresAt;
        private String message;
        private String provider;
        private Long orderId;
        private String qrCode;
        private Object additionalParams;

        public PaymentInitiationResponseBuilder paymentId(Long paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public PaymentInitiationResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PaymentInitiationResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentInitiationResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PaymentInitiationResponseBuilder paymentUrl(String paymentUrl) {
            this.paymentUrl = paymentUrl;
            return this;
        }

        public PaymentInitiationResponseBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public PaymentInitiationResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public PaymentInitiationResponseBuilder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public PaymentInitiationResponseBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public PaymentInitiationResponseBuilder qrCode(String qrCode) {
            this.qrCode = qrCode;
            return this;
        }

        public PaymentInitiationResponseBuilder additionalParams(Object additionalParams) {
            this.additionalParams = additionalParams;
            return this;
        }

        public PaymentInitiationResponse build() {
            PaymentInitiationResponse response = new PaymentInitiationResponse();
            response.setPaymentId(paymentId);
            response.setStatus(status);
            response.setAmount(amount);
            response.setCurrency(currency);
            response.setPaymentUrl(paymentUrl);
            response.setExpiresAt(expiresAt);
            response.setMessage(message);
            response.setProvider(provider);
            response.setOrderId(orderId);
            response.setQrCode(qrCode);
            response.setAdditionalParams(additionalParams);
            response.setCreatedAt(LocalDateTime.now());
            return response;
        }
    }

    // Вспомогательные методы
    public boolean isSuccess() {
        return "PENDING".equals(status) || "SUCCESS".equals(status);
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public long getMinutesUntilExpiry() {
        if (expiresAt == null) return -1;
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }

    @Override
    public String toString() {
        return "PaymentInitiationResponse{" +
                "paymentId=" + paymentId +
                ", status='" + status + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentUrl='" + (paymentUrl != null ? "***" : null) + '\'' +
                ", expiresAt=" + expiresAt +
                ", provider='" + provider + '\'' +
                ", orderId=" + orderId +
                '}';
    }
}