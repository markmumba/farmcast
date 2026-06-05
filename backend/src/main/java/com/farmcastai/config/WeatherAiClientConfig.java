package com.farmcastai.config;

import com.farmcastai.config.properties.ResilienceProperties;
import com.farmcastai.config.properties.WeatherAiProperties;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
public class WeatherAiClientConfig {

    @Bean
    RestClient weatherAiRestClient(RestClient.Builder builder,
                                   WeatherAiProperties properties,
                                   ResilienceProperties resilience) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(resilience.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(resilience.getReadTimeoutSeconds()));

        log.info("WeatherAI RestClient configured: baseUrl={} connectTimeout={}s readTimeout={}s",
                properties.getBaseUrl(),
                resilience.getConnectTimeoutSeconds(),
                resilience.getReadTimeoutSeconds());

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }
}
