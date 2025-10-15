package com.example.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Getter
@Setter
@ToString
@Schema(description = "DTO для Health Check ответа")
public class HealthCheckResponse {

    @Schema(description = "Статус сервиса", example = "UP")
    private String status;

    @Schema(description = "Название сервиса", example = "payment-service")
    private String service;

    @Schema(description = "Временная метка проверки")
    private String timestamp;

    @Schema(description = "Детальная информация о компонентах")
    private Map<String, ComponentHealth> components;

    @Schema(description = "Версия приложения", example = "1.0.0")
    private String version;

    @Schema(description = "Время работы сервиса в миллисекундах")
    private Long uptime;

    @Getter
    @Setter
    @ToString
    public static class ComponentHealth {
        private String status;
        private String details;
        private Map<String, Object> metrics;
    }
}