package com.example.paymentservice.dto.click;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class ClickRequestDto {
    @JsonProperty("click_trans_id")
    private Long clickTransId;

    @JsonProperty("service_id")
    private Integer serviceId;

    @JsonProperty("click_paydoc_id")
    private Long clickPaydocId;

    @JsonProperty("merchant_trans_id")
    private String merchantTransId;

    @JsonProperty("merchant_prepare_id")
    private Long merchantPrepareId;

    private BigDecimal amount;

    private Integer action;

    private Integer error;

    @JsonProperty("error_note")
    private String errorNote;

    @JsonProperty("sign_time")
    private String signTime;

    @JsonProperty("sign_string")
    private String signString;
}
