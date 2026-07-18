package com.roomfit.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@RestController
public class HealthController {

    private final String version;

    public HealthController(@Value("${ROOMFIT_BUILD_VERSION:unknown}") String version) {
        this.version = version;
    }

    @GetMapping("/")
    public String root() {
        return "RoomFit Backend is running";
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "roomfit-backend",
                "version", version
        );
    }
}
