package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Room;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleBasedFeedbackPlanInterpreter implements FeedbackPlanInterpreter {

    private static final Map<String, String> FURNITURE_TERMS = furnitureTerms();
    private static final List<String> ADD_TERMS = List.of("추가", "놓아", "놓아줘", "놓고", "놓기", "넣어", "배치");
    private static final List<String> REMOVE_TERMS = List.of("제거", "빼줘", "빼고", "없애", "치워");

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context) {
        String normalized = feedback == null ? "" : feedback.trim();
        if (normalized.isBlank()) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        }

        try {
            FeedbackOperation legacyDeskOperation = legacyDeskOperation(normalized, furniture);
            if (legacyDeskOperation != null) {
                return direct(normalized, legacyDeskOperation);
            }

            List<FurnitureMention> mentions = mentions(normalized);
            if (isClearSwapRequest(normalized) && mentions.size() >= 2) {
                return direct(normalized, swapOperation(normalized, mentions));
            }
            if (isClearSwapRequest(normalized) && mentions.size() == 1 && hasSizePreference(normalized)) {
                return direct(normalized, sameTypeSwapOperation(normalized, mentions.getFirst().type()));
            }
            if (containsAny(normalized, REMOVE_TERMS) && mentions.size() == 1) {
                return direct(normalized, removeOperation(normalized, mentions.getFirst().type()));
            }
            if (containsAny(normalized, ADD_TERMS) && !mentions.isEmpty()) {
                return direct(normalized, addOperation(normalized, mentions));
            }
        } catch (AmbiguousRuleTargetException e) {
            return new FeedbackPlan("2.0", FeedbackRequestKind.CLARIFICATION, List.of(), List.of(),
                    new FeedbackClarification(e.getMessage(), e.targetFurnitureType()), normalized,
                    FeedbackSource.RULE_BASED, true);
        }
        throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
    }

    private FeedbackOperation legacyDeskOperation(String feedback, List<Furniture> furniture) {
        boolean larger = List.of("책상 더 크게", "책상을 조금 더 넓게 쓰고 싶어", "책상을 넓게", "책상 크게",
                "책상 키워줘", "책상이 더 컸으면 좋겠어").contains(feedback);
        boolean storage = List.of("수납 늘려줘", "수납공간이 많은 책상으로 바꿔줘").contains(feedback);
        boolean openSpace = "방이 넓어 보이게".equals(feedback);
        if (!larger && !storage && !openSpace) {
            return null;
        }

        List<Furniture> desks = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> "desk".equals(item.getType()))
                .toList();
        if (desks.isEmpty()) {
            throw new CustomException(ErrorCode.FURNITURE_NOT_FOUND);
        }
        if (desks.size() > 1) {
            throw new AmbiguousRuleTargetException("어떤 책상을 변경할지 알려주세요.", "desk");
        }

        FeedbackTargetSelector target = new FeedbackTargetSelector(desks.getFirst().getId(), "desk", "");
        if (larger || storage) {
            return new FeedbackOperation("op-1", FeedbackOperationType.REPLACE_PRODUCT, target, null,
                    new FeedbackReplaceConstraints("desk", larger, null, List.of(), List.of(), storage), List.of());
        }
        return new FeedbackOperation("op-1", FeedbackOperationType.MOVE, target,
                new FeedbackPlacement(FeedbackRelation.CENTER, FeedbackMagnitude.MEDIUM, null), null, List.of());
    }

    private FeedbackOperation addOperation(String feedback, List<FurnitureMention> mentions) {
        FurnitureMention targetMention = mentions.getLast();
        FurnitureMention referenceMention = null;
        if (containsAny(feedback, List.of("옆", "왼쪽", "오른쪽")) && mentions.size() >= 2) {
            referenceMention = mentions.getFirst();
            if (referenceMention.type().equals(targetMention.type())) {
                throw new AmbiguousRuleTargetException("추가할 가구와 기준 가구를 구분해서 알려주세요.");
            }
        } else if (mentions.stream().map(FurnitureMention::type).distinct().count() > 1) {
            throw new AmbiguousRuleTargetException("어떤 가구를 추가할지 하나만 알려주세요.");
        }

        FeedbackRelation relation = addRelation(feedback, referenceMention != null);
        FeedbackSide side = relation == FeedbackRelation.NEXT_TO
                ? feedback.contains("왼쪽") ? FeedbackSide.LEFT
                : feedback.contains("오른쪽") ? FeedbackSide.RIGHT : null
                : null;
        FeedbackTargetSelector referenceTarget = referenceMention == null ? null
                : new FeedbackTargetSelector("", referenceMention.type(), "");
        FeedbackProductRequirements requirements = requirements(targetMention.type(), feedback);
        return new FeedbackOperation("op-1", FeedbackOperationType.ADD_FURNITURE,
                new FeedbackTargetSelector("", targetMention.type(), ""), referenceTarget,
                new FeedbackPlacement(relation, null, null, side), null,
                requirements, null, List.of());
    }

    private FeedbackOperation removeOperation(String feedback, String type) {
        return new FeedbackOperation("op-1", FeedbackOperationType.REMOVE_FURNITURE,
                selector(type, feedback), null, null, null, null, null, List.of());
    }

    private FeedbackOperation swapOperation(String feedback, List<FurnitureMention> mentions) {
        FurnitureMention source = mentions.getFirst();
        FurnitureMention replacement = mentions.getLast();
        if (source.type().equals(replacement.type()) && !hasSizePreference(feedback)) {
            throw new AmbiguousRuleTargetException("교체할 기존 가구와 새 가구 종류를 구분해서 알려주세요.");
        }
        return new FeedbackOperation("op-1", FeedbackOperationType.SWAP_FURNITURE,
                selector(source.type(), feedback), null, null, null, null,
                requirements(replacement.type(), feedback), List.of());
    }

    private FeedbackOperation sameTypeSwapOperation(String feedback, String type) {
        return new FeedbackOperation("op-1", FeedbackOperationType.SWAP_FURNITURE,
                selector(type, feedback), null, null, null, null,
                requirements(type, feedback), List.of());
    }

    private FeedbackTargetSelector selector(String type, String feedback) {
        FeedbackLocationHint locationHint = null;
        if (feedback.contains("창가") || feedback.contains("창문")) locationHint = FeedbackLocationHint.NEAR_WINDOW;
        else if (feedback.contains("가운데") || feedback.contains("중앙")) locationHint = FeedbackLocationHint.CENTER;
        else if (feedback.contains("가장 큰")) locationHint = FeedbackLocationHint.LARGEST;
        else if (feedback.contains("가장 작은")) locationHint = FeedbackLocationHint.SMALLEST;
        Integer ordinal = ordinal(feedback);
        return new FeedbackTargetSelector("", type, "", locationHint, ordinal);
    }

    private FeedbackProductRequirements requirements(String type, String feedback) {
        FeedbackSizePreference size = feedback.contains("작은") || feedback.contains("슬림")
                ? FeedbackSizePreference.SMALL
                : feedback.contains("큰") || feedback.contains("넓은")
                ? FeedbackSizePreference.LARGE : FeedbackSizePreference.ANY;
        return new FeedbackProductRequirements(type, size, feedback.contains("수납"), List.of());
    }

    private FeedbackRelation addRelation(String feedback, boolean hasReference) {
        if (feedback.contains("왼쪽")) return FeedbackRelation.LEFT_OF;
        if (feedback.contains("오른쪽")) return FeedbackRelation.RIGHT_OF;
        if (feedback.contains("옆") && hasReference) return FeedbackRelation.NEXT_TO;
        if (feedback.contains("창가") || feedback.contains("창문")) return FeedbackRelation.NEAR_WINDOW;
        if (feedback.contains("구석") || feedback.contains("코너")) return FeedbackRelation.IN_CORNER;
        if (feedback.contains("가운데") || feedback.contains("중앙")) return FeedbackRelation.CENTER;
        return FeedbackRelation.NEAR_WALL;
    }

    private FeedbackPlan direct(String reason, FeedbackOperation operation) {
        return new FeedbackPlan("2.0", FeedbackRequestKind.DIRECT, List.of(operation), List.of(), null,
                reason, FeedbackSource.RULE_BASED, true);
    }

    private List<FurnitureMention> mentions(String feedback) {
        List<FurnitureMention> mentions = new ArrayList<>();
        for (Map.Entry<String, String> entry : FURNITURE_TERMS.entrySet()) {
            int fromIndex = 0;
            while (fromIndex < feedback.length()) {
                int index = feedback.indexOf(entry.getKey(), fromIndex);
                if (index < 0) break;
                mentions.add(new FurnitureMention(entry.getValue(), index, entry.getKey().length()));
                fromIndex = index + entry.getKey().length();
            }
        }
        mentions.sort(Comparator.comparingInt(FurnitureMention::index)
                .thenComparing(Comparator.comparingInt(FurnitureMention::length).reversed()));

        List<FurnitureMention> nonOverlapping = new ArrayList<>();
        int previousEnd = -1;
        for (FurnitureMention mention : mentions) {
            if (mention.index() >= previousEnd) {
                nonOverlapping.add(mention);
                previousEnd = mention.index() + mention.length();
            }
        }
        return nonOverlapping;
    }

    private Integer ordinal(String feedback) {
        if (feedback.contains("첫 번째") || feedback.contains("첫번째")) return 1;
        if (feedback.contains("두 번째") || feedback.contains("두번째")) return 2;
        if (feedback.contains("세 번째") || feedback.contains("세번째")) return 3;
        if (feedback.contains("네 번째") || feedback.contains("네번째")) return 4;
        return null;
    }

    private boolean containsAny(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }

    private boolean isClearSwapRequest(String feedback) {
        if (containsAny(feedback, List.of("대신", "교체", "바꿔"))) {
            return true;
        }
        return feedback.contains("빼고")
                && containsAny(feedback, List.of("넣어", "놓아", "배치", "추가"));
    }

    private boolean hasSizePreference(String feedback) {
        return containsAny(feedback, List.of("작은", "슬림", "큰", "넓은"));
    }

    private static Map<String, String> furnitureTerms() {
        Map<String, String> terms = new LinkedHashMap<>();
        terms.put("책상 의자", "desk_chair");
        terms.put("책상의자", "desk_chair");
        terms.put("사이드 테이블", "side_table");
        terms.put("사이드테이블", "side_table");
        terms.put("1인용 의자", "desk_chair");
        terms.put("전신 거울", "full_length_mirror");
        terms.put("전신거울", "full_length_mirror");
        terms.put("미디어 콘솔", "media_console");
        terms.put("미디어콘솔", "media_console");
        terms.put("소파 베드", "sofa_bed");
        terms.put("소파베드", "sofa_bed");
        terms.put("서랍장", "drawer_chest");
        terms.put("수납장", "storage");
        terms.put("협탁", "nightstand");
        terms.put("책장", "bookshelf");
        terms.put("행거", "hanger");
        terms.put("파티션", "partition_shelf");
        terms.put("식탁", "multi_table");
        terms.put("테이블", "multi_table");
        terms.put("책상", "desk");
        terms.put("침대", "bed");
        terms.put("소파", "sofa");
        terms.put("의자", "desk_chair");
        terms.put("조명", "mood_lamp");
        terms.put("램프", "mood_lamp");
        terms.put("스탠드", "mood_lamp");
        terms.put("모니터", "monitor");
        terms.put("블라인드", "curtain_blind");
        terms.put("커튼", "curtain_blind");
        terms.put("옷장", "wardrobe");
        terms.put("거울", "full_length_mirror");
        terms.put("화분", "plant");
        terms.put("티비", "tv");
        terms.put("TV", "tv");
        terms.put("러그", "rug");
        return Map.copyOf(terms);
    }

    private record FurnitureMention(String type, int index, int length) {
    }

    private static final class AmbiguousRuleTargetException extends RuntimeException {
        private AmbiguousRuleTargetException(String question) {
            this(question, "");
        }

        private AmbiguousRuleTargetException(String question, String targetFurnitureType) {
            super(question);
            this.targetFurnitureType = targetFurnitureType;
        }

        private final String targetFurnitureType;

        private String targetFurnitureType() {
            return targetFurnitureType;
        }
    }
}
