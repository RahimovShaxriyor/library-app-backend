package auth_service.auth.dto;

import java.time.Instant;


public class ErrorResponse {

    private final int statusCode;
    private final Instant timestamp;
    private final String message;
    private final String description;

    public ErrorResponse(int statusCode, String message, String description) {
        this.statusCode = statusCode;
        this.message = message;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
