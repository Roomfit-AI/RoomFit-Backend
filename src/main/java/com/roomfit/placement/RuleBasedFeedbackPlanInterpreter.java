package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Room;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic fallback parser.  Supported compound requests are all-or-nothing plans. */
public class RuleBasedFeedbackPlanInterpreter implements FeedbackPlanInterpreter {

    private static final Map<String, String> FURNITURE_TERMS = furnitureTerms();
    private static final List<String> ADD_TERMS = List.of("추가", "놓아", "놓아줘", "놓고", "놓기", "넣어", "배치");
    private static final List<String> REMOVE_TERMS = List.of("제거", "삭제", "빼줘", "빼고", "없애", "치워");
    private static final List<String> ROTATE_TERMS = List.of("회전", "돌려", "돌려줘", "돌리", "90도", "180도", "반대로", "벽과 평행");
    private static final List<String> MOVE_TERMS = List.of("옮겨", "옮기", "이동", "당겨", "밀어", "앞으로", "뒤로");

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context) {
        return interpret(feedback, room, furniture, context, null);
    }

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context,
                                  String selectedFurnitureId) {
        String normalized = feedback == null ? "" : feedback.trim();
        if (normalized.isBlank()) throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);

        try {
            List<FurnitureMention> mentions = mentions(normalized);
            FeedbackPlan composite = compositePlan(normalized, mentions, furniture, selectedFurnitureId);
            if (composite != null) return composite;
            if (hasUnsupportedCompoundAction(normalized, mentions)) {
                throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
            }

            FeedbackOperation legacy = legacyDeskOperation(normalized, furniture);
            if (legacy != null) return bindSelectedTarget(direct(normalized, legacy), furniture, selectedFurnitureId);

            if (mentions.size() == 1) {
                String type = mentions.getFirst().type();
                FeedbackTargetSelector target = selector(type, normalized);
                if (containsAny(normalized, ROTATE_TERMS)) {
                    return bindSelectedTarget(direct(normalized, new FeedbackOperation("op-1", FeedbackOperationType.ROTATE,
                            target, null, new FeedbackPlacement(null, null, rotationOrientation(normalized)),
                            null, null, null, List.of())), furniture, selectedFurnitureId);
                }
                if (containsAny(normalized, MOVE_TERMS)) {
                    return bindSelectedTarget(direct(normalized, new FeedbackOperation("op-1", FeedbackOperationType.MOVE,
                            target, null, new FeedbackPlacement(moveRelation(normalized), FeedbackMagnitude.MEDIUM, null),
                            null, null, null, List.of())), furniture, selectedFurnitureId);
                }
                if (isSizeRequest(normalized)) {
                    boolean smaller = containsAny(normalized, List.of("작게", "작은 제품", "더 작은"));
                    return bindSelectedTarget(direct(normalized, new FeedbackOperation("op-1", FeedbackOperationType.REPLACE_PRODUCT,
                            target, null, null, new FeedbackReplaceConstraints(type, !smaller, smaller,
                            null, List.of(), List.of(), false), null, null, List.of())), furniture, selectedFurnitureId);
                }
            }
            if (isClearSwapRequest(normalized) && mentions.size() >= 2) {
                return bindSelectedTarget(direct(normalized, swapOperation(normalized, mentions)), furniture, selectedFurnitureId);
            }
            if (isClearSwapRequest(normalized) && mentions.size() == 1 && hasSizePreference(normalized)) {
                return bindSelectedTarget(direct(normalized, sameTypeSwapOperation(normalized, mentions.getFirst().type())),
                        furniture, selectedFurnitureId);
            }
            if (containsAny(normalized, REMOVE_TERMS) && mentions.size() == 1) {
                return bindSelectedTarget(direct(normalized, removeOperation(normalized, mentions.getFirst().type())),
                        furniture, selectedFurnitureId);
            }
            if (containsAny(normalized, ADD_TERMS) && !mentions.isEmpty()) return direct(normalized, addOperation(normalized, mentions));
        } catch (AmbiguousRuleTargetException e) {
            return clarification(e.getMessage(), e.targetFurnitureType());
        }
        throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
    }

    private FeedbackPlan compositePlan(String feedback, List<FurnitureMention> mentions,
                                       List<Furniture> furniture, String selectedFurnitureId) {
        if (mentions.size() == 1 && containsAny(feedback, MOVE_TERMS) && containsAny(feedback, ROTATE_TERMS)) {
            FeedbackTargetSelector target = plainSelector(mentions.getFirst().type());
            FeedbackOperation move = new FeedbackOperation("op-1", FeedbackOperationType.MOVE, target, null,
                    new FeedbackPlacement(moveRelation(feedback), FeedbackMagnitude.MEDIUM, null), null, null, null, List.of());
            FeedbackOperation rotate = new FeedbackOperation("op-2", FeedbackOperationType.ROTATE, target, null,
                    new FeedbackPlacement(null, null, rotationOrientation(feedback)), null, null, null, List.of("op-1"));
            return bindSelectedTarget(composite(feedback, List.of(move, rotate)), furniture, selectedFurnitureId);
        }
        if (mentions.size() == 1 && containsAny(feedback, MOVE_TERMS) && isSizeRequest(feedback)
                && feedback.contains("바꾸")) {
            String type = mentions.getFirst().type();
            boolean smaller = containsAny(feedback, List.of("작게", "작은 제품", "더 작은"));
            FeedbackTargetSelector target = plainSelector(type);
            FeedbackOperation replace = new FeedbackOperation("op-1", FeedbackOperationType.REPLACE_PRODUCT,
                    target, null, null, new FeedbackReplaceConstraints(type, !smaller, smaller,
                    null, List.of(), List.of(), false), null, null, List.of());
            FeedbackOperation move = new FeedbackOperation("op-2", FeedbackOperationType.MOVE,
                    target, null, new FeedbackPlacement(moveRelation(feedback), FeedbackMagnitude.MEDIUM, null),
                    null, null, null, List.of("op-1"));
            return bindSelectedTarget(composite(feedback, List.of(replace, move)), furniture, selectedFurnitureId);
        }
        if (mentions.size() >= 2 && feedback.contains("바꾸") && containsAny(feedback, MOVE_TERMS)
                && containsAny(feedback, List.of("우드", "디자인", "색", "톤"))) {
            FurnitureMention source = mentions.getFirst();
            FurnitureMention reference = mentions.get(1);
            FeedbackTargetSelector target = plainSelector(source.type());
            FeedbackOperation swap = new FeedbackOperation("op-1", FeedbackOperationType.SWAP_FURNITURE,
                    target, null, null, null, null, sameTypeRequirements(source.type(), feedback), List.of());
            FeedbackOperation move = new FeedbackOperation("op-2", FeedbackOperationType.MOVE,
                    target, plainSelector(reference.type()),
                    new FeedbackPlacement(relativeMoveRelation(feedback), FeedbackMagnitude.MEDIUM, null),
                    null, null, null, List.of("op-1"));
            return bindSelectedTarget(composite(feedback, List.of(swap, move)), furniture, selectedFurnitureId);
        }
        if (mentions.size() >= 2 && isClearSwapRequest(feedback) && containsAny(feedback, MOVE_TERMS)) {
            FurnitureMention source = mentions.getFirst();
            FurnitureMention replacement = mentions.getLast();
            if (!GeneratedFurnitureCatalog.get().sameType(source.type(), replacement.type())) {
                return clarification("교체 제품은 기존 가구와 같은 종류로 선택해주세요.", source.type());
            }
            FeedbackTargetSelector target = plainSelector(source.type());
            FeedbackOperation swap = new FeedbackOperation("op-1", FeedbackOperationType.SWAP_FURNITURE,
                    target, null, null, null, null, sameTypeRequirements(source.type(), feedback), List.of());
            FeedbackOperation move = new FeedbackOperation("op-2", FeedbackOperationType.MOVE,
                    target, null, new FeedbackPlacement(moveRelation(feedback), FeedbackMagnitude.MEDIUM, null),
                    null, null, null, List.of("op-1"));
            return bindSelectedTarget(composite(feedback, List.of(swap, move)), furniture, selectedFurnitureId);
        }
        if (mentions.size() >= 2 && containsAny(feedback, REMOVE_TERMS) && containsAny(feedback, ADD_TERMS)
                && !isClearSwapRequest(feedback)) {
            FurnitureMention source = mentions.getFirst();
            FurnitureMention addition = mentions.getLast();
            if (selectedFurnitureId == null || selectedFurnitureId.isBlank()) {
                return clarification("삭제할 가구를 하나로 특정해주세요.", source.type());
            }
            Furniture selected = selectedActiveFurniture(furniture, selectedFurnitureId);
            if (selected == null) return clarification("선택한 가구를 현재 배치에서 찾을 수 없습니다.", source.type());
            String selectedType = GeneratedFurnitureCatalog.get().normalizeType(selected.getType());
            if (!GeneratedFurnitureCatalog.get().sameType(source.type(), selectedType)) {
                return clarification("선택한 가구와 요청한 가구 종류가 다릅니다.", source.type());
            }
            FeedbackOperation remove = new FeedbackOperation("op-1", FeedbackOperationType.REMOVE_FURNITURE,
                    new FeedbackTargetSelector(selected.getId(), selectedType, ""), null, null, null, null, null, List.of());
            FeedbackOperation add = addOperation(feedback, List.of(addition));
            add = new FeedbackOperation("op-2", add.type(), add.target(), add.referenceTarget(), add.placement(),
                    add.constraints(), add.productRequirements(), add.replacementRequirements(), List.of("op-1"));
            return composite(feedback, List.of(remove, add));
        }
        return null;
    }

    private boolean hasUnsupportedCompoundAction(String feedback, List<FurnitureMention> mentions) {
        if (mentions.isEmpty() || !containsAny(feedback, List.of("하고", "옮기고", "바꾸고", "삭제하고", "추가하고", "돌리고"))) {
            return false;
        }
        return feedback.contains("바꾸") && !isSizeRequest(feedback) && mentions.size() == 1;
    }

    private FeedbackPlan bindSelectedTarget(FeedbackPlan plan, List<Furniture> furniture, String selectedFurnitureId) {
        if (selectedFurnitureId == null || selectedFurnitureId.isBlank() || plan.needsClarification()) return plan;
        Furniture selected = selectedActiveFurniture(furniture, selectedFurnitureId);
        if (selected == null) return clarification("선택한 가구를 현재 배치에서 찾을 수 없습니다.", "");
        String selectedType = GeneratedFurnitureCatalog.get().normalizeType(selected.getType());
        boolean mismatch = plan.operations().stream().anyMatch(operation -> {
            FeedbackTargetSelector target = operation.target();
            return operation.type() != FeedbackOperationType.ADD_FURNITURE && target.furnitureId().isBlank()
                    && target.labelKeyword().isBlank() && target.locationHint() == null && target.ordinal() == null
                    && !GeneratedFurnitureCatalog.get().sameType(target.furnitureType(), selectedType);
        });
        if (mismatch) return clarification("선택한 가구와 요청한 가구 종류가 다릅니다.", "");
        List<FeedbackOperation> operations = plan.operations().stream().map(operation -> {
            FeedbackTargetSelector target = operation.target();
            boolean generic = operation.type() != FeedbackOperationType.ADD_FURNITURE && target.furnitureId().isBlank()
                    && target.labelKeyword().isBlank() && target.locationHint() == null && target.ordinal() == null;
            if (!generic) return operation;
            return new FeedbackOperation(operation.operationId(), operation.type(),
                    new FeedbackTargetSelector(selected.getId(), selectedType, ""), operation.referenceTarget(), operation.placement(),
                    operation.constraints(), operation.productRequirements(), operation.replacementRequirements(), operation.dependsOn());
        }).toList();
        return new FeedbackPlan(plan.version(), plan.requestKind(), operations, plan.goals(), plan.clarification(),
                plan.reason(), plan.source(), plan.fallbackUsed());
    }

    private Furniture selectedActiveFurniture(List<Furniture> furniture, String selectedFurnitureId) {
        return furniture.stream().filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> selectedFurnitureId.equals(item.getId())).findFirst().orElse(null);
    }

    private FeedbackPlan clarification(String question, String type) {
        return new FeedbackPlan("2.0", FeedbackRequestKind.CLARIFICATION, List.of(), List.of(),
                new FeedbackClarification(question, type), question, FeedbackSource.RULE_BASED, true);
    }

    private FeedbackOperation legacyDeskOperation(String feedback, List<Furniture> furniture) {
        boolean larger = List.of("책상 더 크게", "책상을 조금 더 넓게 쓰고 싶어", "책상을 넓게", "책상 크게", "책상 키워줘", "책상이 더 컸으면 좋겠어").contains(feedback);
        boolean storage = List.of("수납 늘려줘", "수납공간이 많은 책상으로 바꿔줘").contains(feedback);
        boolean openSpace = "방이 넓어 보이게".equals(feedback);
        if (!larger && !storage && !openSpace) return null;
        List<Furniture> desks = furniture.stream().filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> "desk".equals(item.getType())).toList();
        if (desks.isEmpty()) throw new CustomException(ErrorCode.FURNITURE_NOT_FOUND);
        if (desks.size() > 1) throw new AmbiguousRuleTargetException("어떤 책상을 변경할지 알려주세요.", "desk");
        FeedbackTargetSelector target = new FeedbackTargetSelector(desks.getFirst().getId(), "desk", "");
        if (larger || storage) return new FeedbackOperation("op-1", FeedbackOperationType.REPLACE_PRODUCT, target, null,
                new FeedbackReplaceConstraints("desk", larger, null, List.of(), List.of(), storage), List.of());
        return new FeedbackOperation("op-1", FeedbackOperationType.MOVE, target,
                new FeedbackPlacement(FeedbackRelation.CENTER, FeedbackMagnitude.MEDIUM, null), null, List.of());
    }

    private boolean isSizeRequest(String feedback) { return containsAny(feedback, List.of("크게", "넓게", "큰 제품", "더 큰", "작게", "작은 제품", "더 작은")); }
    private FeedbackOrientation rotationOrientation(String feedback) {
        if (feedback.contains("180") || feedback.contains("반대로")) return FeedbackOrientation.HALF_TURN;
        if (feedback.contains("반시계")) return FeedbackOrientation.QUARTER_TURN_CCW;
        if (feedback.contains("벽과 평행")) return FeedbackOrientation.ALIGN_WITH_WALL;
        return FeedbackOrientation.QUARTER_TURN_CW;
    }
    private FeedbackRelation moveRelation(String feedback) {
        if (feedback.contains("모서리") || feedback.contains("구석") || feedback.contains("코너")) return FeedbackRelation.IN_CORNER;
        if (feedback.contains("뒤로") || feedback.contains("밀어")) return FeedbackRelation.BACKWARD;
        if (feedback.contains("앞으로") || feedback.contains("당겨")) return FeedbackRelation.FORWARD;
        if (feedback.contains("창가") || feedback.contains("창문")) return FeedbackRelation.NEAR_WINDOW;
        return FeedbackRelation.RIGHT;
    }

    private FeedbackRelation relativeMoveRelation(String feedback) {
        if (feedback.contains("왼쪽")) return FeedbackRelation.LEFT_OF;
        if (feedback.contains("오른쪽")) return FeedbackRelation.RIGHT_OF;
        return FeedbackRelation.NEXT_TO;
    }
    private FeedbackOperation addOperation(String feedback, List<FurnitureMention> mentions) {
        FurnitureMention targetMention = mentions.getLast();
        FurnitureMention reference = null;
        if (containsAny(feedback, List.of("옆", "왼쪽", "오른쪽")) && mentions.size() >= 2) {
            reference = mentions.getFirst();
            if (reference.type().equals(targetMention.type())) throw new AmbiguousRuleTargetException("추가할 가구와 기준 가구를 구분해서 알려주세요.");
        } else if (mentions.stream().map(FurnitureMention::type).distinct().count() > 1) {
            throw new AmbiguousRuleTargetException("어떤 가구를 추가할지 하나만 알려주세요.");
        }
        FeedbackRelation relation = addRelation(feedback, reference != null);
        FeedbackSide side = relation == FeedbackRelation.NEXT_TO ? feedback.contains("왼쪽") ? FeedbackSide.LEFT : feedback.contains("오른쪽") ? FeedbackSide.RIGHT : null : null;
        FeedbackTargetSelector referenceTarget = reference == null ? null : new FeedbackTargetSelector("", reference.type(), "");
        return new FeedbackOperation("op-1", FeedbackOperationType.ADD_FURNITURE, new FeedbackTargetSelector("", targetMention.type(), ""), referenceTarget,
                new FeedbackPlacement(relation, null, null, side), null, requirements(targetMention.type(), feedback), null, List.of());
    }
    private FeedbackOperation removeOperation(String feedback, String type) { return new FeedbackOperation("op-1", FeedbackOperationType.REMOVE_FURNITURE, selector(type, feedback), null, null, null, null, null, List.of()); }
    private FeedbackOperation swapOperation(String feedback, List<FurnitureMention> mentions) {
        FurnitureMention source = mentions.getFirst(); FurnitureMention replacement = mentions.getLast();
        if (source.type().equals(replacement.type()) && !hasSizePreference(feedback)) throw new AmbiguousRuleTargetException("교체할 기존 가구와 새 가구 종류를 구분해서 알려주세요.");
        return new FeedbackOperation("op-1", FeedbackOperationType.SWAP_FURNITURE, selector(source.type(), feedback), null, null, null, null, requirements(replacement.type(), feedback), List.of());
    }
    private FeedbackOperation sameTypeSwapOperation(String feedback, String type) { return new FeedbackOperation("op-1", FeedbackOperationType.SWAP_FURNITURE, selector(type, feedback), null, null, null, null, requirements(type, feedback), List.of()); }
    private FeedbackTargetSelector plainSelector(String type) { return new FeedbackTargetSelector("", type, ""); }
    private FeedbackProductRequirements sameTypeRequirements(String type, String feedback) {
        return new FeedbackProductRequirements(type, FeedbackSizePreference.SIMILAR, false, List.of());
    }
    private FeedbackTargetSelector selector(String type, String feedback) {
        FeedbackLocationHint hint = feedback.contains("창가") || feedback.contains("창문") ? FeedbackLocationHint.NEAR_WINDOW : feedback.contains("가운데") || feedback.contains("중앙") ? FeedbackLocationHint.CENTER : feedback.contains("가장 큰") ? FeedbackLocationHint.LARGEST : feedback.contains("가장 작은") ? FeedbackLocationHint.SMALLEST : null;
        return new FeedbackTargetSelector("", type, "", hint, ordinal(feedback));
    }
    private FeedbackProductRequirements requirements(String type, String feedback) {
        FeedbackSizePreference size = feedback.contains("작은") || feedback.contains("슬림") ? FeedbackSizePreference.SMALL : feedback.contains("큰") || feedback.contains("넓은") ? FeedbackSizePreference.LARGE : FeedbackSizePreference.ANY;
        return new FeedbackProductRequirements(type, size, feedback.contains("수납"), List.of());
    }
    private FeedbackRelation addRelation(String feedback, boolean hasReference) {
        if (feedback.contains("왼쪽")) return FeedbackRelation.LEFT_OF; if (feedback.contains("오른쪽")) return FeedbackRelation.RIGHT_OF;
        if (feedback.contains("옆") && hasReference) return FeedbackRelation.NEXT_TO; if (feedback.contains("창가") || feedback.contains("창문")) return FeedbackRelation.NEAR_WINDOW;
        if (feedback.contains("구석") || feedback.contains("코너")) return FeedbackRelation.IN_CORNER; if (feedback.contains("가운데") || feedback.contains("중앙")) return FeedbackRelation.CENTER; return FeedbackRelation.NEAR_WALL;
    }
    private FeedbackPlan direct(String reason, FeedbackOperation operation) { return new FeedbackPlan("2.0", FeedbackRequestKind.DIRECT, List.of(operation), List.of(), null, reason, FeedbackSource.RULE_BASED, true); }
    private FeedbackPlan composite(String reason, List<FeedbackOperation> operations) { return new FeedbackPlan("2.0", FeedbackRequestKind.COMPOSITE, operations, List.of(), null, reason, FeedbackSource.RULE_BASED, true); }
    private List<FurnitureMention> mentions(String feedback) {
        List<FurnitureMention> found = new ArrayList<>();
        for (Map.Entry<String, String> entry : FURNITURE_TERMS.entrySet()) for (int from = 0; from < feedback.length();) { int index = feedback.indexOf(entry.getKey(), from); if (index < 0) break; found.add(new FurnitureMention(entry.getValue(), index, entry.getKey().length())); from = index + entry.getKey().length(); }
        found.sort(Comparator.comparingInt(FurnitureMention::index).thenComparing(Comparator.comparingInt(FurnitureMention::length).reversed()));
        List<FurnitureMention> nonOverlapping = new ArrayList<>(); int end = -1;
        for (FurnitureMention mention : found) if (mention.index() >= end) { nonOverlapping.add(mention); end = mention.index() + mention.length(); }
        return nonOverlapping;
    }
    private Integer ordinal(String feedback) { if (feedback.contains("첫 번째") || feedback.contains("첫번째")) return 1; if (feedback.contains("두 번째") || feedback.contains("두번째")) return 2; if (feedback.contains("세 번째") || feedback.contains("세번째")) return 3; if (feedback.contains("네 번째") || feedback.contains("네번째")) return 4; return null; }
    private boolean containsAny(String value, List<String> terms) { return terms.stream().anyMatch(value::contains); }
    private boolean isClearSwapRequest(String feedback) { return containsAny(feedback, List.of("대신", "교체", "바꿔", "바꾸")) || feedback.contains("빼고") && containsAny(feedback, List.of("넣어", "놓아", "배치", "추가")); }
    private boolean hasSizePreference(String feedback) { return containsAny(feedback, List.of("작은", "슬림", "큰", "넓은")); }
    private static Map<String, String> furnitureTerms() {
        Map<String, String> terms = new LinkedHashMap<>();
        terms.put("책상 의자", "desk_chair"); terms.put("책상의자", "desk_chair"); terms.put("사이드 테이블", "side_table"); terms.put("사이드테이블", "side_table"); terms.put("1인용 의자", "desk_chair"); terms.put("전신 거울", "full_length_mirror"); terms.put("전신거울", "full_length_mirror"); terms.put("미디어 콘솔", "media_console"); terms.put("미디어콘솔", "media_console"); terms.put("소파 베드", "sofa_bed"); terms.put("소파베드", "sofa_bed"); terms.put("서랍장", "drawer_chest"); terms.put("수납장", "storage"); terms.put("협탁", "nightstand"); terms.put("책장", "bookshelf"); terms.put("행거", "hanger"); terms.put("파티션", "partition_shelf"); terms.put("식탁", "multi_table"); terms.put("테이블", "multi_table"); terms.put("책상", "desk"); terms.put("침대", "bed"); terms.put("소파", "sofa"); terms.put("의자", "desk_chair"); terms.put("조명", "mood_lamp"); terms.put("램프", "mood_lamp"); terms.put("스탠드", "mood_lamp"); terms.put("모니터", "monitor"); terms.put("블라인드", "curtain_blind"); terms.put("커튼", "curtain_blind"); terms.put("옷장", "wardrobe"); terms.put("거울", "full_length_mirror"); terms.put("화분", "plant"); terms.put("티비", "tv"); terms.put("TV", "tv"); terms.put("러그", "rug");
        return Map.copyOf(terms);
    }
    private record FurnitureMention(String type, int index, int length) { }
    private static final class AmbiguousRuleTargetException extends RuntimeException { private final String targetFurnitureType; private AmbiguousRuleTargetException(String question) { this(question, ""); } private AmbiguousRuleTargetException(String question, String type) { super(question); this.targetFurnitureType = type; } private String targetFurnitureType() { return targetFurnitureType; } }
}
