package com.roomfit.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "발급/재발급된 페어링 코드")
public class PairingCodeResponse {

    @Schema(description = "다른 브라우저에 입력할 영구 페어링 코드", example = "K7X9QP42")
    private final String code;

    public PairingCodeResponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
