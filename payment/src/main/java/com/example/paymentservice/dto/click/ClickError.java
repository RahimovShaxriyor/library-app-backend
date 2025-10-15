package com.example.paymentservice.dto.click;

public enum ClickError {
    SUCCESS(0, "Success"),
    SIGN_CHECK_FAILED(-1, "SIGN CHECK FAILED!"),
    INVALID_AMOUNT(-2, "Incorrect parameter amount"),
    ACTION_NOT_FOUND(-3, "Action not found"),
    ALREADY_PAID(-4, "Already paid"),
    TRANSACTION_NOT_FOUND(-5, "Transaction not found"),
    TRANSACTION_CANCELLED(-6, "Transaction cancelled"),
    TRANSACTION_EXPIRED(-7, "Transaction expired"),
    MERCHANT_NOT_FOUND(-8, "Merchant not found"),
    TRANSACTION_ALREADY_CANCELLED(-9, "Transaction already cancelled"),
    ACTION_ALREADY_DONE(-10, "Action already done"),
    ORDER_NOT_FOUND(-11, "Order not found"),
    INVALID_ORDER_ID(-12, "Invalid order ID"),
    INVALID_PARAMETERS(-13, "Invalid parameters"),
    SYSTEM_ERROR(-14, "System error"),
    USER_NOT_FOUND(-15, "User not found"),
    INSUFFICIENT_FUNDS(-16, "Insufficient funds"),
    OPERATION_NOT_ALLOWED(-17, "Operation not allowed"),
    TIMEOUT(-18, "Timeout"),
    NETWORK_ERROR(-19, "Network error"),
    DUPLICATE_REQUEST(-20, "Duplicate request"),
    UNAUTHORIZED_IP(-21, "Unauthorized IP address"),
    SERVICE_UNAVAILABLE(-22, "Service temporarily unavailable"),
    INVALID_CURRENCY(-23, "Invalid currency"),
    LIMIT_EXCEEDED(-24, "Transaction limit exceeded"),
    CARD_DECLINED(-25, "Card declined"),
    FRAUD_SUSPECTED(-26, "Fraud suspected"),
    TECHNICAL_ERROR(-27, "Technical error"),
    REFUND_NOT_ALLOWED(-28, "Refund not allowed"),
    PARTIAL_REFUND_NOT_SUPPORTED(-29, "Partial refund not supported"),
    REFUND_AMOUNT_EXCEEDED(-30, "Refund amount exceeded");

    private final int code;
    private final String message;

    ClickError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public ClickResponseDto asResponse() {
        return new ClickResponseDto(null, null, null, null, this.code, this.message);
    }

    public ClickResponseDto asResponseWithTransaction(Long clickTransId, String merchantTransId) {
        return new ClickResponseDto(clickTransId, merchantTransId, null, null, this.code, this.message);
    }

    public static ClickError fromCode(int code) {
        for (ClickError error : values()) {
            if (error.code == code) {
                return error;
            }
        }
        return SYSTEM_ERROR;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isClientError() {
        return code >= -30 && code <= -1;
    }

    public boolean isSystemError() {
        return code <= -31;
    }

    @Override
    public String toString() {
        return "ClickError{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}