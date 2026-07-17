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
    void recommend_tiedStyleScore_prefersProductThatFitsInRoom() {
        // desk-corner-01(minimal, modern)과 desk-midcentury-glass-01(midcentury, modern)이
        // "modern" 태그 1개로 동점이 된다. 방을 좁게 잡아 desk-midcentury-glass-01
        // (필요 가로 2.15m)은 못 들어가고 desk-corner-01(필요 가로 1.7m)만 들어가게 한다.
        AgentContext context = context(List.of(DesignStyle.MODERN));

        Optional<MockProduct> result = service.recommend("desk", context, room(2.0, 2.0));

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("desk-corner-01");
    }

    @Test
    void recommend_noStyleOverlap_fallsBackToCatalogRegistrationOrder() {
        // 어떤 desk 후보도 "cozy" 태그를 갖고 있지 않아 전부 0점 동점 -> 등록 순서상
        // 첫 desk 항목(desk-01)으로 결정적으로 fallback한다.
        AgentContext context = context(List.of(DesignStyle.COZY));

        Optional<MockProduct> result = service.recommend("desk", context, room(10.0, 10.0));

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("desk-01");
    }

    @Test
    void recommend_typeNotInCatalog_returnsEmpty() {
        AgentContext context = context(List.of(DesignStyle.MINIMAL));

        Optional<MockProduct> result = service.recommend("sofa", context, room(4.0, 4.0));

        assertThat(result).isEmpty();
    }

    private AgentContext context(List<DesignStyle> designStyle) {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, designStyle,
                List.of("desk"), List.of(), List.of(1L), List.of(), List.of());
    }

    private Room room(double width, double depth) {
        return new Room(null, width, depth, 2.4, "meter", List.of(), List.of());
    }
}
