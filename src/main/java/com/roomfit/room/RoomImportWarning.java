package com.roomfit.room;

import jakarta.persistence.Embeddable;

@Embeddable
public class RoomImportWarning {

    private String code;
    private String entityId;
    private String furnitureType;
    private String message;
    private Double adjustmentMeters;
    private Double originalX;
    private Double originalZ;
    private Double normalizedX;
    private Double normalizedZ;
    private Double originalRotation;
    private Double normalizedRotation;

    protected RoomImportWarning() {
    }

    public RoomImportWarning(String code, String entityId, String furnitureType, String message, Double adjustmentMeters,
                             Position original, Position normalized, Double originalRotation, Double normalizedRotation) {
        this.code = code;
        this.entityId = entityId;
        this.furnitureType = furnitureType;
        this.message = message;
        this.adjustmentMeters = adjustmentMeters;
        this.originalX = original == null ? null : original.getX();
        this.originalZ = original == null ? null : original.getZ();
        this.normalizedX = normalized == null ? null : normalized.getX();
        this.normalizedZ = normalized == null ? null : normalized.getZ();
        this.originalRotation = originalRotation;
        this.normalizedRotation = normalizedRotation;
    }

    public String getCode() { return code; }
    public String getEntityId() { return entityId; }
    public String getFurnitureType() { return furnitureType; }
    public String getMessage() { return message; }
    public Double getAdjustmentMeters() { return adjustmentMeters; }
    public Double getOriginalX() { return originalX; }
    public Double getOriginalZ() { return originalZ; }
    public Double getNormalizedX() { return normalizedX; }
    public Double getNormalizedZ() { return normalizedZ; }
    public Double getOriginalRotation() { return originalRotation; }
    public Double getNormalizedRotation() { return normalizedRotation; }
}
