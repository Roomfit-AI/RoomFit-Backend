package com.roomfit.auth;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.config.GuestAuthProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * 서버가 추측하기 어려운 게스트 토큰을 발급/검증한다. 외부 JWT 라이브러리
 * 없이 JDK의 HmacSHA256만으로 "<guestId>.<서명>" 형태를 직접 구현 — 이
 * 프로젝트가 회원가입/비밀번호 없이 최소한의 익명 세션만 필요로 하는
 * 범위라, 풀 JWT 스펙(헤더/만료/발급자 등)을 끌어올 필요는 없다고 판단.
 * guestId 자체는 UUID라 그 자체로도 추측이 사실상 불가능하고, 서명은
 * 클라이언트가 임의의 guestId를 골라 위조하는 것을 막는다.
 */
@Service
public class GuestTokenService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String DELIMITER = ".";

    private final GuestAuthProperties properties;

    public GuestTokenService(GuestAuthProperties properties) {
        this.properties = properties;
    }

    public GuestSession issue() {
        String guestId = UUID.randomUUID().toString();
        String token = guestId + DELIMITER + sign(guestId);
        return new GuestSession(guestId, token);
    }

    public String verifyAndExtractGuestId(String token) {
        if (token == null || token.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }

        int lastDot = token.lastIndexOf(DELIMITER);
        if (lastDot <= 0 || lastDot == token.length() - 1) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }

        String guestId = token.substring(0, lastDot);
        String providedSignature = token.substring(lastDot + 1);
        String expectedSignature = sign(guestId);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }

        return guestId;
    }

    private String sign(String guestId) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(guestId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("게스트 토큰 서명에 실패했습니다.", e);
        }
    }
}
