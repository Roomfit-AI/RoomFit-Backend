package com.roomfit.placement;

import com.roomfit.room.Furniture;

import java.util.List;

public record FeedbackExecution(List<Furniture> furniture, FeedbackResult result) {
}
