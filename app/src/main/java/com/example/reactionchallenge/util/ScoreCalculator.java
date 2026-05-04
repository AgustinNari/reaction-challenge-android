
package com.example.reactionchallenge.util;

import com.example.reactionchallenge.model.Difficulty;
import com.example.reactionchallenge.model.GameConfig;
import com.example.reactionchallenge.model.GameSessionResult;

public class ScoreCalculator {

    public static int calculate(GameSessionResult r, GameConfig config) {
        if (config != null && config.isTraining()) {
            return 0;
        }

        double total = Math.max(1, r.totalStimuli);
        double accuracy = (double) Math.max(0, r.correctCount) / total;
        double errorPenalty = (r.wrongCount * 80.0) + (r.missedCount * 60.0);

        double reactionRatio = 0.0;
        if (r.avgReactionMs > 0 && r.maxReactionMs > 0) {
            reactionRatio = 1.0 - Math.min(1.0, (double) r.avgReactionMs / (double) r.maxReactionMs);
        }

        double bestRatio = 0.0;
        if (r.bestReactionMs > 0 && r.maxReactionMs > 0) {
            bestRatio = 1.0 - Math.min(1.0, (double) r.bestReactionMs / (double) r.maxReactionMs);
        }

        double stabilityBonus = Math.max(0, r.levelsCompleted) * 120.0;
        double volumeBonus = Math.log10(Math.max(2, r.totalStimuli + 1)) * 110.0;
        double lifeBonus = Math.max(0, r.livesLeft) * 55.0;
        double reactionBonus = (reactionRatio * 220.0) + (bestRatio * 90.0);

        Difficulty difficulty = config == null || config.difficulty == null ? Difficulty.EASY : config.difficulty;
        double difficultyMultiplier = difficulty.getScoreMultiplier();

        double modeMultiplier = 1.0;
        if (config != null && config.isInverse()) {
            modeMultiplier += 0.12;
        }
        if (config != null && config.isDynamic()) {
            modeMultiplier += 0.18;
        }

        double configBonus = Math.max(0, config == null ? 20 : config.iterationsPerLevel) * 4.0;
        double timePenalty = Math.max(0, config == null ? 20 : config.maxReactionSeconds - 10) * 5.0;

        double raw = (accuracy * 850.0) + reactionBonus + stabilityBonus + volumeBonus + lifeBonus + configBonus - errorPenalty - timePenalty;
        raw = raw * difficultyMultiplier * modeMultiplier;
        return Math.max(0, (int) Math.round(raw));
    }
}
