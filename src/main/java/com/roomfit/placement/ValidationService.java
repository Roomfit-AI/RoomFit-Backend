package com.roomfit.placement;

import com.roomfit.room.Furniture;
import com.roomfit.room.Opening;
import com.roomfit.room.Room;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 배치 검증 로직 (충돌 / 문 가림 / 창문 가림 / 동선 확보).
 * recommend와 validate/update 양쪽에서 재사용하는 별도 서비스로 분리.
 *
 * TODO: 지금은 스켈레톤 - 실제 충돌 판정(AABB 겹침 검사), 문/창문 앞 여유공간 계산,
 * 동선 확보(최소 통로 폭) 로직을 채워야 함.
 */
@Service
public class ValidationService {

    public ValidationResult validate(Room room, List<Furniture> furniture) {
        List<String> warnings = new ArrayList<>();

        boolean collisionFree = checkCollisionFree(furniture, warnings);
        boolean boundaryValid = checkBoundaryValid(room, furniture, warnings);
        boolean doorClearance = checkOpeningClearance(room.getOpenings(), furniture, "door", warnings);
        boolean windowClearance = checkOpeningClearance(room.getOpenings(), furniture, "window", warnings);
        boolean pathSecured = checkPathSecured(room, furniture, warnings);

        List<ValidationItem> validationItems = List.of(
                new ValidationItem("collision", collisionFree, collisionFree ? "가구 충돌 없음" : "가구 충돌 발생"),
                new ValidationItem("boundary", boundaryValid, boundaryValid ? "방 범위 내 배치" : "방 범위 밖 가구 존재"),
                new ValidationItem("door_clearance", doorClearance, doorClearance ? "문 앞 공간 확보" : "문 앞 공간 부족"),
                new ValidationItem("window_clearance", windowClearance, windowClearance ? "창문 앞 공간 확보" : "창문 앞 공간 부족"),
                new ValidationItem("path", pathSecured, pathSecured ? "이동 동선 확보" : "이동 동선 부족")
        );

        return new ValidationResult(collisionFree, boundaryValid, doorClearance,
                windowClearance, pathSecured, validationItems, warnings);
    }

    private boolean checkCollisionFree(List<Furniture> furniture, List<String> warnings) {
        // TODO: 가구 간 AABB(사각형 바운딩 박스) 겹침 검사 구현
        return true;
    }

    private boolean checkBoundaryValid(Room room, List<Furniture> furniture, List<String> warnings) {
        // TODO: 가구 중심 좌표와 full extent 기준 방 경계 검사 구현
        return true;
    }

    private boolean checkOpeningClearance(List<Opening> openings, List<Furniture> furniture,
                                           String openingType, List<String> warnings) {
        // TODO: 문/창문 앞 일정 반경 내 가구 존재 여부 검사 구현
        return true;
    }

    private boolean checkPathSecured(Room room, List<Furniture> furniture, List<String> warnings) {
        // TODO: 최소 동선(MIN_PATH_WIDTH) 확보 여부 검사 구현
        return true;
    }
}
