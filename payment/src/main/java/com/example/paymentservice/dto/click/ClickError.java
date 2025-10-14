package com.example.paymentservice.dto.click;

public enum ClickError {
    SUCCESS(0, "Success"),
    SIGN_CHECK_FAILED(-1, "SIGN CHECK FAILED!"),
    INVALID_AMOUNT(-2, "Incorrect parameter amount"),
    TRANSACTION_NOT_FOUND(-5, "Transaction not found"),
    TRANSACTION_CANCELLED(-9, "Transaction cancelled");

    private final int code;
    private final String message;

    ClickError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ClickResponseDto asResponse() {
        return new ClickResponseDto(null, null, null, null, this.code, this.message);
    }
}
