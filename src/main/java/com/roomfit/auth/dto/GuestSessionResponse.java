package com.roomfit.auth.dto;

import com.roomfit.auth.GuestSession;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게스트 세션 발급 응답")
public class GuestSessionResponse {

    @Schema(description = "발급된 게스트 ID. Room 등의 소유자 식별자로 서버에 저장됩니다.",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private final String guestId;
    @Schema(description = "이후 모든 /api/rooms, /api/agent, /api/layouts 요청에 " +
            "Authorization: Bearer <token> 헤더로 실어 보내야 하는 토큰",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6.9f8c1e2a...")
    private final String token;

    private GuestSessionResponse(String guestId, String token) {
        this.guestId = guestId;
        this.token = token;
    }

    public static GuestSessionResponse from(GuestSession session) {
        return new GuestSessionResponse(session.guestId(), session.token());
    }

    public String getGuestId() {
        return guestId;
    }

    public String getToken() {
        return token;
    }
}
