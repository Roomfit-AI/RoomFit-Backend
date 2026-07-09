package com.roomfit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 배포 프론트 URL이 정해지면 allowedOrigins에 추가하세요.
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 현재 인증/쿠키 기반 기능이 없으므로 credentials는 허용하지 않습니다.
                // 나중에 인증이 추가되면 allowCredentials 설정을 별도 검토하세요.
                .allowCredentials(false)
                .maxAge(3600);
    }
}
