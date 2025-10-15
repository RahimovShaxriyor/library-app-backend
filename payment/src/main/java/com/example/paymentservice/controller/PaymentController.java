package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;
import com.example.paymentservice.service.PaymentService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "API для управления платежами и интеграции с платежными системами")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(
            summary = "Инициация платежа",
            description = "Создание нового платежа для указанного заказа"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Платеж успешно инициирован",
                    content = @Content(schema = @Schema(implementation = PaymentInitiationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные параметры запроса"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заказ не найден"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Дублирующий запрос (idempotency key violation)"
            )
    })
    @RateLimiter(name = "paymentInitiation")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @Parameter(description = "Данные для инициации платежа", required = true)
            @Valid @RequestBody PaymentInitiationRequest request,

            @Parameter(description = "Idempotency Key для предотвращения дублирования",
                    example = "unique-key-12345")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Initiating payment for order: {}, amount: {}, idempotencyKey: {}",
                request.getOrderId(), request.getAmount(), idempotencyKey);

        // Валидация idempotency key
        if (idempotencyKey != null &&
                !paymentService.validateIdempotencyKey(idempotencyKey, "PAYMENT_INITIATION")) {
            log.warn("Duplicate payment initiation request with idempotency key: {}", idempotencyKey);
            return ResponseEntity.status(409).build();
        }

        PaymentInitiationResponse response = paymentService.initiatePayment(request, idempotencyKey);

        log.info("Payment initiated successfully: paymentId: {}, paymentUrl: {}",
                response.getPaymentId(), response.getPaymentUrl());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}/status")
    @Operation(
            summary = "Получение статуса платежа",
            description = "Получение текущего статуса платежа по его ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус платежа получен"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Платеж не найден"
            )
    })
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @Parameter(description = "ID платежа", example = "123", required = true)
            @PathVariable Long paymentId) {

        log.info("Getting payment status for: {}", paymentId);
        Map<String, Object> status = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/order/{orderId}")
    @Operation(
            summary = "Получение информации о платеже по orderId",
            description = "Получение детальной информации о платеже по ID заказа"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация о платеже получена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Платеж для указанного заказа не найден"
            )
    })
    public ResponseEntity<Map<String, Object>> getPaymentByOrderId(
            @Parameter(description = "ID заказа", example = "456", required = true)
            @PathVariable Long orderId) {

        log.info("Getting payment for order: {}", orderId);
        Map<String, Object> paymentInfo = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(paymentInfo);
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(
            summary = "Отмена платежа",
            description = "Отмена pending платежа"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Платеж успешно отменен"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невозможно отменить платеж (неверный статус)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Платеж не найден"
            )
    })
    public ResponseEntity<Map<String, Object>> cancelPayment(
            @Parameter(description = "ID платежа", example = "123", required = true)
            @PathVariable Long paymentId,

            @Parameter(
                    description = "Причина отмены",
                    schema = @Schema(example = "{\"reason\": \"Cancelled by user\"}")
            )
            @RequestBody(required = false) Map<String, String> cancelRequest) {

        String reason = cancelRequest != null ? cancelRequest.get("reason") : "Cancelled by user";
        log.info("Cancelling payment: {}, reason: {}", paymentId, reason);
        Map<String, Object> result = paymentService.cancelPayment(paymentId, reason);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Проверка работоспособности платежного сервиса"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Сервис работает корректно"
    )
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("Health check requested");
        Map<String, Object> healthInfo = paymentService.getHealthInfo();
        return ResponseEntity.ok(healthInfo);
    }

    @GetMapping("/order/{orderId}/history")
    @Operation(
            summary = "Получение истории платежей",
            description = "Получение истории всех платежей для указанного заказа"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "История платежей получена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заказ не найден"
            )
    })
    public ResponseEntity<Map<String, Object>> getPaymentHistory(
            @Parameter(description = "ID заказа", example = "456", required = true)
            @PathVariable Long orderId) {

        log.info("Getting payment history for order: {}", orderId);
        Map<String, Object> history = paymentService.getPaymentHistory(orderId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(
            summary = "Возврат средств",
            description = "Инициация возврата средств для завершенного платежа"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Возврат успешно инициирован"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невозможно выполнить возврат (неверный статус или сумма)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Платеж не найден"
            )
    })
    public ResponseEntity<Map<String, Object>> refundPayment(
            @Parameter(description = "ID платежа", example = "123", required = true)
            @PathVariable Long paymentId,

            @Parameter(
                    description = "Данные для возврата",
                    required = true,
                    schema = @Schema(example = "{\"reason\": \"Product return\", \"amount\": \"100.00\"}")
            )
            @RequestBody Map<String, String> refundRequest,

            @Parameter(description = "Idempotency Key для предотвращения дублирования")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        String reason = refundRequest.get("reason");
        String amountStr = refundRequest.get("amount");

        log.info("Processing refund for payment: {}, reason: {}, amount: {}, idempotencyKey: {}",
                paymentId, reason, amountStr, idempotencyKey);

        // Валидация idempotency key
        if (idempotencyKey != null &&
                !paymentService.validateIdempotencyKey(idempotencyKey, "PAYMENT_REFUND")) {
            log.warn("Duplicate refund request with idempotency key: {}", idempotencyKey);
            return ResponseEntity.status(409).build();
        }

        Map<String, Object> result = paymentService.refundPayment(paymentId, reason, amountStr, idempotencyKey);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/providers")
    @Operation(
            summary = "Получение списка доступных платежных провайдеров",
            description = "Возвращает список поддерживаемых платежных систем и их параметры"
    )
    @ApiResponse(responseCode = "200", description = "Список провайдеров получен")
    public ResponseEntity<Map<String, Object>> getAvailableProviders() {
        log.info("Getting available payment providers");
        Map<String, Object> providers = paymentService.getAvailableProviders();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/metrics")
    @Operation(
            summary = "Метрики платежного сервиса",
            description = "Получение метрик и статистики платежного сервиса"
    )
    @ApiResponse(responseCode = "200", description = "Метрики успешно получены")
    public ResponseEntity<Map<String, Object>> getPaymentMetrics() {
        log.info("Getting payment metrics");
        Map<String, Object> metrics = paymentService.getPaymentMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/cleanup/idempotency-keys")
    @Operation(
            summary = "Очистка просроченных Idempotency Keys",
            description = "Ручной запуск очистки просроченных idempotency keys"
    )
    @ApiResponse(responseCode = "200", description = "Очистка выполнена")
    public ResponseEntity<Map<String, Object>> cleanupIdempotencyKeys() {
        log.info("Manual cleanup of expired idempotency keys requested");
        paymentService.cleanupExpiredIdempotencyKeys();
        return ResponseEntity.ok(Map.of("status", "cleanup_completed"));
    }
}