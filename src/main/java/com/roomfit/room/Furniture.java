package com.roomfit.room;

import com.roomfit.common.VariantIdValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.List;

/**
 * 가구 도메인 모델.
 * width/depth/height: 전체 길이(full extent) 기준 — Three.js BoxGeometry 인자와 동일 기준.
 * rotation: y축 기준 회전, 단위 degree.
 */
@Embeddable
@Schema(description = "방 안의 가구. width/depth/height는 meter, position은 x-z 평면 중심 좌표입니다.")
public class Furniture {

    @Schema(description = "가구 ID", example = "desk-rec-1")
    private String id;
    @Schema(description = "가구 타입. 기존 6종 alias와 JSON Catalog의 canonical type을 지원합니다.", example = "desk")
    private String type;      // bed, desk, chair, storage 등
    @Schema(description = "화면 표시 라벨", example = "화이트 미니멀 책상")
    private String label;
    @Schema(description = "가구 가로 길이(meter)", example = "1.2")
    private double width;
    @Schema(description = "가구 깊이(meter)", example = "0.6")
    private double depth;
    @Schema(description = "가구 높이(meter)", example = "0.72")
    private double height;
    @Embedded
    @Schema(description = "x-z 평면에서의 가구 중심 좌표")
    private Position position;
    @Schema(description = "degree 단위 회전 각도", example = "0")
    private double rotation;  // degree
    @Enumerated(EnumType.STRING)
    @Schema(description = "가구 상태. DELETED는 렌더링/검증에서 제외하는 것이 자연스럽습니다.", example = "RECOMMENDED")
    private FurnitureStatus status;
    @Schema(description = "선택 제품 기반 추천인 경우 제품 ID. 기존 가구는 null 허용", example = "desk-01", nullable = true)
    private String productId;
    @Column(name = "variant_id", nullable = true)
    @Schema(description = "JSON 기반 Furniture Variant Registry 식별자. null 또는 미등록 값이면 프론트는 기존 Renderer를 사용합니다.",
            example = "desk-compact", pattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$", nullable = true)
    private String variantId;
    @Convert(converter = StringListConverter.class)
    @Column(length = 1000)
    @Schema(description = "추천/스타일 계산에 사용되는 태그. 기존 가구는 빈 배열 허용")
    private List<String> styleTags = List.of();

    protected Furniture() {
        // JSON 역직렬화용
    }

    public Furniture(String id, String type, String label, double width, double depth, double height,
                      Position position, double rotation, FurnitureStatus status) {
        this(id, type, label, width, depth, height, position, rotation, status, null, List.of(), null);
    }

    public Furniture(String id, String type, String label, double width, double depth, double height,
                      Position position, double rotation, FurnitureStatus status,
                      String productId, List<String> styleTags) {
        this(id, type, label, width, depth, height, position, rotation, status,
                productId, styleTags, null);
    }

    public Furniture(String id, String type, String label, double width, double depth, double height,
                     Position position, double rotation, FurnitureStatus status,
                     String productId, List<String> styleTags, String variantId) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.position = position;
        this.rotation = rotation;
        this.status = status;
        this.productId = productId;
        this.styleTags = styleTags == null ? List.of() : List.copyOf(styleTags);
        this.variantId = VariantIdValidator.validateNullable(variantId);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public double getWidth() {
        return width;
    }

    public double getDepth() {
        return depth;
    }

    public double getHeight() {
        return height;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public FurnitureStatus getStatus() {
        return status;
    }

    public void setStatus(FurnitureStatus status) {
        this.status = status;
    }

    public String getProductId() {
        return productId;
    }

    public String getVariantId() {
        return variantId;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }
}
