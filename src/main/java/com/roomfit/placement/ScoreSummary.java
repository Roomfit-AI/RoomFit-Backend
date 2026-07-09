package com.roomfit.placement;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추천 점수 요약. validationResult, 생활 목표, 스타일 태그 일치도를 기반으로 계산됩니다.")
public class ScoreSummary {

    @Schema(description = "세부 점수 합계", example = "590")
    private final int totalScore;
    @Schema(description = "충돌 점수. collisionFree=true이면 100, false이면 60", example = "100")
    private final int collisionScore;
    @Schema(description = "방 경계 점수. boundaryValid=true이면 100, false이면 60", example = "100")
    private final int boundaryScore;
    @Schema(description = "문/창문 여유 공간 점수", example = "100")
    private final int doorWindowScore;
    @Schema(description = "동선 점수", example = "100")
    private final int pathScore;
    @Schema(description = "생활 목표 적합도 점수", example = "95")
    private final int goalScore;
    @Schema(description = "스타일 태그 일치도 점수", example = "95")
    private final int styleScore;

    public ScoreSummary(int collisionScore, int boundaryScore, int doorWindowScore,
                         int pathScore, int goalScore, int styleScore) {
        this.collisionScore = collisionScore;
        this.boundaryScore = boundaryScore;
        this.doorWindowScore = doorWindowScore;
        this.pathScore = pathScore;
        this.goalScore = goalScore;
        this.styleScore = styleScore;
        this.totalScore = collisionScore + boundaryScore + doorWindowScore + pathScore + goalScore + styleScore;
    }

    public static ScoreSummary defaultSummary() {
        return new ScoreSummary(100, 100, 100, 100, 90, 90);
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getCollisionScore() {
        return collisionScore;
    }

    public int getBoundaryScore() {
        return boundaryScore;
    }

    public int getDoorWindowScore() {
        return doorWindowScore;
    }

    public int getPathScore() {
        return pathScore;
    }

    public int getGoalScore() {
        return goalScore;
    }

    public int getStyleScore() {
        return styleScore;
    }
}
