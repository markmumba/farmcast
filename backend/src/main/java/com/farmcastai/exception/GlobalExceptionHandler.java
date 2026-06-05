package com.farmcastai.exception;

import com.farmcastai.common.response.BaseResponse;
import com.farmcastai.common.response.ResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── WeatherAI upstream errors ─────────────────────────────────────────────

    @ExceptionHandler(WeatherAiClientException.class)
    public ResponseEntity<String> handleWeatherAiClientException(
            WeatherAiClientException exception, HttpServletRequest request) {

        int status = exception.getStatusCode().value();
        if (exception.getStatusCode().is5xxServerError()) {
            log.error("[{}] {} → upstream {} from WeatherAI: {}",
                    request.getMethod(), request.getRequestURI(),
                    status, truncate(exception.getResponseBody()));
        } else {
            log.warn("[{}] {} → upstream {} from WeatherAI: {}",
                    request.getMethod(), request.getRequestURI(),
                    status, truncate(exception.getResponseBody()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(Optional.ofNullable(exception.getContentType()).orElse(MediaType.APPLICATION_JSON));

        String body = exception.getResponseBody();
        if (body == null || body.isBlank()) {
            body = "{\"message\":\"WeatherAI request failed\"}";
        }

        return new ResponseEntity<>(body, headers, exception.getStatusCode());
    }

    // ── Application configuration errors ─────────────────────────────────────

    @ExceptionHandler(WeatherAiConfigurationException.class)
    public ResponseEntity<BaseResponse<Void>> handleWeatherAiConfigurationException(
            WeatherAiConfigurationException exception, HttpServletRequest request) {

        log.error("[{}] {} → configuration error: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());

        return ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    // ── Validation errors (path/query params via @Validated) ─────────────────

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception, HttpServletRequest request) {

        // Strip the method.paramName prefix → readable "lat: must be ≥ -90.0"
        String message = exception.getConstraintViolations().stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return field + ": " + cv.getMessage();
                })
                .collect(Collectors.joining(", "));

        log.warn("[{}] {} → constraint violation: {}",
                request.getMethod(), request.getRequestURI(), message);

        return ResponseBuilder.badRequest(message.isBlank() ? "Validation failed" : message);
    }

    // ── Validation errors (request body fields via @Valid) ───────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {

        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[{}] {} → argument validation failed: {}",
                request.getMethod(), request.getRequestURI(), message);

        return ResponseBuilder.badRequest(message.isBlank() ? "Validation failed" : message);
    }

    // ── Missing required query/path parameters ────────────────────────────────

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {

        String message = "Required parameter '" + exception.getParameterName() + "' is missing";
        log.warn("[{}] {} → {}", request.getMethod(), request.getRequestURI(), message);

        return ResponseBuilder.badRequest(message);
    }

    // ── Unreadable / malformed request body ──────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {

        log.warn("[{}] {} → unreadable request body: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());

        return ResponseBuilder.badRequest("Malformed or unreadable request body");
    }

    // ── File too large ────────────────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception, HttpServletRequest request) {

        log.warn("[{}] {} → upload too large: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());

        return ResponseBuilder.badRequest("Uploaded file exceeds the maximum allowed size of 20 MB");
    }

    // ── Business rule / input validation ─────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException exception, HttpServletRequest request) {

        log.warn("[{}] {} → illegal argument: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());

        return ResponseBuilder.badRequest(exception.getMessage());
    }

    // ── Catch-all – unexpected server errors ──────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGenericException(
            Exception exception, HttpServletRequest request) {

        log.error("[{}] {} → unhandled exception: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);

        return ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String truncate(String text) {
        if (text == null) return "(empty)";
        return text.length() > 300 ? text.substring(0, 300) + "…" : text;
    }
}
