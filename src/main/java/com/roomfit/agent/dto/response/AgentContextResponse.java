package com.roomfit.agent.dto.response;

import com.roomfit.agent.domain.AgentContext;

import java.time.LocalDateTime;
import java.util.List;

public class AgentContextResponse {

    private final Long contextId;
    private final Long roomId;
    private final String lifestyleGoal;
    private final List<String> designStyle;
    private final List<String> requiredItems;
    private final List<String> optionalItems;
    private final List<Long> selectedImageIds;
    private final List<String> selectedProductIds;
    private final List<String> styleTags;
    private final List<SelectedProductResponse> selectedProducts;
    private final LocalDateTime createdAt;

    private AgentContextResponse(Long contextId, Long roomId, String lifestyleGoal, List<String> designStyle,
                                  List<String> requiredItems, List<String> optionalItems,
                                  List<Long> selectedImageIds, List<String> selectedProductIds,
                                  List<String> styleTags, List<SelectedProductResponse> selectedProducts,
                                  LocalDateTime createdAt) {
        this.contextId = contextId;
        this.roomId = roomId;
        this.lifestyleGoal = lifestyleGoal;
        this.designStyle = designStyle;
        this.requiredItems = requiredItems;
        this.optionalItems = optionalItems;
        this.selectedImageIds = selectedImageIds;
        this.selectedProductIds = selectedProductIds;
        this.styleTags = styleTags;
        this.selectedProducts = selectedProducts;
        this.createdAt = createdAt;
    }

    public static AgentContextResponse from(AgentContext context) {
        return new AgentContextResponse(
                context.getId(),
                context.getRoomId(),
                context.getLifestyleGoal().name(),
                context.getDesignStyle().stream().map(Enum::name).toList(),
                context.getRequiredItems(),
                context.getOptionalItems(),
                context.getSelectedImageIds(),
                context.getSelectedProductIds(),
                context.getStyleTags(),
                context.getSelectedProducts().stream()
                        .map(SelectedProductResponse::from)
                        .toList(),
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

    public List<Long> getSelectedImageIds() {
        return selectedImageIds;
    }

    public List<String> getSelectedProductIds() {
        return selectedProductIds;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public List<SelectedProductResponse> getSelectedProducts() {
        return selectedProducts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
