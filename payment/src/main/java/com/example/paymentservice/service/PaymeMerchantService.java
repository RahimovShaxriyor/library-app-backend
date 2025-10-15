package com.example.paymentservice.service;

import com.example.paymentservice.dto.payme.PaymeRequest;
import com.example.paymentservice.dto.payme.PaymeResponse;

import java.util.Map;

public interface PaymeMerchantService {

    /**
     * Основной метод обработки запросов от Payme
     */
    PaymeResponse handleRequest(String authorization, PaymeRequest request, String clientIp, String requestId);

    /**
     * Аудит входящего запроса
     */
    void auditRequest(String requestId, PaymeRequest request, String clientIp, String direction);

    /**
     * Аудит исходящего ответа
     */
    void auditResponse(String requestId, PaymeResponse response, String direction);

    /**
     * Получение информации о состоянии сервиса
     */
    Map<String, Object> getHealthInfo();

    /**
     * Получение статистики по транзакциям за указанный период
     * @param days количество дней для статистики
     */
    Map<String, Object> getStatistics(int days);

    /**
     * Валидация IP-адреса клиента
     */
    boolean validateIpAddress(String clientIp);

    /**
     * Проверка учетных данных мерчанта
     */
    boolean verifyMerchantCredentials(String authorizationHeader);

    /**
     * Очистка просроченных транзакций (вызывается по расписанию)
     */
    void cleanupExpiredTransactions();

    /**
     * Повторная обработка неудачной транзакции
     * @param transactionId ID транзакции в системе Payme
     */
    Map<String, Object> retryFailedTransaction(String transactionId);

    /**
     * Получение детальной информации о транзакции
     * @param transactionId ID транзакции в системе Payme
     */
    Map<String, Object> getTransactionDetails(String transactionId);

    /**
     * Получение списка транзакций по заказу
     * @param orderId ID заказа
     */
    Map<String, Object> getTransactionsByOrder(Long orderId);

    /**
     * Проверка доступности сервиса Payme
     */
    boolean isServiceAvailable();

    /**
     * Получение метрик производительности
     */
    Map<String, Object> getPerformanceMetrics();

    /**
     * Принудительная отмена транзакции
     * @param transactionId ID транзакции в системе Payme
     * @param reason причина отмены
     */
    Map<String, Object> forceCancelTransaction(String transactionId, String reason);
}