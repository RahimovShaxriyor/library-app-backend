package com.example.paymentservice.dto.payme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymeRequest {
    private String method;
    private PaymeParams params;
}

