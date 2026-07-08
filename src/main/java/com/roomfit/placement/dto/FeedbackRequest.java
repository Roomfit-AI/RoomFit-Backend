package com.roomfit.placement.dto;

public class FeedbackRequest {

    private Long layoutId;
    private String feedback;

    protected FeedbackRequest() {
        // JSON 역직렬화용
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public String getFeedback() {
        return feedback;
    }
}
