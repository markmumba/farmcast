package com.farmcastai.config;

import com.farmcastai.config.properties.WeatherAiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WeatherAiClientConfig {

    @Bean
    RestClient weatherAiRestClient(RestClient.Builder builder, WeatherAiProperties properties) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
