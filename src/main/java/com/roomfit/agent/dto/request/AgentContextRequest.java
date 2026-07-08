package com.roomfit.agent.dto.request;

import java.util.List;

public class AgentContextRequest {

    private Long roomId;
    private String lifestyleGoal;
    private List<String> designStyle;
    private List<String> requiredItems;
    private List<String> optionalItems;
    private List<Long> selectedImageIds;
    private List<String> selectedProductIds;

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

    public List<Long> getSelectedImageIds() {
        return selectedImageIds;
    }

    public List<String> getSelectedProductIds() {
        return selectedProductIds;
    }
}
