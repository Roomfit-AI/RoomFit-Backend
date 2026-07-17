package com.roomfit.agent.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Room 정보 + 사용자 입력(생활 목적/취향/선택 가구/이미지 태그)을 통합한 추천 조건 컨텍스트.
 * 선택된 MockProduct 전체 객체는 저장하지 않고 selectedProductIds만 영속화한다 —
 * 필요한 곳(RuleBasedPlacementService 등)에서 MockProductService로 그때그때 조회한다.
 */
@Entity
public class AgentContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long roomId;
    @Enumerated(EnumType.STRING)
    private LifestyleGoal lifestyleGoal;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_context_design_style", joinColumns = @JoinColumn(name = "context_id"))
    @Enumerated(EnumType.STRING)
    private List<DesignStyle> designStyle;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_context_required_items", joinColumns = @JoinColumn(name = "context_id"))
    private List<String> requiredItems;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_context_optional_items", joinColumns = @JoinColumn(name = "context_id"))
    private List<String> optionalItems;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_context_selected_image_ids", joinColumns = @JoinColumn(name = "context_id"))
    private List<Long> selectedImageIds;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_context_selected_product_ids", joinColumns = @JoinColumn(name = "context_id"))
    private List<String> selectedProductIds;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_context_style_tags", joinColumns = @JoinColumn(name = "context_id"))
    private List<String> styleTags;
    @Enumerated(EnumType.STRING)
    private PreferredColorTone preferredColorTone;
    private LocalDateTime createdAt;

    protected AgentContext() {
        // JPA용
    }

    public AgentContext(Long roomId, LifestyleGoal lifestyleGoal, List<DesignStyle> designStyle,
                         List<String> requiredItems, List<String> optionalItems,
                         List<Long> selectedImageIds, List<String> selectedProductIds,
                         List<String> styleTags) {
        this(roomId, lifestyleGoal, designStyle, requiredItems, optionalItems,
                selectedImageIds, selectedProductIds, styleTags, null);
    }

    public AgentContext(Long roomId, LifestyleGoal lifestyleGoal, List<DesignStyle> designStyle,
                         List<String> requiredItems, List<String> optionalItems,
                         List<Long> selectedImageIds, List<String> selectedProductIds,
                         List<String> styleTags, PreferredColorTone preferredColorTone) {
        // Hibernate가 @ElementCollection 필드를 직접 관리하려면 가변 List여야
        // 한다 — List.copyOf()로 만든 불변 리스트를 넣으면 저장 시
        // UnsupportedOperationException이 난다.
        this.roomId = roomId;
        this.lifestyleGoal = lifestyleGoal;
        this.designStyle = new ArrayList<>(designStyle);
        this.requiredItems = new ArrayList<>(requiredItems);
        this.optionalItems = new ArrayList<>(optionalItems);
        this.selectedImageIds = new ArrayList<>(selectedImageIds);
        this.selectedProductIds = new ArrayList<>(selectedProductIds);
        this.styleTags = new ArrayList<>(styleTags);
        this.preferredColorTone = preferredColorTone;
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

    public PreferredColorTone getPreferredColorTone() {
        return preferredColorTone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
