package com.example.paymentservice.dto.payme;

import java.util.Map;

public class PaymeResponseMessage {

    public record PaymeError(int code, Map<String, String> message) {}

    // System Errors
    public static final PaymeError SYSTEM_ERROR = new PaymeError(-32400, Map.of(
            "ru", "Системная ошибка",
            "uz", "Tizim xatosi",
            "en", "System error"
    ));

    public static final PaymeError INVALID_JSON_RPC = new PaymeError(-32600, Map.of(
            "ru", "Неверный JSON-RPC объект",
            "uz", "Noto'g'ri JSON-RPC ob'ekti",
            "en", "Invalid JSON-RPC object"
    ));

    public static final PaymeError METHOD_NOT_FOUND = new PaymeError(-32601, Map.of(
            "ru", "Метод не найден",
            "uz", "Metod topilmadi",
            "en", "Method not found"
    ));

    public static final PaymeError INVALID_ACCESS_DATA = new PaymeError(-32504, Map.of(
            "ru", "Неверные данные доступа",
            "uz", "Kirish ma'lumotlari noto'g'ri",
            "en", "Invalid access data"
    ));

    public static final PaymeError INVALID_AUTHORIZATION = new PaymeError(-32504, Map.of(
            "ru", "Неверная авторизация",
            "uz", "Avtorizatsiyada xatolik",
            "en", "Invalid authorization"
    ));

    // Transaction Errors
    public static final PaymeError TRANSACTION_NOT_FOUND = new PaymeError(-31003, Map.of(
            "ru", "Транзакция не найдена",
            "uz", "Tranzaksiya topilmadi",
            "en", "Transaction not found"
    ));

    public static final PaymeError TRANSACTION_ALREADY_EXISTS = new PaymeError(-31051, Map.of(
            "ru", "Транзакция уже существует",
            "uz", "Tranzaksiya allaqachon mavjud",
            "en", "Transaction already exists"
    ));

    public static final PaymeError CANNOT_PERFORM_OPERATION = new PaymeError(-31008, Map.of(
            "ru", "Невозможно выполнить операцию",
            "uz", "Operatsiyani bajarib bo'lmadi",
            "en", "Cannot perform operation"
    ));

    public static final PaymeError ORDER_NOT_FOUND = new PaymeError(-31050, Map.of(
            "ru", "Заказ не найден",
            "uz", "Buyurtma topilmadi",
            "en", "Order not found"
    ));

    public static final PaymeError INVALID_ORDER_ID = new PaymeError(-31050, Map.of(
            "ru", "Неверный идентификатор заказа",
            "uz", "Buyurtma identifikatori noto'g'ri",
            "en", "Invalid order ID"
    ));

    public static final PaymeError INVALID_AMOUNT = new PaymeError(-31001, Map.of(
            "ru", "Неверная сумма",
            "uz", "Summa noto'g'ri",
            "en", "Invalid amount"
    ));

    public static final PaymeError UNABLE_TO_CANCEL = new PaymeError(-31007, Map.of(
            "ru", "Невозможно отменить транзакцию",
            "uz", "Tranzaksiyani bekor qilib bo'lmadi",
            "en", "Unable to cancel transaction"
    ));

    public static final PaymeError UNABLE_TO_PERFORM = new PaymeError(-31008, Map.of(
            "ru", "Невозможно выполнить транзакцию",
            "uz", "Tranzaksiyani bajarib bo'lmadi",
            "en", "Unable to perform transaction"
    ));

    public static final PaymeError TRANSACTION_TIMEOUT = new PaymeError(-31009, Map.of(
            "ru", "Транзакция превысила время ожидания",
            "uz", "Tranzaksiya kutish vaqtidan oshdi",
            "en", "Transaction timeout"
    ));

    // Account Errors
    public static final PaymeError INVALID_ACCOUNT = new PaymeError(-31050, Map.of(
            "ru", "Неверный аккаунт",
            "uz", "Hisob noto'g'ri",
            "en", "Invalid account"
    ));

    public static final PaymeError ORDER_ALREADY_PAID = new PaymeError(-31051, Map.of(
            "ru", "Заказ уже оплачен",
            "uz", "Buyurtma allaqachon to'langan",
            "en", "Order already paid"
    ));

    public static final PaymeError ORDER_EXPIRED = new PaymeError(-31052, Map.of(
            "ru", "Срок действия заказа истек",
            "uz", "Buyurtma muddati tugadi",
            "en", "Order expired"
    ));

    public static final PaymeError ORDER_CANCELLED = new PaymeError(-31053, Map.of(
            "ru", "Заказ отменен",
            "uz", "Buyurtma bekor qilindi",
            "en", "Order cancelled"
    ));

    // Merchant Errors
    public static final PaymeError MERCHANT_NOT_FOUND = new PaymeError(-31054, Map.of(
            "ru", "Мерчант не найден",
            "uz", "Merchant topilmadi",
            "en", "Merchant not found"
    ));

    public static final PaymeError MERCHANT_INACTIVE = new PaymeError(-31055, Map.of(
            "ru", "Мерчант неактивен",
            "uz", "Merchant faol emas",
            "en", "Merchant inactive"
    ));

    // Payment Method Errors
    public static final PaymeError PAYMENT_METHOD_DISABLED = new PaymeError(-31056, Map.of(
            "ru", "Метод оплаты отключен",
            "uz", "To'lov usuli o'chirilgan",
            "en", "Payment method disabled"
    ));

    public static final PaymeError INSUFFICIENT_FUNDS = new PaymeError(-31057, Map.of(
            "ru", "Недостаточно средств",
            "uz", "Mablag' yetarli emas",
            "en", "Insufficient funds"
    ));

    // Technical Errors
    public static final PaymeError NETWORK_ERROR = new PaymeError(-31058, Map.of(
            "ru", "Ошибка сети",
            "uz", "Tarmoq xatosi",
            "en", "Network error"
    ));

    public static final PaymeError EXTERNAL_SERVICE_ERROR = new PaymeError(-31059, Map.of(
            "ru", "Ошибка внешнего сервиса",
            "uz", "Tashqi xizmat xatosi",
            "en", "External service error"
    ));

    // Additional Payme specific errors
    public static final PaymeError INVALID_ACCOUNT_FORMAT = new PaymeError(-31050, Map.of(
            "ru", "Неверный формат аккаунта",
            "uz", "Hisob formati noto'g'ri",
            "en", "Invalid account format"
    ));

    public static final PaymeError TRANSACTION_IN_PROGRESS = new PaymeError(-31099, Map.of(
            "ru", "Транзакция в процессе выполнения",
            "uz", "Tranzaksiya bajarilmoqda",
            "en", "Transaction in progress"
    ));

    public static final PaymeError TRANSACTION_CANCELLED = new PaymeError(-31012, Map.of(
            "ru", "Транзакция отменена",
            "uz", "Tranzaksiya bekor qilindi",
            "en", "Transaction cancelled"
    ));

    // Success Messages (for reference)
    public static final Map<String, String> SUCCESS_MESSAGE = Map.of(
            "ru", "Операция выполнена успешно",
            "uz", "Operatsiya muvaffaqiyatli bajarildi",
            "en", "Operation completed successfully"
    );

    // Helper method to get error message by language
    public static String getErrorMessage(PaymeError error, String language) {
        if (error == null || error.message() == null) {
            return "Unknown error";
        }
        String lang = language != null ? language.toLowerCase() : "ru";
        return error.message().getOrDefault(lang, error.message().get("ru"));
    }

    // Helper method to get error by code
    public static PaymeError getErrorByCode(int code) {
        return switch (code) {
            case -32400 -> SYSTEM_ERROR;
            case -32600 -> INVALID_JSON_RPC;
            case -32601 -> METHOD_NOT_FOUND;
            case -32504 -> INVALID_AUTHORIZATION;
            case -31003 -> TRANSACTION_NOT_FOUND;
            case -31050 -> INVALID_ACCOUNT;
            case -31051 -> TRANSACTION_ALREADY_EXISTS;
            case -31008 -> CANNOT_PERFORM_OPERATION;
            case -31001 -> INVALID_AMOUNT;
            case -31007 -> UNABLE_TO_CANCEL;
            case -31009 -> TRANSACTION_TIMEOUT;
            case -31054 -> MERCHANT_NOT_FOUND;
            case -31055 -> MERCHANT_INACTIVE;
            case -31056 -> PAYMENT_METHOD_DISABLED;
            case -31057 -> INSUFFICIENT_FUNDS;
            case -31058 -> NETWORK_ERROR;
            case -31059 -> EXTERNAL_SERVICE_ERROR;
            case -31099 -> TRANSACTION_IN_PROGRESS;
            case -31012 -> TRANSACTION_CANCELLED;
            default -> SYSTEM_ERROR;
        };
    }

    // Helper method to create custom error
    public static PaymeError createCustomError(int code, String ruMessage, String uzMessage, String enMessage) {
        return new PaymeError(code, Map.of(
                "ru", ruMessage,
                "uz", uzMessage,
                "en", enMessage
        ));
    }

    // Check if error code is related to transaction
    public static boolean isTransactionError(int code) {
        return code >= -31099 && code <= -31001;
    }

    // Check if error code is related to system
    public static boolean isSystemError(int code) {
        return code <= -32000 && code >= -32699;
    }

    // Check if error code is related to authorization
    public static boolean isAuthorizationError(int code) {
        return code == -32504;
    }
}