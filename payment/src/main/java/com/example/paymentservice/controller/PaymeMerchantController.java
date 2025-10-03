package com.example.paymentservice.controller;

import com.example.paymentservice.dto.payme.PaymeRequest;
import com.example.paymentservice.dto.payme.PaymeResponse;
import com.example.paymentservice.service.PaymeMerchantService;
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
            @RequestBody PaymeRequest request) {

        log.info("Received Payme request: method={}, id={}",
                request.getMethod(), request.getId());

        PaymeResponse response = paymeMerchantService.handleRequest(authorization, request);

        log.info("Sending Payme response: id={}, result={}",
                response.getId(), response.getResult() != null ? "success" : "error");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Проверка здоровья сервиса Payme",
            description = "Endpoint для проверки доступности сервиса обработки платежей Payme")
    @ApiResponse(responseCode = "200", description = "Сервис работает корректно")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Payme Merchant Service is healthy");
    }
}