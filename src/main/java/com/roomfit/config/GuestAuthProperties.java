package com.roomfit.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 게스트 세션 토큰 서명용 시크릿. roomfit.llm.*와 동일하게 env var
 * ROOMFIT_AUTH_GUEST_SECRET로 주입한다.
 *
 * 반드시 값이 있어야 하며(@NotBlank), 없으면 Spring이 이 설정 빈을
 * 바인딩하는 시점에 애플리케이션 기동 자체를 실패시킨다 — 예전에는
 * 미설정 시 부팅마다 랜덤 시크릿으로 조용히 대체했는데, 그러면 서버가
 * 재시작될 때마다 그 전에 발급된 모든 게스트 토큰이 무효화되어 사용자가
 * 자기 방에 다시 접근하지 못하는 문제가 생긴다. 배포 환경에서 이 값을
 * 빠뜨리는 실수를 "서버는 뜨는데 인증이 조용히 깨짐"이 아니라 "서버가
 * 아예 안 뜸"으로 즉시 드러내는 게 훨씬 안전하다.
 */
@ConfigurationProperties(prefix = "roomfit.auth.guest")
@Validated
public class GuestAuthProperties {

    @NotBlank(message = "ROOMFIT_AUTH_GUEST_SECRET(roomfit.auth.guest.secret)이 설정되지 않았습니다. "
            + "이 값 없이는 서버를 기동하지 않습니다 — 값이 없으면 재시작마다 임시 시크릿을 새로 만들게 되어, "
            + "그 순간 이전에 발급된 모든 게스트 토큰이 무효화되고 사용자가 자기 방에 접근할 수 없게 됩니다.")
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
