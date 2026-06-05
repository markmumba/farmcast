package com.farmcastai.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

public class WeatherAiClientException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final String responseBody;
    private final MediaType contentType;

    public WeatherAiClientException(HttpStatusCode statusCode, String responseBody, MediaType contentType) {
        super("WeatherAI request failed with status " + statusCode.value());
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.contentType = contentType;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public MediaType getContentType() {
        return contentType;
    }
}
