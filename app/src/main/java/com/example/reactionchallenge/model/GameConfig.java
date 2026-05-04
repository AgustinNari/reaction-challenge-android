
package com.example.reactionchallenge.model;

public class GameConfig {
    public String playerName;
    public GameMode mode;
    public Difficulty difficulty;
    public int iterationsPerLevel;
    public int maxReactionSeconds;
    public boolean soundEnabled;
    public boolean vibrationEnabled;
    public boolean menuMusicEnabled;
    public boolean gameMusicEnabled;
    public boolean trainingEnabled;
    public boolean trainingNoFail;
    public boolean trainingNoTimeLimit;
    public boolean trainingShowHelp;

    public boolean isDynamic() {
        return difficulty == Difficulty.DYNAMIC;
    }

    public boolean isInverse() {
        return mode != null && mode.isInverse();
    }

    public boolean isTraining() {
        return trainingEnabled;
    }
}
