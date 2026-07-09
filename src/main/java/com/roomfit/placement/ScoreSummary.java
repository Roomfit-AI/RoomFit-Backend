package com.roomfit.placement;

public class ScoreSummary {

    private final int totalScore;
    private final int collisionScore;
    private final int boundaryScore;
    private final int doorWindowScore;
    private final int pathScore;
    private final int goalScore;
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
