package com.roomfit.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페어링 코드로 clientId를 조회하는 요청")
public class PairingCodeRedeemRequest {

    @Schema(description = "다른 기기(앱)에서 발급받은 페어링 코드. 대시/공백/대소문자는 무시합니다.", example = "K7X9-QP42")
    private String code;

    protected PairingCodeRedeemRequest() {
        // JSON 역직렬화용
    }

    public String getCode() {
        return code;
    }
}
