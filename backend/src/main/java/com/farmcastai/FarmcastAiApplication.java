package com.farmcastai;

import com.farmcastai.config.properties.ApplicationProperties;
import com.farmcastai.config.properties.ResilienceProperties;
import com.farmcastai.config.properties.WeatherAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class, WeatherAiProperties.class, ResilienceProperties.class})
public class FarmcastAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmcastAiApplication.class, args);
    }
}
