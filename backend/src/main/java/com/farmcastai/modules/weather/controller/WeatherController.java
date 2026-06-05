package com.farmcastai.modules.weather.controller;

import com.farmcastai.modules.weather.dto.WeatherQueryDto;
import com.farmcastai.modules.weather.service.WeatherService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {
    private final WeatherService weatherService;

    @GetMapping
    public JsonNode getForecast(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
            @RequestParam(defaultValue = "7") @Min(1) @Max(16) int days,
            @RequestParam(defaultValue = "true") boolean ai,
            @RequestParam(defaultValue = "metric") @Pattern(regexp = "metric|imperial") String units,
            @RequestParam(defaultValue = "en") @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String lang
    ) {
        return weatherService.getForecast(new WeatherQueryDto(lat, lon, days, ai, units, lang));
    }

    @GetMapping("/current")
    public JsonNode getCurrent(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
            @RequestParam(defaultValue = "1") @Min(1) @Max(16) int days,
            @RequestParam(defaultValue = "true") boolean ai,
            @RequestParam(defaultValue = "metric") @Pattern(regexp = "metric|imperial") String units,
            @RequestParam(defaultValue = "en") @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String lang
    ) {
        return weatherService.getCurrent(new WeatherQueryDto(lat, lon, days, ai, units, lang));
    }

    @GetMapping("/daily")
    public JsonNode getDaily(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
            @RequestParam(defaultValue = "7") @Min(1) @Max(16) int days,
            @RequestParam(defaultValue = "true") boolean ai,
            @RequestParam(defaultValue = "metric") @Pattern(regexp = "metric|imperial") String units,
            @RequestParam(defaultValue = "en") @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String lang
    ) {
        return weatherService.getDaily(new WeatherQueryDto(lat, lon, days, ai, units, lang));
    }

    @GetMapping("/hourly")
    public JsonNode getHourly(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
            @RequestParam(defaultValue = "7") @Min(1) @Max(16) int days,
            @RequestParam(defaultValue = "true") boolean ai,
            @RequestParam(defaultValue = "metric") @Pattern(regexp = "metric|imperial") String units,
            @RequestParam(defaultValue = "en") @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String lang
    ) {
        return weatherService.getHourly(new WeatherQueryDto(lat, lon, days, ai, units, lang));
    }
}
