package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FurnitureAdditionPolicyTest {

    private final FurnitureAdditionPolicy policy = new FurnitureAdditionPolicy();

    @Test
    void acceptsEightNewAndTwelveActiveFurniture() {
        assertThatCode(() -> policy.validate(furniture(4, FurnitureStatus.EXISTING), types(8)))
                .doesNotThrowAnyException();
    }

    @Test
    void countsDuplicateTypesAsSeparateAdditions() {
        assertThatCode(() -> policy.validate(List.of(), List.of(
                "desk", "desk", "desk", "desk", "desk", "desk", "desk", "desk")))
                .doesNotThrowAnyException();
        assertRejected(List.of(), List.of(
                "desk", "desk", "desk", "desk", "desk", "desk", "desk", "desk", "desk"));
    }

    @Test
    void rejectsMoreThanEightNewOrMoreThanTwelveActiveFurniture() {
        assertRejected(furniture(5, FurnitureStatus.EXISTING), types(8));
        assertRejected(furniture(12, FurnitureStatus.EXISTING), types(1));
        assertRejected(List.of(), types(9));
    }

    @Test
    void excludesDeletedFurnitureFromActiveCount() {
        List<Furniture> existing = new java.util.ArrayList<>(furniture(4, FurnitureStatus.EXISTING));
        existing.addAll(furniture(20, FurnitureStatus.DELETED));
        assertThatCode(() -> policy.validate(existing, types(8))).doesNotThrowAnyException();
        assertThat(existing.stream().filter(FurnitureAdditionPolicy::active)).hasSize(4);
    }

    private void assertRejected(List<Furniture> existing, List<String> requested) {
        assertThatThrownBy(() -> policy.validate(existing, requested))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getErrorCode())
                        .isEqualTo(ErrorCode.FURNITURE_ADDITION_FAILED));
    }

    private List<String> types(int count) {
        return IntStream.range(0, count).mapToObj(index -> "desk").toList();
    }

    private List<Furniture> furniture(int count, FurnitureStatus status) {
        return IntStream.range(0, count)
                .mapToObj(index -> new Furniture("existing-" + status + "-" + index,
                        "desk", "Desk", 0.4, 0.4, 0.7,
                        new Position(1.0 + index, 1.0), 0, status,
                        "desk-compact-01", List.of("minimal"), "desk-compact"))
                .toList();
    }
}
