package com.roomfit.auth;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * GuestAuthInterceptor가 요청마다 한 번 채워 넣는, 현재 요청을 보낸
 * 게스트의 id. 요청 스코프 빈이라 서비스 계층(RoomAccessService 등)이
 * 스레드 안전하게 생성자 주입으로 참조할 수 있다 — ThreadLocal을 직접
 * 다룰 필요가 없다.
 */
@Component
@RequestScope
public class CurrentGuestContext {

    private String guestId;

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getGuestId() {
        if (guestId == null) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }
        return guestId;
    }
}
