package com.roomfit.agent.domain;

import com.roomfit.product.domain.MockProduct;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Room 정보 + 사용자 입력(생활 목적/취향/선택 가구/이미지 태그)을 통합한 추천 조건 컨텍스트.
 */
public class AgentContext {

    private Long id;
    private Long roomId;
    private LifestyleGoal lifestyleGoal;
    private List<DesignStyle> designStyle;
    private List<String> requiredItems;
    private List<String> optionalItems;
    private List<Long> selectedImageIds;
    private List<String> selectedProductIds;
    private List<String> styleTags;
    private List<MockProduct> selectedProducts;
    private LocalDateTime createdAt;

    public AgentContext(Long roomId, LifestyleGoal lifestyleGoal, List<DesignStyle> designStyle,
                         List<String> requiredItems, List<String> optionalItems,
                         List<Long> selectedImageIds, List<String> selectedProductIds,
                         List<String> styleTags, List<MockProduct> selectedProducts) {
        this.roomId = roomId;
        this.lifestyleGoal = lifestyleGoal;
        this.designStyle = List.copyOf(designStyle);
        this.requiredItems = List.copyOf(requiredItems);
        this.optionalItems = List.copyOf(optionalItems);
        this.selectedImageIds = List.copyOf(selectedImageIds);
        this.selectedProductIds = List.copyOf(selectedProductIds);
        this.styleTags = List.copyOf(styleTags);
        this.selectedProducts = new ArrayList<>(selectedProducts);
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

    public List<DesignStyle> getDesignStyle() {
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

    public List<MockProduct> getSelectedProducts() {
        return selectedProducts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
