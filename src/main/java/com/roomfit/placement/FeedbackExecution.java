package com.roomfit.placement;

import com.roomfit.room.Furniture;

import java.util.List;

public record FeedbackExecution(
        List<Furniture> furniture,
        FeedbackResult result,
        List<FeedbackOperationExecution> operationResults
) {
    public FeedbackExecution(List<Furniture> furniture, FeedbackResult result) {
        this(furniture, result, List.of());
    }

    public FeedbackExecution {
        furniture = List.copyOf(furniture);
        operationResults = List.copyOf(operationResults);
    }
}
