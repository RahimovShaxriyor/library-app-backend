package com.example.paymentservice.dto.payme;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymeResponse {

    private final Map<String, Object> result;
    private final Map<String, Object> error;

    private String id;

    @JsonProperty("jsonrpc")
    private final String jsonRpc = "2.0";

    private PaymeResponse(String id, Map<String, Object> result, Map<String, Object> error) {
        this.id = id;
        this.result = result;
        this.error = error;
    }

    // Success responses
    public static PaymeResponse allow(String id) {
        return new PaymeResponse(id, Map.of("allow", true), null);
    }

    public static PaymeResponse allow(String id, Object detail) {
        return new PaymeResponse(id, Map.of("allow", true, "detail", detail), null);
    }

    public static PaymeResponse successCreate(String id, long createTime, String transaction, int state) {
        return new PaymeResponse(id, Map.of(
                "create_time", createTime,
                "transaction", transaction,
                "state", state
        ), null);
    }

    public static PaymeResponse successPerform(String id, long performTime, String transaction, int state) {
        return new PaymeResponse(id, Map.of(
                "perform_time", performTime,
                "transaction", transaction,
                "state", state
        ), null);
    }

    public static PaymeResponse successCancel(String id, long cancelTime, String transaction, int state) {
        return new PaymeResponse(id, Map.of(
                "cancel_time", cancelTime,
                "transaction", transaction,
                "state", state
        ), null);
    }

    public static PaymeResponse successCancel(String id, long cancelTime, String transaction, int state, Integer reason) {
        Map<String, Object> result = Map.of(
                "cancel_time", cancelTime,
                "transaction", transaction,
                "state", state
        );
        if (reason != null) {
            result = Map.of(
                    "cancel_time", cancelTime,
                    "transaction", transaction,
                    "state", state,
                    "reason", reason
            );
        }
        return new PaymeResponse(id, result, null);
    }

    public static PaymeResponse successCheck(String id, String transaction, long createTime, long performTime, long cancelTime, int state) {
        return new PaymeResponse(id, Map.of(
                "transaction", transaction,
                "create_time", createTime,
                "perform_time", performTime,
                "cancel_time", cancelTime,
                "state", state
        ), null);
    }

    public static PaymeResponse successStatement(String id, Object transactions) {
        return new PaymeResponse(id, Map.of("transactions", transactions), null);
    }

    // Error responses
    public static PaymeResponse error(String id, PaymeResponseMessage.PaymeError paymeError) {
        return new PaymeResponse(id, null, Map.of(
                "code", paymeError.code(),
                "message", paymeError.message()
        ));
    }

    public static PaymeResponse error(String id, PaymeResponseMessage.PaymeError paymeError, String language) {
        Map<String, String> messageMap = paymeError.message();
        String message = messageMap.getOrDefault(language, messageMap.get("ru"));
        return new PaymeResponse(id, null, Map.of(
                "code", paymeError.code(),
                "message", message
        ));
    }

    public static PaymeResponse error(String id, PaymeResponseMessage.PaymeError paymeError, Object data) {
        return new PaymeResponse(id, null, Map.of(
                "code", paymeError.code(),
                "message", paymeError.message(),
                "data", data
        ));
    }

    // Getters
    public Map<String, Object> getResult() {
        return result;
    }

    public Map<String, Object> getError() {
        return error;
    }

    public String getJsonRpc() {
        return jsonRpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}