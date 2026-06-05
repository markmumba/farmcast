package com.farmcastai.modules.account.service;

import com.farmcastai.shared.weatherai.WeatherAiApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final WeatherAiApiClient weatherAiApiClient;

    public JsonNode getUsage() {
        return weatherAiApiClient.get("/v1/usage", Map.of());
    }
}
