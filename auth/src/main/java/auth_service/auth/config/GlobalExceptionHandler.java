package auth_service.auth.config;

import auth_service.auth.exception.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // 409
    public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        return new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                new Date(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", new Date());
        body.put("status", HttpStatus.BAD_REQUEST.value());

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
                ));
        body.put("errors", errors);
        body.put("message", "Ошибка валидации");

        return body;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGlobalException(Exception ex, WebRequest request) {
        return new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                new Date(),
                "Произошла внутренняя ошибка сервера. Пожалуйста, попробуйте позже.",
                request.getDescription(false).replace("uri=", "")
        );
    }

    public static class ErrorResponse {
        public int statusCode;
        public Date timestamp;
        public String message;
        public String path;

        public ErrorResponse(int statusCode, Date timestamp, String message, String path) {
            this.statusCode = statusCode;
            this.timestamp = timestamp;
            this.message = message;
            this.path = path;
        }
    }
}

