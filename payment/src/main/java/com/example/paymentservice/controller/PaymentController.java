package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentInitiationRequest;
import com.example.paymentservice.dto.PaymentInitiationResponse;
import com.example.paymentservice.dto.payme.PaymeRequest;
import com.example.paymentservice.dto.payme.PaymeResponse;
import com.example.paymentservice.service.PaymeMerchantService;
import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    private final PaymeMerchantService paymeMerchantService;

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
            )
    })
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @Parameter(description = "Данные для инициации платежа", required = true)
            @Valid @RequestBody PaymentInitiationRequest request) {

        log.info("Initiating payment for order: {}, amount: {}", request.getOrderId(), request.getAmount());
        PaymentInitiationResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback/{provider}")
    @Operation(
            summary = "Обработка callback от платежных провайдеров",
            description = "Endpoint для получения уведомлений от платежных систем"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Callback успешно обработан"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат callback данных"
            )
    })
    public ResponseEntity<Void> handleCallback(
            @Parameter(description = "Платежный провайдер", example = "payme", required = true)
            @PathVariable String provider,

            @Parameter(description = "Данные callback от платежной системы", required = true)
            @RequestBody Map<String, Object> callbackData) {

        log.info("Received callback from provider: {}, data: {}", provider, callbackData);
        paymentService.handlePaymentCallback(provider, callbackData);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payme")
    @Operation(
            summary = "Обработка запросов Payme",
            description = "Основной endpoint для интеграции с платежной системой Payme",
            security = @SecurityRequirement(name = "Authorization")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Запрос успешно обработан",
                    content = @Content(schema = @Schema(implementation = PaymeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неавторизованный доступ"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера"
            )
    })
    public ResponseEntity<PaymeResponse> handlePaymeRequest(
            @Parameter(
                    description = "Authorization header для аутентификации",
                    example = "Basic dGVzdDp0ZXN0",
                    required = false
            )
            @RequestHeader(value = "Authorization", required = false) String authorization,

            @Parameter(
                    description = "Запрос от платежной системы Payme",
                    required = true,
                    schema = @Schema(implementation = PaymeRequest.class)
            )
            @RequestBody PaymeRequest request) {

        log.info("Received Payme request: method={}, id={}", request.getMethod(), request.getId());
        PaymeResponse response = paymeMerchantService.handleRequest(authorization, request);
        log.info("Sending Payme response for method: {}, id: {}", request.getMethod(), request.getId());
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
    public ResponseEntity<Map<String, String>> healthCheck() {
        log.debug("Health check requested");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "payment-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
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
            @RequestBody Map<String, String> refundRequest) {

        String reason = refundRequest.get("reason");
        String amountStr = refundRequest.get("amount");

        log.info("Processing refund for payment: {}, reason: {}, amount: {}",
                paymentId, reason, amountStr);

        Map<String, Object> result = paymentService.refundPayment(paymentId, reason, amountStr);
        return ResponseEntity.ok(result);
    }
}