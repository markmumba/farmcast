package com.farmcastai.modules.weather.service;

import com.farmcastai.modules.weather.dto.WeatherQueryDto;
import com.farmcastai.shared.weatherai.WeatherAiApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {
    private final WeatherAiApiClient weatherAiApiClient;

    public JsonNode getForecast(WeatherQueryDto query) {
        log.debug("getForecast lat={} lon={} days={} units={}", query.getLat(), query.getLon(), query.getDays(), query.getUnits());
        return weatherAiApiClient.get("/v1/weather", query.toQueryParams());
    }

    public JsonNode getCurrent(WeatherQueryDto query) {
        log.debug("getCurrent lat={} lon={}", query.getLat(), query.getLon());
        return weatherAiApiClient.get("/v1/current", query.toQueryParams());
    }

    public JsonNode getDaily(WeatherQueryDto query) {
        log.debug("getDaily lat={} lon={} days={}", query.getLat(), query.getLon(), query.getDays());
        return weatherAiApiClient.get("/v1/daily", query.toQueryParams());
    }

    public JsonNode getHourly(WeatherQueryDto query) {
        log.debug("getHourly lat={} lon={}", query.getLat(), query.getLon());
        return weatherAiApiClient.get("/v1/hourly", query.toQueryParams());
    }
}
