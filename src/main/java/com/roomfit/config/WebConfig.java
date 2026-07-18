package com.roomfit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    static final String DEFAULT_ALLOWED_ORIGINS =
            "http://localhost:5173,http://localhost:5174,http://localhost:5175";

    private final String[] allowedOrigins;

    public WebConfig(@Value("${CORS_ALLOWED_ORIGINS:" + DEFAULT_ALLOWED_ORIGINS + "}") String allowedOrigins) {
        this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
    }

    static String[] parseAllowedOrigins(String configuredOrigins) {
        String origins = configuredOrigins == null || configuredOrigins.isBlank()
                ? DEFAULT_ALLOWED_ORIGINS
                : configuredOrigins;

        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
