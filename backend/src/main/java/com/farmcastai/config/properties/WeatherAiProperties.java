package com.farmcastai.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather-ai")
@Getter
@Setter
public class WeatherAiProperties {
    private String apiKey;
    private String baseUrl;
}
