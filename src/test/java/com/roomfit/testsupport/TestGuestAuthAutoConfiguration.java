package com.roomfit.testsupport;

import com.roomfit.auth.GuestTokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * src/test/resources/META-INF/spring/org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc.imports
 * 에 등록되어 @AutoConfigureMockMvc를 쓰는 모든 테스트 클래스에 자동
 * 적용된다(개별 테스트 파일에 @Import를 추가할 필요 없음). GuestAuthInterceptor가
 * 이제 /api/rooms, /api/agent, /api/layouts 전부를 막기 때문에, 이게 없으면
 * 기존 컨트롤러 테스트 87개 mockMvc.perform 호출이 전부 401로 깨진다.
 *
 * defaultRequest로 등록한 기본 Authorization 헤더는, 개별 요청이 같은
 * 헤더를 명시적으로 지정하면 그쪽이 우선한다(Spring의
 * MockHttpServletRequestBuilder#merge가 자식 요청에 이미 있는 헤더는 부모
 * 기본값으로 덮지 않음) — 그래서 게스트 A/B 분리를 검증하는 테스트는 이
 * 기본값을 무시하고 자기가 만든 다른 게스트 토큰을 그냥 얹으면 된다.
 */
@AutoConfiguration
public class TestGuestAuthAutoConfiguration {

    @Bean
    public DefaultTestGuest defaultTestGuest(GuestTokenService guestTokenService) {
        return new DefaultTestGuest(guestTokenService);
    }

    @Bean
    public MockMvcBuilderCustomizer defaultGuestAuthMockMvcCustomizer(DefaultTestGuest defaultTestGuest) {
        return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/")
                        .header("Authorization", defaultTestGuest.bearerHeaderValue()));
    }
}
