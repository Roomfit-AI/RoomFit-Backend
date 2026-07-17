package com.roomfit.auth;

import com.roomfit.auth.dto.GuestSessionResponse;
import com.roomfit.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원가입 없이 앱 첫 실행 시 발급하는 게스트 세션 API")
public class AuthController {

    private final GuestTokenService guestTokenService;

    public AuthController(GuestTokenService guestTokenService) {
        this.guestTokenService = guestTokenService;
    }

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "게스트 세션 발급", description = "앱 첫 실행 시 호출합니다. 별도 회원가입 UI 없이 서버가 추측하기 어려운 토큰을 발급하며, " +
            "이후 /api/rooms, /api/agent, /api/layouts 요청은 모두 이 토큰을 Authorization: Bearer 헤더로 실어 보내야 합니다. " +
            "토큰을 잃어버리면 이 API를 다시 호출해 새 게스트로 시작하면 되지만, 이전 게스트가 소유한 방은 더 이상 접근할 수 없습니다.")
    @ApiResponse(responseCode = "201", description = "게스트 세션 발급 성공")
    public CommonResponse<GuestSessionResponse> issueGuestSession() {
        return CommonResponse.ok(GuestSessionResponse.from(guestTokenService.issue()));
    }
}
