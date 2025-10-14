package com.example.paymentservice.dto.click;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClickResponseDto {
    @JsonProperty("click_trans_id")
    private Long clickTransId;

    @JsonProperty("merchant_trans_id")
    private String merchantTransId;

    @JsonProperty("merchant_prepare_id")
    private Long merchantPrepareId;

    @JsonProperty("merchant_confirm_id")
    private Long merchantConfirmId;

    private Integer error;

    @JsonProperty("error_note")
    private String errorNote;
}
