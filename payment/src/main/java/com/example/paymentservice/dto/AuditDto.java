package com.example.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Schema(description = "DTO для аудита запросов/ответов")
public class AuditDto {

    @Schema(description = "Уникальный идентификатор запроса")
    private String requestId;

    @Schema(description = "Тип операции (INCOMING/OUTGOING)")
    private String direction;

    @Schema(description = "Платежный провайдер")
    private String provider;

    @Schema(description = "Метод/действие")
    private String method;

    @Schema(description = "IP адрес клиента")
    private String clientIp;

    @Schema(description = "Тело запроса/ответа")
    private String payload;

    @Schema(description = "Статус ответа")
    private Integer responseStatus;

    @Schema(description = "Время обработки в мс")
    private Long processingTime;

    @Schema(description = "Временная метка создания")
    private LocalDateTime createdAt;

    @Schema(description = "Дополнительные метаданные")
    private String metadata;
}