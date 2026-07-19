package com.roomfit.room;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Production's room_import_warnings table already has originalx/originalz/
 * normalizedx/normalizedz (no underscore) — created before these four fields
 * had explicit @Column names, back when whatever naming strategy was active
 * didn't split the single trailing X/Z the way it splits every other
 * multi-word column here (adjustment_meters, entity_id, etc., which do have
 * underscores and match Hibernate's current default fine). ddl-auto=update
 * only adds missing columns, it never renames existing ones, so every query
 * against this table has been failing with "column ... does not exist" since
 * whatever change made Hibernate start expecting normalized_x — 136 existing
 * rows in production are affected. Pinning these four to the columns that
 * actually exist, instead of migrating the live table, is the reversible,
 * data-preserving fix.
 */
@Embeddable
public class RoomImportWarning {

    private String code;
    private String entityId;
    private String furnitureType;
    private String message;
    private Double adjustmentMeters;
    @Column(name = "originalx")
    private Double originalX;
    @Column(name = "originalz")
    private Double originalZ;
    @Column(name = "normalizedx")
    private Double normalizedX;
    @Column(name = "normalizedz")
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
