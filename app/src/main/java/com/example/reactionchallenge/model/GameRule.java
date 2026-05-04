package com.example.reactionchallenge.model;

public class GameRule {
    public interface Evaluator {
        boolean matches(Stimulus stimulus);
    }

    private final String description;
    private final StimulusType focusType;
    private final Evaluator evaluator;

    public GameRule(String description, StimulusType focusType, Evaluator evaluator) {
        this.description = description;
        this.focusType = focusType;
        this.evaluator = evaluator;
    }

    public String getDescription() {
        return description;
    }

    public StimulusType getFocusType() {
        return focusType;
    }

    public boolean matches(Stimulus stimulus) {
        return evaluator.matches(stimulus);
    }
}
