package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.product.repository.MockProductRepository;
import com.roomfit.product.service.MockProductService;
import com.roomfit.product.service.ProductRecommendationService;
import com.roomfit.room.Furniture;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * candidatePositions()가 방 실측 치수를 반영하는지, monitor/tv가 desk/media_console
 * 근처에 붙는지 검증한다. Position에는 높이(y)나 "무엇 위에 얹혔다"는 개념이 없어서
 * "책상 위에 모니터가 진짜로 겹쳐 놓인다"까지는 이 실험 범위에서 다루지 않는다 —
 * ValidationService/FurnitureDomainPolicy 변경이 필요하기 때문이다(별도 승인 필요).
 */
class RuleBasedPlacementPositioningTest {

    private final MockProductService mockProductService = new MockProductService(new MockProductRepository());
    private final ProductRecommendationService productRecommendationService =
            new ProductRecommendationService(new MockProductRepository());
    private final ValidationService validationService = new ValidationService();
    private final RuleBasedPlacementService service =
            new RuleBasedPlacementService(mockProductService, productRecommendationService, validationService);

    @Test
    void recommend_defaultCaseFurniture_staysProportionallyWithinSmallRoom() {
        // wardrobe는 desk/desk_chair/mood_lamp/monitor/tv/storage/rug/bed 전용
        // 규칙이 없는 "default" 케이스로 떨어지는 타입이다. 예전 고정 좌표
        // (2.2, 2.0)는 1.8x1.8m 방에서는 이미 방 밖이라 clamp가 벽 쪽으로
        // 눌러버린다 — 방 안쪽 15~85% 비례 구간을 벗어나야 정상이다.
        Room smallRoom = room(1.8, 1.8);
        AgentContext context = new AgentContext(1L, null, List.of(),
                List.of("wardrobe"), List.of(), List.of(), List.of(), List.of());

        PlacementResult result = service.recommend(context, smallRoom);

        assertThat(result.getRecommendationStatus()).isEqualTo(RecommendationExecutionStatus.SUCCESS);
        Furniture wardrobe = findByType(result.getRecommendedFurniture(), "wardrobe");
        assertThat(wardrobe.getPosition().getX())
                .isBetween(smallRoom.getWidth() * 0.15, smallRoom.getWidth() * 0.85);
        assertThat(wardrobe.getPosition().getZ())
                .isBetween(smallRoom.getDepth() * 0.15, smallRoom.getDepth() * 0.85);
    }

    @Test
    void recommend_defaultCaseFurniture_positionScalesWithRoomSize() {
        AgentContext context = new AgentContext(1L, null, List.of(),
                List.of("wardrobe"), List.of(), List.of(), List.of(), List.of());
        Room bigRoom = room(9.0, 7.0);

        Furniture inBigRoom = findByType(service.recommend(context, bigRoom).getRecommendedFurniture(), "wardrobe");

        // 첫 후보 좌표는 (width*0.25, depth*0.25) 근처여야 한다(clamp로 인한
        // 미세 보정은 허용).
        assertThat(inBigRoom.getPosition().getX()).isCloseTo(9.0 * 0.25, org.assertj.core.data.Offset.offset(0.5));
        assertThat(inBigRoom.getPosition().getZ()).isCloseTo(7.0 * 0.25, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void recommend_monitor_isAnchoredBehindDesk() {
        Room room = room(3.0, 3.0);
        AgentContext context = new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MODERN),
                List.of("desk", "monitor"), List.of(), List.of(), List.of(), List.of());

        PlacementResult result = service.recommend(context, room);

        assertThat(result.getRecommendationStatus()).isEqualTo(RecommendationExecutionStatus.SUCCESS);
        Furniture desk = findByType(result.getRecommendedFurniture(), "desk");
        Furniture monitor = findByType(result.getRecommendedFurniture(), "monitor");
        // monitor는 desk와 같은 x축 부근, desk 뒤쪽(z가 더 작은 쪽)에 붙어야 한다.
        assertThat(monitor.getPosition().getX()).isCloseTo(desk.getPosition().getX(), org.assertj.core.data.Offset.offset(desk.getWidth() / 2.0));
        assertThat(monitor.getPosition().getZ()).isLessThan(desk.getPosition().getZ());
        assertFullyValid(room, result);
    }

    @Test
    void recommend_tv_isAnchoredBehindMediaConsole() {
        Room room = room(4.0, 4.0);
        AgentContext context = new AgentContext(1L, LifestyleGoal.HOBBY_FOCUSED, List.of(DesignStyle.MODERN),
                List.of("media_console", "tv"), List.of(), List.of(), List.of(), List.of());

        PlacementResult result = service.recommend(context, room);

        assertThat(result.getRecommendationStatus()).isEqualTo(RecommendationExecutionStatus.SUCCESS);
        Furniture console = findByType(result.getRecommendedFurniture(), "media_console");
        Furniture tv = findByType(result.getRecommendedFurniture(), "tv");
        assertThat(tv.getPosition().getX()).isCloseTo(console.getPosition().getX(), org.assertj.core.data.Offset.offset(console.getWidth() / 2.0));
        assertThat(tv.getPosition().getZ()).isLessThan(console.getPosition().getZ());
        assertFullyValid(room, result);
    }

    @Test
    void recommend_monitorWithoutDesk_fallsBackToRoomProportionalPosition() {
        // desk가 없으면 anchoredCandidatePositions가 방 비례 기본 좌표로 fallback해야
        // 하고, 예외 없이 정상 배치돼야 한다.
        Room room = room(4.0, 4.0);
        AgentContext context = new AgentContext(1L, null, List.of(),
                List.of("monitor"), List.of(), List.of(), List.of(), List.of());

        PlacementResult result = service.recommend(context, room);

        assertThat(result.getRecommendationStatus()).isEqualTo(RecommendationExecutionStatus.SUCCESS);
        assertFullyValid(room, result);
    }

    private Furniture findByType(List<Furniture> furniture, String type) {
        return furniture.stream()
                .filter(item -> type.equals(item.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no furniture of type " + type + " in " + furniture));
    }

    private void assertFullyValid(Room room, PlacementResult result) {
        ValidationResult validation = validationService.validate(room, result.getRecommendedFurniture());
        assertThat(validation.isCollisionFree()).isTrue();
        assertThat(validation.isBoundaryValid()).isTrue();
        assertThat(validation.isDoorClearance()).isTrue();
        assertThat(validation.isWindowClearance()).isTrue();
        assertThat(validation.isPathSecured()).isTrue();
    }

    private Room room(double width, double depth) {
        return new Room(null, width, depth, 2.4, "meter", List.of(), List.of());
    }
}
