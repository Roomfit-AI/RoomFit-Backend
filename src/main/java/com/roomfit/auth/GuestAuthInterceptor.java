package com.roomfit.auth;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Authorization: Bearer <token> 헤더를 검증해 CurrentGuestContext를 채운다.
 * WebConfig가 /api/rooms/**, /api/agent/**, /api/layouts/**에만 등록한다 —
 * 토큰 발급 자체(POST /api/auth/guest)와 상품/스타일 카탈로그, actuator,
 * swagger 경로는 보호 대상이 아니다. 예외를 던지면 GlobalExceptionHandler가
 * 그대로 401 JSON 응답으로 변환한다(인터셉터에서 던진 예외도 Spring MVC의
 * 표준 예외 리졸버 체인을 거치므로 컨트롤러 예외와 동일하게 처리됨).
 */
@Component
public class GuestAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GuestTokenService guestTokenService;
    private final CurrentGuestContext currentGuestContext;

    public GuestAuthInterceptor(GuestTokenService guestTokenService, CurrentGuestContext currentGuestContext) {
        this.guestTokenService = guestTokenService;
        this.currentGuestContext = currentGuestContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        String guestId = guestTokenService.verifyAndExtractGuestId(token);
        currentGuestContext.setGuestId(guestId);
        return true;
    }
}
