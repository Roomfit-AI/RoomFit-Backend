package com.roomfit.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "배치 추천 생성 요청")
public class RecommendRequest {

    @Schema(description = "추천에 사용할 Agent Context ID", example = "1")
    private Long contextId;

    protected RecommendRequest() {
        // JSON 역직렬화용
    }

    public Long getContextId() {
        return contextId;
    }
}
