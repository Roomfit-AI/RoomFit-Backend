package com.roomfit.placement.dto;

import com.roomfit.placement.Layout;

import java.time.LocalDateTime;

public class ConfirmResponse {

    private final Long layoutId;
    private final boolean confirmed;
    private final LocalDateTime confirmedAt;

    private ConfirmResponse(Long layoutId, boolean confirmed, LocalDateTime confirmedAt) {
        this.layoutId = layoutId;
        this.confirmed = confirmed;
        this.confirmedAt = confirmedAt;
    }

    public static ConfirmResponse from(Layout layout) {
        return new ConfirmResponse(layout.getId(), layout.isConfirmed(), layout.getConfirmedAt());
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
}
