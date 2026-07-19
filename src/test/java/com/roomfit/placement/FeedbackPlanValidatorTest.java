package com.roomfit.placement;

import com.roomfit.common.CustomException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackPlanValidatorTest {

    private final FeedbackPlanValidator validator = new FeedbackPlanValidator();

    @Test
    void defaultFeedbackLimitRemainsFourOperations() {
        FeedbackPlan fiveOperations = plan(5);

        assertThatThrownBy(() -> validator.validate(fiveOperations))
                .isInstanceOf(CustomException.class);
        assertThatCode(() -> validator.validate(fiveOperations, FurnitureAdditionPolicy.MAX_NEW_ADDITIONS))
                .doesNotThrowAnyException();
    }

    @Test
    void additionsSpecificLimitAcceptsEightButNotNine() {
        assertThatCode(() -> validator.validate(plan(8), FurnitureAdditionPolicy.MAX_NEW_ADDITIONS))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(plan(9), FurnitureAdditionPolicy.MAX_NEW_ADDITIONS))
                .isInstanceOf(CustomException.class);
    }

    private FeedbackPlan plan(int operationCount) {
        List<FeedbackOperation> operations = IntStream.range(0, operationCount)
                .mapToObj(index -> new FeedbackOperation(
                        "add-" + index,
                        FeedbackOperationType.ADD_FURNITURE,
                        new FeedbackTargetSelector("", "desk", ""),
                        null,
                        new FeedbackPlacement(FeedbackRelation.NEAR_WALL, null, null, null),
                        null,
                        new FeedbackProductRequirements("desk", FeedbackSizePreference.ANY, false, List.of()),
                        null,
                        List.of()))
                .toList();
        return new FeedbackPlan("2.0", operationCount == 1 ? FeedbackRequestKind.DIRECT : FeedbackRequestKind.COMPOSITE,
                operations, List.of(), null, "test", FeedbackSource.RULE_BASED, false);
    }
}
