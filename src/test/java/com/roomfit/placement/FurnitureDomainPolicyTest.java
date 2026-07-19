package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FurnitureDomainPolicyTest {

    private final FurnitureDomainPolicy policy = new FurnitureDomainPolicy();

    @Test
    void loftBedWithoutSeparateDeskIsAllowed() {
        assertThatCode(() -> policy.validateFinalState(List.of(loftBed())))
                .doesNotThrowAnyException();
    }

    @Test
    void canonicalDeskWithoutLoftBedIsAllowed() {
        assertThatCode(() -> policy.validateFinalState(List.of(desk(FurnitureStatus.RECOMMENDED))))
                .doesNotThrowAnyException();
    }

    @Test
    void loftBedAndCanonicalDeskAreRejectedRegardlessOfTypeAlias() {
        Furniture aliasedDesk = new Furniture("desk-1", "DESK", "일반 책상", 1.2, 0.6, 0.72,
                new Position(4.0, 4.0), 0, FurnitureStatus.RECOMMENDED,
                "desk-compact-01", List.of(), "desk-compact");

        assertThatThrownBy(() -> policy.validateFinalState(List.of(loftBed(), aliasedDesk)))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.FURNITURE_DOMAIN_CONFLICT))
                .hasMessage("로프트 침대와 일반 책상은 동시에 배치할 수 없습니다.");
    }

    @Test
    void deletedDeskDoesNotConflictWithLoftBed() {
        assertThatCode(() -> policy.validateFinalState(List.of(loftBed(), desk(FurnitureStatus.DELETED))))
                .doesNotThrowAnyException();
    }

    static Furniture loftBed() {
        return new Furniture("loft-1", "bed", "책상 결합 로프트 침대", 2.1, 1.2, 1.8,
                new Position(2.0, 2.0), 0, FurnitureStatus.RECOMMENDED,
                "bed-loft-desk-01", List.of(), "bed-loft-desk");
    }

    static Furniture desk(FurnitureStatus status) {
        return new Furniture("desk-1", "desk", "일반 책상", 1.2, 0.6, 0.72,
                new Position(4.0, 4.0), 0, status,
                "desk-compact-01", List.of(), "desk-compact");
    }
}
