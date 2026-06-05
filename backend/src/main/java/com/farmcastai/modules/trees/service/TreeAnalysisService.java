package com.farmcastai.modules.trees.service;

import com.farmcastai.modules.trees.dto.TreeAnalysisRequestDto;
import com.farmcastai.shared.weatherai.WeatherAiApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeAnalysisService {
    private final WeatherAiApiClient weatherAiApiClient;

    public JsonNode analyze(MultipartFile image, TreeAnalysisRequestDto request) {
        log.debug("analyze image={} size={}B county={} farmerId={}",
                image != null ? image.getOriginalFilename() : null,
                image != null ? image.getSize() : 0,
                request.getCounty(),
                request.getFarmerId());

        validateImage(image);

        MultiValueMap<String, Object> parts = weatherAiApiClient.multipartWithImage(image);
        request.toFormFields().forEach((key, value) -> {
            if (value != null && !value.toString().isBlank()) {
                parts.add(key, value.toString());
            }
        });

        return weatherAiApiClient.postMultipart("/v1/trees/analyze", parts);
    }

    public JsonNode getHistory(int limit, String cursor) {
        log.debug("getHistory limit={} cursor={}", limit, cursor);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", limit);
        params.put("cursor", cursor);
        return weatherAiApiClient.get("/v1/trees/history", params);
    }

    public JsonNode getQuota() {
        log.debug("getQuota");
        return weatherAiApiClient.get("/v1/trees/quota", Map.of());
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("image is required");
        }

        String contentType = image.getContentType();
        if (contentType == null || !isSupportedImage(contentType)) {
            throw new IllegalArgumentException("image must be a JPEG, PNG, or WEBP file");
        }
    }

    private boolean isSupportedImage(String contentType) {
        return contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/png")
                || contentType.equalsIgnoreCase("image/webp");
    }
}
