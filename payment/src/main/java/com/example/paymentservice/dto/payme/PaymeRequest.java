package com.example.paymentservice.dto.payme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymeRequest {
    private String id;
    private String method;
    private PaymeParams params;

    @JsonProperty("jsonrpc")
    private String jsonRpc = "2.0";
}