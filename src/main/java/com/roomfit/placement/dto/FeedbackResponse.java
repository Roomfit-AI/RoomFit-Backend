package com.roomfit.placement.dto;

import com.roomfit.placement.Layout;
import com.roomfit.placement.FeedbackResult;
import com.roomfit.placement.RecommendationStatus;
import com.roomfit.placement.ScoreSummary;
import com.roomfit.placement.ValidationResult;
import com.roomfit.room.Furniture;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Schema(description = "사용자 피드백 기반 재추천 응답")
public class FeedbackResponse {

    @Schema(description = "새로 생성된 배치 ID", example = "2")
    private final Long layoutId;
    @Schema(description = "배치가 속한 방 ID", example = "1")
    private final Long roomId;
    @Schema(description = "피드백 결과 Layout의 원본 배치 ID", nullable = true)
    private final Long sourceLayoutId;
    @Schema(description = "최종 확정 여부")
    private final boolean confirmed;
    @Schema(description = "확정 시각", nullable = true)
    private final LocalDateTime confirmedAt;
    @Schema(description = "재추천 상태", example = "SUCCESS")
    private final RecommendationStatus status;
    @Schema(description = "피드백을 반영한 추천 가구 배열")
    private final List<Furniture> recommendedFurniture;
    @Schema(description = "재추천 점수 요약")
    private final ScoreSummary scoreSummary;
    @Schema(description = "재추천 배치 검증 결과")
    private final ValidationResult validationResult;
    @Schema(description = "피드백 문장을 해석한 내부 의도. 프론트 디버깅/표시에 사용할 수 있습니다.")
    private final Map<String, Object> interpretedIntent;
    @Schema(description = "피드백 적용 결과. 실제 변경이 없을 때 이유를 포함합니다.")
    private final FeedbackResult feedbackResult;

    private FeedbackResponse(Layout layout, RecommendationStatus status, ScoreSummary scoreSummary,
                             ValidationResult validationResult, Map<String, Object> interpretedIntent,
                             FeedbackResult feedbackResult) {
        this.layoutId = layout.getId();
        this.roomId = layout.getRoomId();
        this.sourceLayoutId = layout.getSourceLayoutId();
        this.confirmed = layout.isConfirmed();
        this.confirmedAt = layout.getConfirmedAt();
        this.status = status;
        this.recommendedFurniture = layout.getFurniture();
        this.scoreSummary = scoreSummary;
        this.validationResult = validationResult;
        this.interpretedIntent = interpretedIntent;
        this.feedbackResult = feedbackResult;
    }

    public static FeedbackResponse of(Layout layout, RecommendationStatus status, ScoreSummary scoreSummary,
                                       ValidationResult validationResult, Map<String, Object> interpretedIntent) {
        return new FeedbackResponse(layout, status, scoreSummary, validationResult, interpretedIntent, null);
    }

    public static FeedbackResponse of(Layout layout, RecommendationStatus status, ScoreSummary scoreSummary,
                                      ValidationResult validationResult, Map<String, Object> interpretedIntent,
                                      FeedbackResult feedbackResult) {
        return new FeedbackResponse(layout, status, scoreSummary, validationResult, interpretedIntent, feedbackResult);
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getSourceLayoutId() {
        return sourceLayoutId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public List<Furniture> getRecommendedFurniture() {
        return recommendedFurniture;
    }

    public ScoreSummary getScoreSummary() {
        return scoreSummary;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public Map<String, Object> getInterpretedIntent() {
        return interpretedIntent;
    }

    public FeedbackResult getFeedbackResult() {
        return feedbackResult;
    }
}
