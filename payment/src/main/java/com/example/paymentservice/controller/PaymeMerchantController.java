package com.example.paymentservice.controller;

import com.example.paymentservice.dto.payme.PaymeRequest;
import com.example.paymentservice.dto.payme.PaymeResponse;
import com.example.paymentservice.service.PaymeMerchantService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/merchant/payme")
@RequiredArgsConstructor
@Tag(name = "Payme Merchant API", description = "API для обработки запросов от платежной системы Payme")
public class PaymeMerchantController {

    private final PaymeMerchantService paymeMerchantService;

    @PostMapping
    @Operation(
            summary = "Обработка запросов Payme",
            description = "Основной endpoint для обработки всех входящих запросов от платежной системы Payme",
            security = @SecurityRequirement(name = "Authorization")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная обработка запроса",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат запроса"
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
    @RateLimiter(name = "paymeApi")
    public ResponseEntity<PaymeResponse> handlePaymeRequest(
            @Parameter(
                    description = "Authorization header для аутентификации",
                    example = "Basic dGVzdDp0ZXN0",
                    required = false
            )
            @RequestHeader(value = "Authorization", required = false) String authorization,

            @Parameter(
                    description = "Тело запроса от Payme",
                    required = true,
                    schema = @Schema(implementation = PaymeRequest.class)
            )
            @RequestBody PaymeRequest request,

            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        String requestId = generateRequestId();

        log.info("Received Payme request [{}] from {}: method={}, id={}",
                requestId, clientIp, request.getMethod(), request.getId());

        // Валидация IP адреса
        if (!paymeMerchantService.validateIpAddress(clientIp)) {
            log.warn("Blocked Payme request from unauthorized IP: {}", clientIp);
            return ResponseEntity.status(401).build();
        }

        // Валидация учетных данных мерчанта
        if (!paymeMerchantService.verifyMerchantCredentials(authorization)) {
            log.warn("Invalid merchant credentials from IP: {}", clientIp);
            return ResponseEntity.status(401).build();
        }

        paymeMerchantService.auditRequest(requestId, request, clientIp, "INCOMING");

        PaymeResponse response = paymeMerchantService.handleRequest(authorization, request, clientIp, requestId);

        paymeMerchantService.auditResponse(requestId, response, "OUTGOING");

        String errorCode = "none";
        if (response.getError() != null) {
            errorCode = String.valueOf(response.getError());
        }

        log.info("Sending Payme response [{}]: id={}, result={}, error={}",
                requestId, response.getId(),
                response.getResult() != null ? "success" : "error",
                errorCode);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Проверка здоровья сервиса Payme",
            description = "Endpoint для проверки доступности сервиса обработки платежей Payme")
    @ApiResponse(responseCode = "200", description = "Сервис работает корректно")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = paymeMerchantService.getHealthInfo();
        return ResponseEntity.ok(healthInfo);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Статистика по Payme транзакциям",
            description = "Получение статистической информации по обработке запросов Payme")
    @ApiResponse(responseCode = "200", description = "Статистика успешно получена")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @Parameter(description = "Период в днях", example = "7")
            @RequestParam(defaultValue = "7") int days) {

        Map<String, Object> statistics = paymeMerchantService.getStatistics(days);
        return ResponseEntity.ok(statistics);
    }

    @PostMapping("/transactions/{transactionId}/retry")
    @Operation(summary = "Повторная обработка неудачной транзакции",
            description = "Повторная попытка обработки неудачной транзакции Payme")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Повторная обработка запущена"),
            @ApiResponse(responseCode = "404", description = "Транзакция не найдена"),
            @ApiResponse(responseCode = "400", description = "Транзакция не может быть повторно обработана")
    })
    public ResponseEntity<Map<String, Object>> retryTransaction(
            @Parameter(description = "ID транзакции", required = true)
            @PathVariable String transactionId) {

        log.info("Retrying Payme transaction: {}", transactionId);
        Map<String, Object> result = paymeMerchantService.retryFailedTransaction(transactionId);
        return ResponseEntity.ok(result);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String generateRequestId() {
        return "payme_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}