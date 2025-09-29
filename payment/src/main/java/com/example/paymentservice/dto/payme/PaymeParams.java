package com.example.paymentservice.dto.payme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymeParams {
    private String id;
    private Long time;
    private BigDecimal amount;
    private Map<String, Long> account;
    private Integer reason;
}

