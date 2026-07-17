package com.roomfit.product.service;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.repository.MockProductRepository;
import com.roomfit.room.Room;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 사용자가 특정 Product를 정확히 고르지 않은 가구 타입(예: "desk" 유형만 선택)에 대해
 * Catalog에서 Product 하나를 고른다. 호출부(RuleBasedPlacementService)가 이미
 * selectedProductIds의 exact match를 우선 처리하므로, 이 서비스는 그 매치가 없을
 * 때만 호출된다.
 *
 * 선택 기준(우선순위 순):
 * 1. context.designStyle을 정규화한 태그와 product.styleTags의 교집합 개수
 * 2. 동점이면 방에 실제로 들어갈 수 있는(치수+clearance) 후보를 우선
 * 3. 그래도 동점이면 Catalog 등록 순서상 첫 번째 (deterministic fallback)
 *
 * lifestyleGoal/preferredColorTone은 의도적으로 점수에 반영하지 않는다 — MockProduct에
 * lifestyleTags 필드가 없고, styleTags에 섞인 "study"/"relax" 같은 단어는 공식적으로
 * LifestyleGoal과 매핑되도록 설계된 값이 아니라 근거 없는 매핑을 만들지 않기 위함이다.
 * preferredColorTone도 Material Palette 매칭이 아직 없어 동일한 이유로 제외한다.
 */
@Service
public class ProductRecommendationService {

    private final MockProductRepository mockProductRepository;

    public ProductRecommendationService(MockProductRepository mockProductRepository) {
        this.mockProductRepository = mockProductRepository;
    }

    public Optional<MockProduct> recommend(String type, AgentContext context, Room room) {
        List<MockProduct> candidates = mockProductRepository.findAll().stream()
                .filter(product -> type.equals(product.getType()))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Set<String> normalizedStyleTags = normalizeDesignStyle(context.getDesignStyle());

        List<MockProduct> topByStyle = topScored(candidates, normalizedStyleTags);
        if (topByStyle.size() == 1) {
            return Optional.of(topByStyle.get(0));
        }

        List<MockProduct> fittable = topByStyle.stream()
                .filter(product -> fitsInRoom(product, room))
                .toList();
        List<MockProduct> tieBreakPool = fittable.isEmpty() ? topByStyle : fittable;

        // tieBreakPool은 candidates(= Catalog 등록 순서)의 부분집합이라 순서가
        // 그대로 유지된다 — 그중 첫 번째가 곧 "등록 순서 기반 deterministic fallback".
        return tieBreakPool.stream().findFirst();
    }

    private Set<String> normalizeDesignStyle(List<DesignStyle> designStyle) {
        Set<String> tags = new LinkedHashSet<>();
        if (designStyle != null) {
            designStyle.forEach(style -> tags.add(style.toStyleTag()));
        }
        return tags;
    }

    private List<MockProduct> topScored(List<MockProduct> candidates, Set<String> normalizedStyleTags) {
        int bestScore = candidates.stream()
                .mapToInt(product -> styleOverlapScore(product, normalizedStyleTags))
                .max()
                .orElse(0);

        return candidates.stream()
                .filter(product -> styleOverlapScore(product, normalizedStyleTags) == bestScore)
                .toList();
    }

    private int styleOverlapScore(MockProduct product, Set<String> normalizedStyleTags) {
        if (normalizedStyleTags.isEmpty()) {
            return 0;
        }
        return (int) product.getStyleTags().stream()
                .filter(normalizedStyleTags::contains)
                .count();
    }

    private boolean fitsInRoom(MockProduct product, Room room) {
        double requiredWidth = product.getWidth() + product.getRequiredClearance().getSide() * 2;
        double requiredDepth = product.getDepth() + product.getRequiredClearance().getFront();
        return requiredWidth <= room.getWidth() && requiredDepth <= room.getDepth();
    }
}
