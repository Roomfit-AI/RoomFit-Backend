package com.roomfit.placement;

import com.roomfit.room.Furniture;

import java.time.LocalDateTime;
import java.util.List;

public class Layout {

    private Long id;
    private final Long roomId;
    private final Long contextId;
    private List<Furniture> furniture;
    private boolean confirmed;
    private LocalDateTime confirmedAt;

    public Layout(Long roomId, Long contextId, List<Furniture> furniture) {
        this.roomId = roomId;
        this.contextId = contextId;
        this.furniture = furniture;
        this.confirmed = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getContextId() {
        return contextId;
    }

    public List<Furniture> getFurniture() {
        return furniture;
    }

    public void setFurniture(List<Furniture> furniture) {
        this.furniture = furniture;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void confirm() {
        this.confirmed = true;
        this.confirmedAt = LocalDateTime.now();
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
}
