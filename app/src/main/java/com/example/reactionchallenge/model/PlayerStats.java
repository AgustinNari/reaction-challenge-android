
package com.example.reactionchallenge.model;

public class PlayerStats {
    public String playerName;
    public int gamesPlayed;
    public int gamesWon;
    public int gamesLost;
    public int totalScore;
    public int bestScore;
    public int totalCorrect;
    public int totalWrong;
    public int totalMissed;
    public long totalReactionMs;
    public long reactionSamples;
    public long bestReactionMs;
    public long worstReactionMs;
    public int totalStimuli;
    public int totalIterations;
    public long lastPlayedAt;

    public long getAverageReactionMs() {
        return reactionSamples <= 0 ? 0L : totalReactionMs / reactionSamples;
    }

    public int getAverageScore() {
        return gamesPlayed <= 0 ? 0 : Math.round((float) totalScore / (float) gamesPlayed);
    }

    public int getWinRate() {
        return gamesPlayed <= 0 ? 0 : Math.round((gamesWon * 100f) / gamesPlayed);
    }
}
