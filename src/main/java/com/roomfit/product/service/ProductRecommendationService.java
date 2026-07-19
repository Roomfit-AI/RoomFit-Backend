package com.roomfit.product.service;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
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
 * 1. styleScore(정규화된 designStyle과 product.styleTags 교집합 개수)와
 *    lifestyleScore(정규화된 lifestyleGoal과 product.lifestyleTags 교집합 개수)를
 *    각각 별도로 계산한 뒤 합산한 총점이 가장 높은 후보
 * 2. 총점 동점이면 방에 실제로 들어갈 수 있는(치수+clearance) 후보를 우선
 * 3. 그래도 동점이면 Catalog 등록 순서상 첫 번째 (deterministic fallback)
 *
 * JSON Variant Product에는 Furniture Catalog의 공식 lifestyleTags가 채워져 있고,
 * 기존 legacy Product는 빈 리스트를 유지한다. HOBBY_LEISURE처럼 대응되는
 * LifestyleGoal이 없는 태그는 LifestyleGoal.toLifestyleTag()가 만들어내지 않으므로
 * 어떤 lifestyleGoal로도 매칭되지 않는다 — 근거 없는 매핑을 임의로 만들지 않는다.
 *
 * preferredColorTone은 Material Palette 매칭이 아직 없어 이번 점수 계산에서 제외한다.
 */
@Service
public class ProductRecommendationService {

    private final MockProductRepository mockProductRepository;

    public ProductRecommendationService(MockProductRepository mockProductRepository) {
        this.mockProductRepository = mockProductRepository;
    }

    public Optional<MockProduct> recommend(String type, AgentContext context, Room room) {
        List<MockProduct> candidates = mockProductRepository.findAll().stream()
                .filter(product -> GeneratedFurnitureCatalog.get().sameType(type, product.getType()))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Set<String> normalizedStyleTags = normalizeDesignStyle(context.getDesignStyle());
        Set<String> normalizedLifestyleTags = normalizeLifestyleGoal(context.getLifestyleGoal());

        List<MockProduct> topScored = topScored(candidates, normalizedStyleTags, normalizedLifestyleTags);
        if (topScored.size() == 1) {
            return Optional.of(topScored.get(0));
        }

        List<MockProduct> fittable = topScored.stream()
                .filter(product -> fitsInRoom(product, room))
                .toList();
        List<MockProduct> tieBreakPool = fittable.isEmpty() ? topScored : fittable;

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

    private Set<String> normalizeLifestyleGoal(LifestyleGoal lifestyleGoal) {
        if (lifestyleGoal == null) {
            return Set.of();
        }
        return Set.of(lifestyleGoal.toLifestyleTag());
    }

    private List<MockProduct> topScored(List<MockProduct> candidates, Set<String> normalizedStyleTags,
                                         Set<String> normalizedLifestyleTags) {
        int bestScore = candidates.stream()
                .mapToInt(product -> totalScore(product, normalizedStyleTags, normalizedLifestyleTags))
                .max()
                .orElse(0);

        return candidates.stream()
                .filter(product -> totalScore(product, normalizedStyleTags, normalizedLifestyleTags) == bestScore)
                .toList();
    }

    private int totalScore(MockProduct product, Set<String> normalizedStyleTags, Set<String> normalizedLifestyleTags) {
        return styleOverlapScore(product, normalizedStyleTags) + lifestyleOverlapScore(product, normalizedLifestyleTags);
    }

    private int styleOverlapScore(MockProduct product, Set<String> normalizedStyleTags) {
        return overlapCount(product.getStyleTags(), normalizedStyleTags);
    }

    private int lifestyleOverlapScore(MockProduct product, Set<String> normalizedLifestyleTags) {
        return overlapCount(product.getLifestyleTags(), normalizedLifestyleTags);
    }

    private int overlapCount(List<String> productTags, Set<String> normalizedContextTags) {
        if (normalizedContextTags.isEmpty()) {
            return 0;
        }
        return (int) productTags.stream()
                .filter(normalizedContextTags::contains)
                .count();
    }

    private boolean fitsInRoom(MockProduct product, Room room) {
        boolean floorOverlay = "rug".equals(GeneratedFurnitureCatalog.get().normalizeType(product.getType()));
        double requiredWidth = product.getWidth()
                + (floorOverlay ? 0 : product.getRequiredClearance().getSide() * 2);
        double requiredDepth = product.getDepth()
                + (floorOverlay ? 0 : product.getRequiredClearance().getFront());
        return requiredWidth <= room.getWidth() && requiredDepth <= room.getDepth();
    }
}
