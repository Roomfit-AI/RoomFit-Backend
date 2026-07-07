package com.roomfit.agent.dto;

import java.util.List;

public class AgentContextRequest {

    private Long roomId;
    private String lifestyleGoal;   // study_focused / relax_focused / storage_focused / wfh_focused
    private List<String> designStyle;
    private List<String> requiredItems;
    private List<String> optionalItems;
    private List<String> styleTags;

    protected AgentContextRequest() {
        // JSON 역직렬화용
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
}
