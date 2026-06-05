package com.farmcastai.shared.weatherai;

import com.farmcastai.config.properties.WeatherAiProperties;
import com.farmcastai.exception.WeatherAiClientException;
import com.farmcastai.exception.WeatherAiConfigurationException;
import com.farmcastai.exception.WeatherAiUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherAiApiClient {

    private final RestClient restClient;
    private final WeatherAiProperties properties;
    private final CircuitBreaker weatherAiCircuitBreaker;
    private final Retry weatherAiRetry;


    public JsonNode get(String path, Map<String, ?> queryParams) {
        log.debug("→ GET {} params={}", path, queryParams);
        return executeWithResilience(() -> doGet(path, queryParams));
    }

    public JsonNode postMultipart(String path, MultiValueMap<String, Object> parts) {
        log.debug("→ POST {} (multipart, {} part(s))", path, parts.size());
        return executeWithResilience(() -> doPostMultipart(path, parts));
    }

    public MultiValueMap<String, Object> multipartWithImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("image is required");
        }

        log.debug("Preparing multipart image: name={} size={}B type={}",
                image.getOriginalFilename(), image.getSize(), image.getContentType());

        LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        try {
            ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    String name = image.getOriginalFilename();
                    return (name == null || name.isBlank()) ? "farm-image" : name;
                }
            };

            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentType(resolveMediaType(image));
            parts.add("image", new HttpEntity<>(imageResource, imageHeaders));
            return parts;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to read uploaded image", ex);
        }
    }


    private JsonNode doGet(String path, Map<String, ?> queryParams) {
        long start = System.currentTimeMillis();
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        queryParams.forEach((key, value) -> {
                            if (value != null) {
                                uriBuilder.queryParam(key, value);
                            }
                        });
                        return uriBuilder.build();
                    })
                    .headers(this::applyAuthHeader)
                    .retrieve()
                    .body(JsonNode.class);

            log.debug("← GET {} 200 ({}ms)", path, elapsed(start));
            return response;
        } catch (RestClientResponseException ex) {
            log.warn("← GET {} {} ({}ms): {}", path, ex.getStatusCode().value(), elapsed(start), truncate(ex.getResponseBodyAsString()));
            throw toWeatherAiException(ex);
        } catch (RestClientException ex) {
            log.warn("← GET {} UNREACHABLE ({}ms): {}", path, elapsed(start), ex.getMessage());
            throw new WeatherAiUnavailableException("WeatherAI unreachable at GET " + path, ex);
        }
    }

    private JsonNode executeWithResilience(Supplier<JsonNode> httpCall) {
        Supplier<JsonNode> retriedCall = Retry.decorateSupplier(weatherAiRetry, httpCall);
        Supplier<JsonNode> guardedCall = CircuitBreaker.decorateSupplier(weatherAiCircuitBreaker, retriedCall);
        return guardedCall.get();
    }

    private JsonNode doPostMultipart(String path, MultiValueMap<String, Object> parts) {
        long start = System.currentTimeMillis();
        try {
            JsonNode response = restClient.post()
                    .uri(path)
                    .headers(this::applyAuthHeader)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(JsonNode.class);

            log.debug("← POST {} 200 ({}ms)", path, elapsed(start));
            return response;
        } catch (RestClientResponseException ex) {
            log.warn("← POST {} {} ({}ms): {}", path, ex.getStatusCode().value(), elapsed(start), truncate(ex.getResponseBodyAsString()));
            throw toWeatherAiException(ex);
        } catch (RestClientException ex) {
            log.warn("← POST {} UNREACHABLE ({}ms): {}", path, elapsed(start), ex.getMessage());
            throw new WeatherAiUnavailableException("WeatherAI unreachable at POST " + path, ex);
        }
    }


    private void applyAuthHeader(HttpHeaders headers) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new WeatherAiConfigurationException("WeatherAI API key is not configured");
        }
        headers.setBearerAuth(properties.getApiKey());
    }

    private MediaType resolveMediaType(MultipartFile image) {
        String contentType = image.getContentType();
        return (contentType == null || contentType.isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
    }

    private WeatherAiClientException toWeatherAiException(RestClientResponseException ex) {
        MediaType contentType = ex.getResponseHeaders() == null
                ? MediaType.APPLICATION_JSON
                : ex.getResponseHeaders().getContentType();
        return new WeatherAiClientException(ex.getStatusCode(), ex.getResponseBodyAsString(), contentType);
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private String truncate(String body) {
        if (body == null) return "(empty)";
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }
}
