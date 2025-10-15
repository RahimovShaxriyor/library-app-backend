package com.example.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@ToString
@Schema(description = "DTO для запроса с Idempotency Key")
public class IdempotencyRequest {

    @Schema(description = "Idempotency Key для предотвращения дублирования запросов",
            example = "unique-key-12345", required = true)
    private String idempotencyKey;

    @Schema(description = "Тип операции", example = "PAYMENT_INITIATION")
    private String operationType;

    @Schema(description = "Время создания запроса")
    private Long timestamp;
}