package com.roomfit.placement;

public record FeedbackOperationExecution(
        String operationId,
        FeedbackOperationType type,
        Status status,
        String reasonCode,
        String affectedFurnitureId
) {
    public enum Status {
        APPLIED,
        FAILED,
        SKIPPED
    }

    static FeedbackOperationExecution applied(FeedbackOperation operation, String furnitureId) {
        return new FeedbackOperationExecution(operation.operationId(), operation.type(), Status.APPLIED, null, furnitureId);
    }

    static FeedbackOperationExecution failed(FeedbackOperation operation, String reasonCode) {
        return new FeedbackOperationExecution(operation.operationId(), operation.type(), Status.FAILED, reasonCode, null);
    }

    static FeedbackOperationExecution skipped(FeedbackOperation operation, String reasonCode) {
        return new FeedbackOperationExecution(operation.operationId(), operation.type(), Status.SKIPPED, reasonCode, null);
    }
}
