package com.roomfit.product.service;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.repository.MockProductRepository;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 MockProductRepository 기본 카탈로그(desk 5종: desk-01, desk-compact-01,
 * desk-storage-01, desk-corner-01, desk-midcentury-glass-01)를 그대로 사용해
 * ProductRecommendationService의 선택 기준(스타일 교집합 -> 방 배치 가능 여부 -> 등록 순서)을
 * 검증한다.
 */
class ProductRecommendationServiceTest {

    private final ProductRecommendationService service = new ProductRecommendationService(new MockProductRepository());

    @Test
    void recommend_singleStyleMatch_picksTheOnlyOverlappingProduct() {
        AgentContext context = context(List.of(DesignStyle.MIDCENTURY));

        Optional<MockProduct> result = service.recommend("desk", context, room(4.0, 4.0));

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("desk-midcentury-glass-01");
    }

    @Test
    void recommend_tiedTotalScore_prefersProductThatFitsInRoom() {
        // "modern" 스타일 태그로는 desk-corner-01/desk-midcentury-glass-01이 동점(1점)이고,
        // STUDY_FOCUSED -> WORK_STUDY lifestyle 태그로도 둘 다 1점씩 더 받아 총점 2점으로
        // 계속 동점이다. 방을 좁게 잡아 desk-midcentury-glass-01(필요 가로 2.15m)은 못
        // 들어가고 desk-corner-01(필요 가로 1.7m)만 들어가게 한다.
        AgentContext context = context(List.of(DesignStyle.MODERN), LifestyleGoal.STUDY_FOCUSED);

        Optional<MockProduct> result = service.recommend("desk", context, room(2.0, 2.0));

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("desk-corner-01");
    }

    @Test
    void recommend_noStyleOrLifestyleOverlap_fallsBackToCatalogRegistrationOrder() {
        // "cozy" 스타일 태그는 어떤 desk도 갖고 있지 않고, RELAX_FOCUSED -> "REST" lifestyle
        // 태그도 desk 카탈로그 어디에도 없어 전부 0점 동점 -> 등록 순서상 첫 desk
        // 항목(desk-01)으로 결정적으로 fallback한다.
        AgentContext context = context(List.of(DesignStyle.COZY), LifestyleGoal.RELAX_FOCUSED);

        Optional<MockProduct> result = service.recommend("desk", context, room(10.0, 10.0));

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("desk-01");
    }

    @Test
    void recommend_lifestyleGoalBreaksStyleTie_prefersHigherLifestyleOverlap() {
        // designStyle을 비워 style 점수를 전부 0으로 무력화하면, STORAGE_FOCUSED ->
        // "STORAGE" lifestyle 태그를 가진 desk-storage-01/desk-corner-01만 1점으로
        // 동점 최상위가 된다(desk-01/desk-compact-01/desk-midcentury-glass-01은
        // STORAGE 태그가 없어 0점). 방 깊이를 desk-corner-01이 못 들어가게 좁혀
        // desk-storage-01만 남긴다.
        AgentContext context = context(List.of(), LifestyleGoal.STORAGE_FOCUSED);

        Optional<MockProduct> result = service.recommend("desk", context, room(2.0, 1.3));

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("desk-storage-01");
    }

    @Test
    void recommend_typeNotInCatalog_returnsEmpty() {
        AgentContext context = context(List.of(DesignStyle.MINIMAL), LifestyleGoal.STUDY_FOCUSED);

        Optional<MockProduct> result = service.recommend("sofa", context, room(4.0, 4.0));

        assertThat(result).isEmpty();
    }

    private AgentContext context(List<DesignStyle> designStyle) {
        return context(designStyle, LifestyleGoal.STUDY_FOCUSED);
    }

    private AgentContext context(List<DesignStyle> designStyle, LifestyleGoal lifestyleGoal) {
        return new AgentContext(1L, lifestyleGoal, designStyle,
                List.of("desk"), List.of(), List.of(1L), List.of(), List.of());
    }

    private Room room(double width, double depth) {
        return new Room(null, width, depth, 2.4, "meter", List.of(), List.of());
    }
}
