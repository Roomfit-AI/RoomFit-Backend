package com.roomfit.agent.dto;

import com.roomfit.agent.AgentContext;

import java.time.LocalDateTime;
import java.util.List;

public class AgentContextResponse {

    private final Long contextId;
    private final Long roomId;
    private final String lifestyleGoal;
    private final List<String> designStyle;
    private final List<String> requiredItems;
    private final List<String> optionalItems;
    private final List<String> styleTags;
    private final LocalDateTime createdAt;

    private AgentContextResponse(Long contextId, Long roomId, String lifestyleGoal, List<String> designStyle,
                                  List<String> requiredItems, List<String> optionalItems, List<String> styleTags,
                                  LocalDateTime createdAt) {
        this.contextId = contextId;
        this.roomId = roomId;
        this.lifestyleGoal = lifestyleGoal;
        this.designStyle = designStyle;
        this.requiredItems = requiredItems;
        this.optionalItems = optionalItems;
        this.styleTags = styleTags;
        this.createdAt = createdAt;
    }

    public static AgentContextResponse from(AgentContext context) {
        return new AgentContextResponse(
                context.getId(),
                context.getRoomId(),
                context.getLifestyleGoal().name().toLowerCase(),
                context.getDesignStyle(),
                context.getRequiredItems(),
                context.getOptionalItems(),
                context.getStyleTags(),
                context.getCreatedAt()
        );
    }

    public Long getContextId() {
        return contextId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getLifestyleGoal() {
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
