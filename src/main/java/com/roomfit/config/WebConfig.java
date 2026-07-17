package com.roomfit.config;

import com.roomfit.auth.GuestAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final GuestAuthInterceptor guestAuthInterceptor;

    public WebConfig(GuestAuthInterceptor guestAuthInterceptor) {
        this.guestAuthInterceptor = guestAuthInterceptor;
    }

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "http://127.0.0.1:5175"
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 게스트 토큰 발급(POST /api/auth/guest) 자체와 product/style 카탈로그,
        // actuator, swagger 경로는 보호 대상이 아니다 — 요구사항에 명시된
        // Room/Agent Context/Layout 관련 API에만 인증을 요구한다.
        registry.addInterceptor(guestAuthInterceptor)
                .addPathPatterns("/api/rooms/**", "/api/agent/**", "/api/layouts/**");
    }
}
