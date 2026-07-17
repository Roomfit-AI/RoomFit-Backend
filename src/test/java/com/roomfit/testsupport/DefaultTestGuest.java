package com.roomfit.testsupport;

import com.roomfit.auth.GuestSession;
import com.roomfit.auth.GuestTokenService;

/**
 * 이 Spring 컨텍스트의 모든 MockMvc 요청에 기본으로 실리는 게스트 —
 * TestGuestAuthAutoConfiguration이 컨텍스트 시작 시 한 번 발급한다.
 * GuestTokenService.issue()는 항상 랜덤 guestId를 만들기 때문에(테스트 전용
 * 백도어 메서드를 프로덕션 클래스에 추가하지 않기 위해 일부러 그렇게 둠),
 * 리포지토리로 Room을 직접 만드는 테스트가 그 room의 ownerId를 이 값과
 * 맞추려면 이 빈을 주입받아 guestId()를 읽어야 한다.
 */
public class DefaultTestGuest {

    private final GuestSession session;

    public DefaultTestGuest(GuestTokenService guestTokenService) {
        this.session = guestTokenService.issue();
    }

    public String guestId() {
        return session.guestId();
    }

    public String token() {
        return session.token();
    }

    public String bearerHeaderValue() {
        return "Bearer " + session.token();
    }
}
