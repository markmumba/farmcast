package com.farmcastai.modules.health.controller;

import com.farmcastai.modules.health.dto.HealthResponseDto;
import com.farmcastai.modules.health.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    @GetMapping
    HealthResponseDto health() {
        return healthService.check();
    }
}
