package com.roomfit.agent.dto.response;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.product.domain.MockProduct;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "추천에 사용할 Agent Context 응답")
public class AgentContextResponse {

    @Schema(description = "추천 Context ID", example = "1")
    private final Long contextId;
    @Schema(description = "추천 대상 방 ID", example = "1")
    private final Long roomId;
    @Schema(description = "생활 목표", example = "STUDY_FOCUSED")
    private final String lifestyleGoal;
    @Schema(description = "디자인 스타일 목록")
    private final List<String> designStyle;
    @Schema(description = "필수 가구 타입 목록")
    private final List<String> requiredItems;
    @Schema(description = "선택 가구 타입 목록")
    private final List<String> optionalItems;
    @Schema(description = "선택한 스타일 이미지 ID 목록")
    private final List<Long> selectedImageIds;
    @Schema(description = "선택한 Mock Product ID 목록")
    private final List<String> selectedProductIds;
    @Schema(description = "스타일 이미지와 선택 제품에서 병합한 추천/스타일 계산용 태그")
    private final List<String> styleTags;
    @Schema(description = "선택한 제품 상세. 추천 가구 크기와 styleTags에 사용됩니다.")
    private final List<SelectedProductResponse> selectedProducts;
    @Schema(description = "Context 생성 시각", example = "2026-07-09T02:13:15.411289")
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

    public static AgentContextResponse from(AgentContext context, List<MockProduct> selectedProducts) {
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
                selectedProducts.stream()
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
