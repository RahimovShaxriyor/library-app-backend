package com.example.paymentservice.dto.click;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@Schema(description = "DTO для входящих запросов от платежной системы Click")
public class ClickRequestDto {

    @JsonProperty("click_trans_id")
    @Schema(description = "Уникальный идентификатор транзакции в системе Click", example = "123456789")
    private Long clickTransId;

    @JsonProperty("service_id")
    @Schema(description = "ID мерчанта в системе Click", example = "12345")
    private Integer serviceId;

    @JsonProperty("click_paydoc_id")
    @Schema(description = "ID платежного документа в системе Click", example = "987654321")
    private Long clickPaydocId;

    @JsonProperty("merchant_trans_id")
    @Schema(description = "ID транзакции в системе мерчанта", example = "order_123")
    private String merchantTransId;

    @JsonProperty("merchant_prepare_id")
    @Schema(description = "ID подготовительной транзакции", example = "456789")
    private Long merchantPrepareId;

    @Schema(description = "Сумма платежа", example = "100000.00")
    private BigDecimal amount;

    @Schema(description = "Тип действия: 0 - подготовка, 1 - завершение", example = "1")
    private Integer action;

    @Schema(description = "Код ошибки", example = "0")
    private Integer error;

    @JsonProperty("error_note")
    @Schema(description = "Описание ошибки", example = "Transaction not found")
    private String errorNote;

    @JsonProperty("sign_time")
    @Schema(description = "Время подписи в формате UNIX timestamp", example = "1636543200")
    private String signTime;

    @JsonProperty("sign_string")
    @Schema(description = "Цифровая подпись для проверки запроса",
            example = "a1b2c3d4e5f6g7h8i9j0")
    private String signString;

    @Schema(description = "Валюта платежа", example = "UZS")
    private String currency;

    @JsonProperty("merchant_id")
    @Schema(description = "ID мерчанта", example = "123")
    private Integer merchantId;

    @JsonProperty("system_time")
    @Schema(description = "Системное время транзакции", example = "1636543200")
    private Long systemTime;

    @JsonProperty("phone_number")
    @Schema(description = "Номер телефона клиента", example = "+998901234567")
    private String phoneNumber;
}