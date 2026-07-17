package com.roomfit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * 게스트 세션 토큰 서명용 시크릿. roomfit.llm.*와 동일하게 env var
 * ROOMFIT_AUTH_GUEST_SECRET로 주입한다. 미설정 시 부팅마다 랜덤 시크릿을
 * 생성해 서버가 항상 기동은 되게 하되(로컬/데모 편의), 재시작 시 기존에
 * 발급된 모든 토큰이 무효화된다는 걸 눈에 띄게 경고한다 — 실제 배포에서는
 * 반드시 이 값을 고정해서 넣어야 한다.
 */
@ConfigurationProperties(prefix = "roomfit.auth.guest")
public class GuestAuthProperties {

    private static final Logger log = LoggerFactory.getLogger(GuestAuthProperties.class);

    private String secret = "";

    public synchronized String getSecret() {
        if (secret == null || secret.isBlank()) {
            log.warn("ROOMFIT_AUTH_GUEST_SECRET이 설정되지 않아 임시 랜덤 시크릿을 사용합니다. "
                    + "서버가 재시작되면 이전에 발급된 모든 게스트 토큰이 무효화됩니다. "
                    + "배포 환경에서는 반드시 고정된 값을 설정하세요.");
            secret = UUID.randomUUID().toString();
        }
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
