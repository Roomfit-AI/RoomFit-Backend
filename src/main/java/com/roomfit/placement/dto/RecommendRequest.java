package com.roomfit.placement.dto;

public class RecommendRequest {

    private Long contextId;

    protected RecommendRequest() {
        // JSON 역직렬화용
    }

    public Long getContextId() {
        return contextId;
    }
}
