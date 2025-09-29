package com.example.paymentservice.dto.payme;

import com.example.paymentservice.service.OrderServiceClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymeResponse {

    private final Map<String, Object> result;
    private final Map<String, Object> error;

    @JsonProperty("jsonrpc")
    private final String jsonRpc = "2.0";

    private PaymeResponse(Map<String, Object> result, Map<String, Object> error) {
        this.result = result;
        this.error = error;
    }

    public static PaymeResponse allow(OrderServiceClient.OrderDetail detail) {
        return new PaymeResponse(Map.of("allow", true, "detail", detail), null);
    }

    public static PaymeResponse error(PaymeResponseMessage.PaymeError paymeError) {
        return new PaymeResponse(null, Map.of("code", paymeError.code(), "message", paymeError.message()));
    }

    public static PaymeResponse successCreate(long createTime, String transaction, int state) {
        return new PaymeResponse(Map.of("create_time", createTime, "transaction", transaction, "state", state), null);
    }

    public static PaymeResponse successPerform(long performTime, String transaction, int state) {
        return new PaymeResponse(Map.of("perform_time", performTime, "transaction", transaction, "state", state), null);
    }

    public static PaymeResponse successCancel(long cancelTime, String transaction, int state) {
        return new PaymeResponse(Map.of("cancel_time", cancelTime, "transaction", transaction, "state", state), null);
    }

    public static PaymeResponse successCheck(String transaction, long createTime, long performTime, long cancelTime, int state, int reason) {
        return new PaymeResponse(Map.of(
                "transaction", transaction,
                "create_time", createTime,
                "perform_time", performTime,
                "cancel_time", cancelTime,
                "state", state,
                "reason", reason
        ), null);
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public Map<String, Object> getError() {
        return error;
    }

    public String getJsonRpc() {
        return jsonRpc;
    }
}

