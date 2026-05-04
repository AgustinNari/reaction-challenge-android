package com.example.reactionchallenge.model;

import java.util.HashSet;
import java.util.Set;

public class StagePlan {
    private final Difficulty difficulty;
    private final GameRule rule;
    private final Set<StimulusType> allowedTypes;
    private final int maxReactionMs;
    private final int iterations;
    private final boolean hideRuleDuringPlay;
    private final String introText;

    public StagePlan(Difficulty difficulty, GameRule rule, Set<StimulusType> allowedTypes, int maxReactionMs, int iterations, boolean hideRuleDuringPlay, String introText) {
        this.difficulty = difficulty;
        this.rule = rule;
        this.allowedTypes = new HashSet<>(allowedTypes);
        this.maxReactionMs = maxReactionMs;
        this.iterations = iterations;
        this.hideRuleDuringPlay = hideRuleDuringPlay;
        this.introText = introText;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public GameRule getRule() {
        return rule;
    }

    public Set<StimulusType> getAllowedTypes() {
        return allowedTypes;
    }

    public int getMaxReactionMs() {
        return maxReactionMs;
    }

    public int getIterations() {
        return iterations;
    }

    public boolean isHideRuleDuringPlay() {
        return hideRuleDuringPlay;
    }

    public String getIntroText() {
        return introText;
    }
}
