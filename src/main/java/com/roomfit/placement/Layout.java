package com.roomfit.placement;

import com.roomfit.room.Furniture;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Layout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long roomId;
    private Long contextId;
    // Room.furniture와 공유하지 않는 독립된 사본 — 추천/피드백 시점의 값 복사
    // 스냅샷이며(RuleBasedPlacementService.copyFurniture 등 참고), Room 쪽
    // 편집이 이 Layout에 자동 반영되지 않는다. 별도 @ElementCollection 테이블.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "layout_furniture", joinColumns = @JoinColumn(name = "layout_id"))
    @OrderColumn(name = "furniture_order")
    private List<Furniture> furniture;
    private boolean confirmed;
    private LocalDateTime confirmedAt;

    protected Layout() {
        // JPA용
    }

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
