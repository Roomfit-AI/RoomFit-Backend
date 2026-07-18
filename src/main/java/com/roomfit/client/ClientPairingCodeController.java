package com.roomfit.client;

import com.roomfit.client.dto.PairingCodeRedeemRequest;
import com.roomfit.client.dto.PairingCodeRedeemResponse;
import com.roomfit.client.dto.PairingCodeResponse;
import com.roomfit.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients/pairing-code")
@Tag(name = "Client Pairing", description = "회원가입 없이 앱의 익명 clientId를 다른 브라우저로 옮기기 위한 영구 페어링 코드 API")
public class ClientPairingCodeController {

    private final ClientPairingCodeService pairingCodeService;

    public ClientPairingCodeController(ClientPairingCodeService pairingCodeService) {
        this.pairingCodeService = pairingCodeService;
    }

    @PostMapping
    @Operation(summary = "내 페어링 코드 조회/발급",
            description = "요청 헤더의 X-RoomFit-Client-Id에 대해 이미 발급된 코드가 있으면 그대로, 없으면 새로 발급해 반환합니다(멱등). "
                    + "만료되지 않으며, 재발급하려면 /regenerate를 사용하세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코드 조회/발급 성공"),
            @ApiResponse(responseCode = "400", description = "X-RoomFit-Client-Id 헤더가 없음")
    })
    public CommonResponse<PairingCodeResponse> issueOrGetCode() {
        return CommonResponse.ok(new PairingCodeResponse(pairingCodeService.issueOrGetCode()));
    }

    @PostMapping("/regenerate")
    @Operation(summary = "페어링 코드 재발급",
            description = "기존 코드를 무효화하고 새 코드를 발급합니다. 코드가 노출됐다고 판단될 때 사용하세요 — 재발급 즉시 예전 코드로는 redeem이 되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "400", description = "X-RoomFit-Client-Id 헤더가 없음")
    })
    public CommonResponse<PairingCodeResponse> regenerateCode() {
        return CommonResponse.ok(new PairingCodeResponse(pairingCodeService.regenerateCode()));
    }

    @PostMapping("/redeem")
    @Operation(summary = "페어링 코드로 clientId 조회",
            description = "다른 기기(앱)에서 발급받은 코드를 입력해 그 clientId를 알아냅니다. "
                    + "이 호출 자체는 X-RoomFit-Client-Id 헤더가 필요 없습니다 — 아직 신원을 모르는 브라우저가 자기 clientId를 알아내려고 부르는 호출이라서입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코드")
    })
    public CommonResponse<PairingCodeRedeemResponse> redeem(@RequestBody PairingCodeRedeemRequest request) {
        return CommonResponse.ok(new PairingCodeRedeemResponse(pairingCodeService.redeem(request.getCode())));
    }
}
