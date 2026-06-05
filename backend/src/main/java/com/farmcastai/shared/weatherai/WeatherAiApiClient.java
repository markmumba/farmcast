package com.farmcastai.shared.weatherai;

import com.farmcastai.config.properties.WeatherAiProperties;
import com.farmcastai.exception.WeatherAiClientException;
import com.farmcastai.exception.WeatherAiConfigurationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
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

    public JsonNode get(String path, Map<String, ?> queryParams) {
        log.debug("→ GET {} params={}", path, queryParams);
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
            log.debug("← GET {} 200 ({}ms)", path, System.currentTimeMillis() - start);
            return response;
        } catch (RestClientResponseException exception) {
            log.warn("← GET {} {} ({}ms): {}",
                    path,
                    exception.getStatusCode().value(),
                    System.currentTimeMillis() - start,
                    truncate(exception.getResponseBodyAsString()));
            throw toWeatherAiException(exception);
        } catch (RestClientException exception) {
            log.error("← GET {} UNREACHABLE ({}ms): {}",
                    path, System.currentTimeMillis() - start, exception.getMessage(), exception);
            throw serviceUnavailable(exception);
        }
    }

    public JsonNode postMultipart(String path, MultiValueMap<String, Object> parts) {
        log.debug("→ POST {} (multipart, {} part(s))", path, parts.size());
        long start = System.currentTimeMillis();
        try {
            JsonNode response = restClient.post()
                    .uri(path)
                    .headers(this::applyAuthHeader)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(JsonNode.class);
            log.debug("← POST {} 200 ({}ms)", path, System.currentTimeMillis() - start);
            return response;
        } catch (RestClientResponseException exception) {
            log.warn("← POST {} {} ({}ms): {}",
                    path,
                    exception.getStatusCode().value(),
                    System.currentTimeMillis() - start,
                    truncate(exception.getResponseBodyAsString()));
            throw toWeatherAiException(exception);
        } catch (RestClientException exception) {
            log.error("← POST {} UNREACHABLE ({}ms): {}",
                    path, System.currentTimeMillis() - start, exception.getMessage(), exception);
            throw serviceUnavailable(exception);
        }
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
                    String originalFilename = image.getOriginalFilename();
                    return originalFilename == null || originalFilename.isBlank() ? "farm-image" : originalFilename;
                }
            };

            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentType(resolveMediaType(image));
            parts.add("image", new HttpEntity<>(imageResource, imageHeaders));
            return parts;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to read uploaded image");
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
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return MediaType.parseMediaType(contentType);
    }

    private WeatherAiClientException toWeatherAiException(RestClientResponseException exception) {
        MediaType contentType = exception.getResponseHeaders() == null
                ? MediaType.APPLICATION_JSON
                : exception.getResponseHeaders().getContentType();

        return new WeatherAiClientException(
                exception.getStatusCode(),
                exception.getResponseBodyAsString(),
                contentType
        );
    }

    private WeatherAiClientException serviceUnavailable(RestClientException exception) {
        return new WeatherAiClientException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "{\"message\":\"WeatherAI service is currently unavailable\"}",
                MediaType.APPLICATION_JSON
        );
    }

    /** Prevents overly long error bodies from flooding the log. */
    private String truncate(String body) {
        if (body == null) return "(empty)";
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }
}
