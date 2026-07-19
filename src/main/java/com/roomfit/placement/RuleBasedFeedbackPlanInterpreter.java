package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Room;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** A conservative Korean fallback.  It only creates an executable plan when one target is known. */
public class RuleBasedFeedbackPlanInterpreter implements FeedbackPlanInterpreter {

    private static final List<String> ADD_TERMS = List.of("추가", "하나 더", "새로", "넣어", "놓아", "놔", "있었으면 좋겠");
    private static final List<String> REMOVE_TERMS = List.of("삭제", "제거", "없애", "치워", "빼", "필요 없어");
    private static final List<String> MOVE_TERMS = List.of("옮겨", "옮기", "이동", "붙여", "붙이");
    private static final List<String> SWAP_TERMS = List.of("교체", "바꿔", "바꾸", "다른 디자인", "다른 제품");

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context) {
        return interpret(feedback, room, furniture, context, "");
    }

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context,
                                  String selectedFurnitureId) {
        String normalized = feedback == null ? "" : feedback.trim();
        if (normalized.isBlank()) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        }
        try {
            FeedbackOperation legacy = legacyDeskOperation(normalized, furniture);
            if (legacy != null) return direct(normalized, legacy);
            if (normalized.contains("색 바")) {
                throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
            }
            if (isGenericMetadataSwapRequest(normalized)) {
                Furniture selected = selectedActiveFurniture(selectedFurnitureId, furniture);
                if (selected == null) {
                    return clarification("교체할 가구를 선택해주세요.", "");
                }
                String selectedType = FeedbackVocabularyNormalizer.normalizeCanonicalType(selected.getType());
                return direct(normalized, sameTypeSwapOperation("op-1", normalized,
                        new FeedbackTargetSelector(selected.getId(), selectedType, ""), selectedType));
            }
            if (isGenericMoveRequest(normalized)) {
                Furniture selected = selectedActiveFurniture(selectedFurnitureId, furniture);
                if (selected == null) {
                    return clarification("어떤 가구를 말씀하시는지 확인이 필요합니다.", "");
                }
                String selectedType = FeedbackVocabularyNormalizer.normalizeCanonicalType(selected.getType());
                FeedbackTargetSelector target = new FeedbackTargetSelector(selected.getId(), selectedType, "");
                return direct(normalized, moveOperation("op-1", normalized, target, null));
            }
            if (isVague(normalized) || (FeedbackVocabularyNormalizer.isAmbiguousFurnitureWord(normalized)
                    && !isTypedMetadataSwapRequest(normalized))) {
                return clarification("어떤 가구를 말씀하시는지 확인이 필요합니다.", "");
            }
            if (isClassicSwap(normalized)) {
                return direct(normalized, swapOperation(normalized, mentions(normalized), furniture));
            }

            List<String> clauses = splitCompound(normalized);
            if (clauses.size() > FeedbackPlanValidator.MAX_FEEDBACK_OPERATIONS) {
                return clarification("한 번에 변경할 수 있는 항목 수를 초과했습니다.", "");
            }
            List<FeedbackOperation> operations = new ArrayList<>();
            for (String clause : clauses) {
                operations.add(parseClause(clause, room, furniture, operations.size() + 1, selectedFurnitureId));
            }
            if (operations.size() == 1) return direct(normalized, operations.getFirst());
            return new FeedbackPlan("2.0", FeedbackRequestKind.COMPOSITE, operations, List.of(), null,
                    normalized, FeedbackSource.RULE_BASED, true);
        } catch (ClarificationRequired e) {
            return clarification(e.getMessage(), e.targetFurnitureType());
        }
    }

    private FeedbackOperation parseClause(String clause, Room room, List<Furniture> furniture, int sequence,
                                         String selectedFurnitureId) {
        List<FurnitureMention> mentions = mentions(clause);
        if (mentions.isEmpty() && isTypedMetadataSwapRequest(clause)) {
            mentions = List.of(new FurnitureMention("drawer_chest", clause.indexOf("수납장"), "수납장".length()));
        }
        if (mentions.isEmpty()) {
            throw new ClarificationRequired("어떤 가구를 말씀하시는지 확인이 필요합니다.", "");
        }
        String operationId = "op-" + sequence;
        if (containsAny(clause, SWAP_TERMS)) {
            FeedbackTargetSelector target = selectorForExisting(mentions.getFirst().type(), clause, furniture);
            validateSelectionMatchesExplicitType(selectedFurnitureId, target.furnitureType(), furniture);
            return sameTypeSwapOperation(operationId, clause, target, target.furnitureType());
        }
        if (containsAny(clause, REMOVE_TERMS)) {
            return removeOperation(operationId, mentions.getFirst().type(), clause, furniture);
        }

        boolean explicitAdd = containsAny(clause, ADD_TERMS);
        boolean placeExisting = clause.contains("배치") && !explicitAdd
                && activeByType(mentions.getFirst().type(), furniture).size() == 1;
        if (explicitAdd || (clause.contains("배치") && !placeExisting)) {
            return addOperation(operationId, clause, mentions, furniture);
        }
        if (placeExisting || containsAny(clause, MOVE_TERMS)) {
            return moveOperation(operationId, clause, mentions, furniture);
        }
        throw new ClarificationRequired("요청한 변경 방식을 확인할 수 없습니다. 추가, 이동, 삭제, 교체 중 하나로 말씀해주세요.",
                mentions.getFirst().type());
    }

    private FeedbackOperation legacyDeskOperation(String feedback, List<Furniture> furniture) {
        boolean larger = List.of("책상 더 크게", "책상을 조금 더 넓게 쓰고 싶어", "책상을 넓게", "책상 크게",
                "책상 키워줘", "책상이 더 컸으면 좋겠어").contains(feedback);
        boolean storage = List.of("수납 늘려줘", "수납공간이 많은 책상으로 바꿔줘").contains(feedback);
        boolean openSpace = feedback.equals("방이 넓어 보이게") || feedback.equals("방이 넓어 보이게 정리해줘");
        if (!larger && !storage && !openSpace) return null;

        FeedbackTargetSelector target = selectorForExisting("desk", "", furniture);
        if (larger || storage) {
            return new FeedbackOperation("op-1", FeedbackOperationType.REPLACE_PRODUCT, target, null,
                    new FeedbackReplaceConstraints("desk", larger, null, List.of(), List.of(), storage), List.of());
        }
        return new FeedbackOperation("op-1", FeedbackOperationType.MOVE, target,
                new FeedbackPlacement(FeedbackRelation.CENTER, FeedbackMagnitude.MEDIUM, null), null, List.of());
    }

    private FeedbackOperation addOperation(String operationId, String feedback, List<FurnitureMention> mentions,
                                           List<Furniture> furniture) {
        FurnitureMention targetMention = mentions.getLast();
        FurnitureMention referenceMention = null;
        if (mentions.size() >= 2 && containsAny(feedback, List.of("옆", "왼쪽", "오른쪽", "근처"))) {
            referenceMention = mentions.getFirst();
            if (referenceMention.type().equals(targetMention.type())) {
                throw new ClarificationRequired("추가할 가구와 기준 가구를 구분해서 알려주세요.", targetMention.type());
            }
        } else if (mentions.stream().map(FurnitureMention::type).distinct().count() > 1) {
            throw new ClarificationRequired("어떤 가구를 추가할지 하나만 알려주세요.", "");
        }

        FeedbackRelation relation = addRelation(feedback, referenceMention != null);
        FeedbackSide side = relation == FeedbackRelation.NEXT_TO
                ? feedback.contains("왼쪽") ? FeedbackSide.LEFT
                : feedback.contains("오른쪽") ? FeedbackSide.RIGHT : null : null;
        FeedbackTargetSelector referenceTarget = referenceMention == null ? null
                : selectorForExisting(referenceMention.type(), feedback, furniture);
        return new FeedbackOperation(operationId, FeedbackOperationType.ADD_FURNITURE,
                new FeedbackTargetSelector("", targetMention.type(), ""), referenceTarget,
                new FeedbackPlacement(relation, null, null, side), null,
                requirements(targetMention.type(), feedback), null, List.of());
    }

    private FeedbackOperation removeOperation(String operationId, String type, String feedback, List<Furniture> furniture) {
        return new FeedbackOperation(operationId, FeedbackOperationType.REMOVE_FURNITURE,
                selectorForExisting(type, feedback, furniture), null, null, null, null, null, List.of());
    }

    private FeedbackOperation sameTypeSwapOperation(String operationId, String feedback,
                                                     FeedbackTargetSelector target, String type) {
        List<String> metadataKeywords = FeedbackMetadataKeywordNormalizer.keywordsFor(feedback);
        if (FeedbackMetadataKeywordNormalizer.containsMetadataRequest(feedback) && metadataKeywords.isEmpty()) {
            throw new ClarificationRequired("요청한 톤 또는 소재 조건은 현재 카탈로그 정보로 판단할 수 없습니다.", type);
        }
        return new FeedbackOperation(operationId, FeedbackOperationType.SWAP_FURNITURE,
                target, null, null, null, null,
                swapRequirements(type, feedback, metadataKeywords), List.of());
    }

    private FeedbackOperation swapOperation(String feedback, List<FurnitureMention> mentions, List<Furniture> furniture) {
        if (mentions.size() < 2) {
            throw new ClarificationRequired("교체할 기존 가구와 새 가구 종류를 구분해서 알려주세요.", "");
        }
        FurnitureMention source = mentions.getFirst();
        FurnitureMention replacement = mentions.getLast();
        return new FeedbackOperation("op-1", FeedbackOperationType.SWAP_FURNITURE,
                selectorForExisting(source.type(), feedback, furniture), null, null, null, null,
                requirements(replacement.type(), feedback), List.of());
    }

    private FeedbackOperation moveOperation(String operationId, String feedback, List<FurnitureMention> mentions,
                                            List<Furniture> furniture) {
        boolean referenceBeforeTarget = mentions.size() >= 2 && feedback.contains("옆에")
                && mentions.getFirst().index() < feedback.indexOf("옆에")
                && feedback.indexOf("옆에") < mentions.get(1).index();
        FurnitureMention targetMention = referenceBeforeTarget ? mentions.get(1) : mentions.getFirst();
        FeedbackTargetSelector target = selectorForExisting(targetMention.type(), feedback, furniture);
        FurnitureMention referenceMention = mentions.size() >= 2 && containsAny(feedback, List.of("옆", "근처", "가까이"))
                ? referenceBeforeTarget ? mentions.getFirst() : mentions.get(1) : null;
        if (referenceMention != null && referenceMention.type().equals(targetMention.type())) {
            throw new ClarificationRequired("이동할 가구와 기준 가구를 구분해서 알려주세요.", targetMention.type());
        }
        FeedbackTargetSelector reference = referenceMention == null ? null
                : selectorForExisting(referenceMention.type(), feedback, furniture);
        return moveOperation(operationId, feedback, target, reference);
    }

    private FeedbackOperation moveOperation(String operationId, String feedback, FeedbackTargetSelector target,
                                            FeedbackTargetSelector reference) {
        return new FeedbackOperation(operationId, FeedbackOperationType.MOVE,
                target, reference, movePlacement(feedback, reference != null), null, null, null, List.of());
    }

    private FeedbackPlacement movePlacement(String feedback, boolean hasReference) {
        if (containsAny(feedback, List.of("구석", "모서리", "코너"))) {
            return new FeedbackPlacement(FeedbackRelation.IN_CORNER, null, null);
        }
        if (hasReference) {
            FeedbackRelation relation = feedback.contains("왼쪽") ? FeedbackRelation.LEFT_OF
                    : feedback.contains("오른쪽") ? FeedbackRelation.RIGHT_OF : FeedbackRelation.NEXT_TO;
            FeedbackSide side = relation == FeedbackRelation.NEXT_TO && feedback.contains("왼쪽") ? FeedbackSide.LEFT
                    : relation == FeedbackRelation.NEXT_TO && feedback.contains("오른쪽") ? FeedbackSide.RIGHT : null;
            return new FeedbackPlacement(relation, null, null, side);
        }
        FeedbackRelation relation = feedback.contains("창가") || feedback.contains("창문") ? FeedbackRelation.NEAR_WINDOW
                : feedback.contains("문에서 멀") ? FeedbackRelation.AWAY_FROM_DOOR
                : feedback.contains("왼쪽") ? FeedbackRelation.LEFT
                : feedback.contains("오른쪽") ? FeedbackRelation.RIGHT
                : feedback.contains("가운데") || feedback.contains("중앙") ? FeedbackRelation.CENTER
                : feedback.contains("벽") ? FeedbackRelation.NEAR_WALL
                : FeedbackRelation.RIGHT;
        FeedbackMagnitude magnitude = containsAny(feedback, List.of("조금", "살짝"))
                ? FeedbackMagnitude.SMALL : FeedbackMagnitude.MEDIUM;
        return new FeedbackPlacement(relation, magnitude, null);
    }

    private FeedbackRelation addRelation(String feedback, boolean hasReference) {
        if (feedback.contains("왼쪽") && hasReference) return FeedbackRelation.LEFT_OF;
        if (feedback.contains("오른쪽") && hasReference) return FeedbackRelation.RIGHT_OF;
        if ((feedback.contains("옆") || feedback.contains("근처") || feedback.contains("가까이")) && hasReference) {
            return FeedbackRelation.NEXT_TO;
        }
        if (feedback.contains("창가") || feedback.contains("창문")) return FeedbackRelation.NEAR_WINDOW;
        if (containsAny(feedback, List.of("구석", "모서리", "코너"))) return FeedbackRelation.IN_CORNER;
        if (feedback.contains("가운데") || feedback.contains("중앙")) return FeedbackRelation.CENTER;
        return FeedbackRelation.NEAR_WALL;
    }

    private FeedbackProductRequirements requirements(String type, String feedback) {
        FeedbackSizePreference size = containsAny(feedback, List.of("작은", "슬림")) ? FeedbackSizePreference.SMALL
                : containsAny(feedback, List.of("큰", "넓은")) ? FeedbackSizePreference.LARGE : FeedbackSizePreference.ANY;
        return new FeedbackProductRequirements(type, size, feedback.contains("수납"), List.of());
    }

    private FeedbackProductRequirements swapRequirements(String type, String feedback, List<String> metadataKeywords) {
        FeedbackSizePreference size = containsAny(feedback, List.of("작은", "슬림")) ? FeedbackSizePreference.SMALL
                : containsAny(feedback, List.of("큰", "넓은")) ? FeedbackSizePreference.LARGE : FeedbackSizePreference.ANY;
        return new FeedbackProductRequirements(type, size, feedback.contains("수납"), metadataKeywords);
    }

    private FeedbackTargetSelector selectorForExisting(String type, String feedback, List<Furniture> furniture) {
        List<Furniture> matches = activeByType(type, furniture);
        if (matches.isEmpty()) {
            throw new ClarificationRequired("요청한 가구를 현재 배치에서 찾지 못했습니다.", type);
        }
        Integer ordinal = ordinal(feedback);
        if (ordinal != null) {
            List<Furniture> ordered = ordered(matches);
            if (ordinal <= ordered.size()) return new FeedbackTargetSelector(ordered.get(ordinal - 1).getId(), type, "");
            throw new ClarificationRequired("말씀하신 순서의 가구를 현재 배치에서 찾지 못했습니다.", type);
        }
        if (matches.size() == 1) return new FeedbackTargetSelector(matches.getFirst().getId(), type, "");
        throw new ClarificationRequired(candidateQuestion(type, matches), type);
    }

    private List<Furniture> activeByType(String type, List<Furniture> furniture) {
        return furniture.stream().filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> type.equals(FeedbackVocabularyNormalizer.normalizeCanonicalType(item.getType()))).toList();
    }

    private String candidateQuestion(String type, List<Furniture> candidates) {
        List<String> labels = ordered(candidates).stream().map(this::candidateLabel).toList();
        return "어떤 " + koreanType(type) + "을 말씀하시나요? " + String.join(", ", labels) + " 중에서 선택해주세요.";
    }

    private List<Furniture> ordered(List<Furniture> furniture) {
        return furniture.stream().sorted(Comparator.comparingDouble((Furniture item) -> item.getPosition().getX())
                .thenComparingDouble(item -> item.getPosition().getZ()).thenComparing(Furniture::getId)).toList();
    }

    private String candidateLabel(Furniture item) {
        String label = item.getLabel() == null ? "" : item.getLabel().trim();
        if (!label.isBlank() && !label.equalsIgnoreCase(item.getType())) return label;
        return koreanType(FeedbackVocabularyNormalizer.normalizeCanonicalType(item.getType()));
    }

    private String koreanType(String type) {
        return switch (type) {
            case "bed" -> "침대";
            case "desk" -> "책상";
            case "desk_chair" -> "의자";
            case "nightstand" -> "협탁";
            case "sofa" -> "소파";
            case "bookshelf" -> "책장";
            default -> "가구";
        };
    }

    private List<FurnitureMention> mentions(String feedback) {
        List<FurnitureMention> found = new ArrayList<>();
        String lower = feedback.toLowerCase(Locale.ROOT);
        for (var entry : FeedbackVocabularyNormalizer.aliasesByLength()) {
            String alias = entry.getKey().replace('_', ' ');
            int from = 0;
            while (from < lower.length()) {
                int index = lower.indexOf(alias, from);
                if (index < 0) break;
                found.add(new FurnitureMention(entry.getValue(), index, alias.length()));
                from = index + alias.length();
            }
        }
        found.sort(Comparator.comparingInt(FurnitureMention::index)
                .thenComparing(Comparator.comparingInt(FurnitureMention::length).reversed()));
        List<FurnitureMention> nonOverlapping = new ArrayList<>();
        int end = -1;
        for (FurnitureMention mention : found) {
            if (mention.index() >= end) {
                nonOverlapping.add(mention);
                end = mention.index() + mention.length();
            }
        }
        return nonOverlapping;
    }

    private List<String> splitCompound(String feedback) {
        String split = feedback.replace("그리고 나서", "|").replace("그리고", "|").replace("그다음", "|").replace(",", "|")
                .replace("삭제하고", "삭제|").replace("제거하고", "제거|")
                .replace("없애고", "없애|").replace("치우고", "치워|").replace("빼고", "빼|")
                .replace("옮겨 주고", "옮겨|").replace("옮겨주고", "옮겨|")
                .replace("옮긴 다음", "옮겨|").replace("옮긴 뒤", "옮겨|").replace("옮긴 후", "옮겨|")
                .replace("옮겨서", "옮겨|").replace("옮기고", "옮기|").replace("이동하고", "이동|").replace("붙이고", "붙이|")
                .replace("바꿔 주고", "바꿔|").replace("바꿔주고", "바꿔|").replace("바꾸고", "바꿔|")
                .replace("교체하고", "교체|").replace("추가하고", "추가|").replace("넣고", "넣어|")
                .replace("한 다음", "|").replace("한 뒤", "|").replace("한 후", "|").replace("하고 나서", "|");
        return java.util.Arrays.stream(split.split("\\|"))
                .map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    private boolean isClassicSwap(String feedback) {
        return feedback.contains("빼고") && containsAny(feedback, List.of("넣어", "놓아", "추가", "배치"));
    }

    private boolean isVague(String feedback) {
        return feedback.contains("저거") || feedback.equals("창가 쪽으로 옮겨줘")
                || feedback.contains("적당히") || feedback.contains("예쁘게") || feedback.equals("가구를 없애줘")
                || feedback.equals("가구를 모서리에 배치해줘");
    }

    private boolean isGenericMoveRequest(String feedback) {
        return feedback.contains("가구") && (feedback.contains("배치") || containsAny(feedback, MOVE_TERMS));
    }

    private boolean isGenericMetadataSwapRequest(String feedback) {
        return !feedback.contains("수납장") && mentions(feedback).isEmpty() && containsAny(feedback, SWAP_TERMS)
                && FeedbackMetadataKeywordNormalizer.containsMetadataRequest(feedback);
    }

    private boolean isTypedMetadataSwapRequest(String feedback) {
        return feedback.contains("수납장") && containsAny(feedback, SWAP_TERMS)
                && FeedbackMetadataKeywordNormalizer.containsMetadataRequest(feedback);
    }

    private void validateSelectionMatchesExplicitType(String selectedFurnitureId, String explicitType,
                                                      List<Furniture> furniture) {
        Furniture selected = selectedActiveFurniture(selectedFurnitureId, furniture);
        if (selected == null) return;
        String selectedType = FeedbackVocabularyNormalizer.normalizeCanonicalType(selected.getType());
        if (!explicitType.equals(selectedType)) {
            throw new ClarificationRequired("선택한 가구와 요청한 가구 종류가 다릅니다.", explicitType);
        }
    }

    private Furniture selectedActiveFurniture(String selectedFurnitureId, List<Furniture> furniture) {
        if (selectedFurnitureId == null || selectedFurnitureId.isBlank()) {
            return null;
        }
        return furniture.stream().filter(item -> selectedFurnitureId.equals(item.getId()))
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED).findFirst().orElse(null);
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

    private FeedbackPlan direct(String reason, FeedbackOperation operation) {
        return new FeedbackPlan("2.0", FeedbackRequestKind.DIRECT, List.of(operation), List.of(), null,
                reason, FeedbackSource.RULE_BASED, true);
    }

    private FeedbackPlan clarification(String question, String type) {
        return new FeedbackPlan("2.0", FeedbackRequestKind.CLARIFICATION, List.of(), List.of(),
                new FeedbackClarification(question, type), "", FeedbackSource.RULE_BASED, true);
    }

    private record FurnitureMention(String type, int index, int length) {
    }

    private static final class ClarificationRequired extends RuntimeException {
        private final String targetFurnitureType;

        private ClarificationRequired(String message, String targetFurnitureType) {
            super(message);
            this.targetFurnitureType = targetFurnitureType;
        }

        private String targetFurnitureType() {
            return targetFurnitureType;
        }
    }
}
