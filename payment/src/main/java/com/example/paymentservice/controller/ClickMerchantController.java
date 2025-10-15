package com.example.paymentservice.controller;

import com.example.paymentservice.dto.click.ClickRequestDto;
import com.example.paymentservice.dto.click.ClickResponseDto;
import com.example.paymentservice.service.ClickMerchantService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/merchant/click")
@RequiredArgsConstructor
@Tag(name = "Click Merchant API", description = "API для обработки запросов от платежной системы Click")
public class ClickMerchantController {

    private final ClickMerchantService clickMerchantService;

    @PostMapping("/prepare")
    @Operation(summary = "Подготовка транзакции Click", description = "Подготовительный этап для создания транзакции")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Транзакция успешно подготовлена"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "401", description = "Неверная подпись запроса"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @RateLimiter(name = "clickApi")
    public ResponseEntity<ClickResponseDto> prepare(
            @RequestBody ClickRequestDto request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        log.info("Click Prepare request received from {}: {}", clientIp, request);

        // Валидация IP адреса
        if (!clickMerchantService.validateIpAddress(clientIp)) {
            log.warn("Blocked Click request from unauthorized IP: {}", clientIp);
            return ResponseEntity.status(401).build();
        }

        // Проверка цифровой подписи
        if (!clickMerchantService.verifySignature(request)) {
            log.warn("Invalid signature in Click Prepare request from IP: {}", clientIp);
            return ResponseEntity.status(401).build();
        }

        // Проверка на дублирующий запрос
        if (clickMerchantService.isDuplicateRequest(request.getClickTransId().toString(), Long.parseLong(request.getSignTime()))) {
            log.warn("Duplicate Click Prepare request: {}", request.getClickTransId());
            return ResponseEntity.status(409).build();
        }

        ClickResponseDto response = clickMerchantService.prepare(request, clientIp);

        log.info("Click Prepare response sent: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    @Operation(summary = "Завершение транзакции Click", description = "Завершающий этап подтверждения транзакции")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Транзакция успешно завершена"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "401", description = "Неверная подпись запроса"),
            @ApiResponse(responseCode = "404", description = "Транзакция не найдена"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @RateLimiter(name = "clickApi")
    public ResponseEntity<ClickResponseDto> complete(
            @RequestBody ClickRequestDto request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        log.info("Click Complete request received from {}: {}", clientIp, request);

        // Валидация IP адреса
        if (!clickMerchantService.validateIpAddress(clientIp)) {
            log.warn("Blocked Click request from unauthorized IP: {}", clientIp);
            return ResponseEntity.status(401).build();
        }

        // Проверка цифровой подписи
        if (!clickMerchantService.verifySignature(request)) {
            log.warn("Invalid signature in Click Complete request from IP: {}", clientIp);
            return ResponseEntity.status(401).build();
        }

        ClickResponseDto response = clickMerchantService.complete(request, clientIp);

        log.info("Click Complete response sent: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    @Operation(summary = "Возврат средств Click", description = "Инициация возврата средств через Click")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Возврат успешно инициирован"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "401", description = "Неверная подпись запроса"),
            @ApiResponse(responseCode = "404", description = "Транзакция не найдена")
    })
    @RateLimiter(name = "clickApi")
    public ResponseEntity<ClickResponseDto> refund(
            @RequestBody ClickRequestDto request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        log.info("Click Refund request received from {}: {}", clientIp, request);

        // Валидация IP адреса
        if (!clickMerchantService.validateIpAddress(clientIp)) {
            log.warn("Blocked Click refund request from unauthorized IP: {}", clientIp);
            return ResponseEntity.status(401).build();
        }

        // Проверка цифровой подписи
        if (!clickMerchantService.verifySignature(request)) {
            log.warn("Invalid signature in Click Refund request from IP: {}", clientIp);
            return ResponseEntity.status(401).build();
        }

        ClickResponseDto response = clickMerchantService.refund(request, clientIp);

        log.info("Click Refund response sent: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{clickTransId}")
    @Operation(summary = "Получение статуса транзакции Click")
    @ApiResponse(responseCode = "200", description = "Статус транзакции получен")
    public ResponseEntity<Map<String, Object>> getTransactionStatus(
            @PathVariable String clickTransId) {

        log.info("Getting Click transaction status: {}", clickTransId);
        Map<String, Object> status = clickMerchantService.getTransactionStatus(clickTransId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    @Operation(summary = "Проверка здоровья Click сервиса")
    @ApiResponse(responseCode = "200", description = "Сервис работает корректно")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = clickMerchantService.getHealthInfo();
        return ResponseEntity.ok(healthInfo);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Статистика по Click транзакциям")
    @ApiResponse(responseCode = "200", description = "Статистика успешно получена")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "7") int days) {

        Map<String, Object> statistics = clickMerchantService.getClickStatistics(days);
        return ResponseEntity.ok(statistics);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}