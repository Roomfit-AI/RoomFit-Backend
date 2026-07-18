package com.roomfit.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페어링 코드로 알아낸 clientId. 이 값을 이 브라우저의 X-RoomFit-Client-Id로 계속 사용하면 됩니다.")
public class PairingCodeRedeemResponse {

    @Schema(description = "코드를 발급한 원래 기기의 clientId(UUID)")
    private final String clientId;

    public PairingCodeRedeemResponse(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
