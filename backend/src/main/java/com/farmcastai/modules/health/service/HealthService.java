package com.farmcastai.modules.health.service;

import com.farmcastai.modules.health.dto.HealthResponseDto;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
    public HealthResponseDto check() {
        return new HealthResponseDto(
                "UP",
                "farmcast-ai-backend",
                Instant.now().toString()
        );
    }
}
