package com.roomfit.agent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Room 정보 + 사용자 입력(생활 목적/취향/선택 가구/이미지 태그)을 통합한 추천 조건 컨텍스트.
 */
public class AgentContext {

    private Long id;
    private Long roomId;
    private LifestyleGoal lifestyleGoal;
    private List<String> designStyle;
    private List<String> requiredItems;
    private List<String> optionalItems;
    private List<String> styleTags;
    private LocalDateTime createdAt;

    public AgentContext(Long roomId, LifestyleGoal lifestyleGoal, List<String> designStyle,
                         List<String> requiredItems, List<String> optionalItems, List<String> styleTags) {
        this.roomId = roomId;
        this.lifestyleGoal = lifestyleGoal;
        this.designStyle = designStyle;
        this.requiredItems = requiredItems;
        this.optionalItems = optionalItems;
        this.styleTags = styleTags;
        this.createdAt = LocalDateTime.now();
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

    public LifestyleGoal getLifestyleGoal() {
        return lifestyleGoal;
    }

    public List<String> getDesignStyle() {
        return designStyle;
    }

    public List<String> getRequiredItems() {
        return requiredItems;
    }

    public List<String> getOptionalItems() {
        return optionalItems;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
