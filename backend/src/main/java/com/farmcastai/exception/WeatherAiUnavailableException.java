package com.farmcastai.exception;

/**
 * Thrown when the WeatherAI service cannot be reached at all (network/connectivity failure).
 *
 * <p>Distinct from {@link WeatherAiClientException}, which represents an HTTP error response
 * received from the WeatherAI API (4xx / 5xx). This exception triggers retry logic and is
 * recorded by the circuit breaker as a service failure.
 */
public class WeatherAiUnavailableException extends RuntimeException {

    public WeatherAiUnavailableException(String message) {
        super(message);
    }

    public WeatherAiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
