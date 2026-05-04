
package com.example.reactionchallenge.model;

public enum Difficulty {
    EASY("Fácil", 20, 5, 1.00),
    MEDIUM("Media", 15, 4, 1.20),
    HARD("Difícil", 10, 3, 1.45),
    DYNAMIC("Dinámica", 12, 4, 1.60);

    private final String label;
    private final int defaultMaxSeconds;
    private final int startingLives;
    private final double scoreMultiplier;

    Difficulty(String label, int defaultMaxSeconds, int startingLives, double scoreMultiplier) {
        this.label = label;
        this.defaultMaxSeconds = defaultMaxSeconds;
        this.startingLives = startingLives;
        this.scoreMultiplier = scoreMultiplier;
    }

    public String getLabel() {
        return label;
    }

    public int getDefaultMaxSeconds() {
        return defaultMaxSeconds;
    }

    public int getStartingLives() {
        return startingLives;
    }

    public double getScoreMultiplier() {
        return scoreMultiplier;
    }

    public static Difficulty fromLabel(String label) {
        for (Difficulty d : values()) {
            if (d.label.equalsIgnoreCase(label) || d.name().equalsIgnoreCase(label)) {
                return d;
            }
        }
        return EASY;
    }

    @Override
    public String toString() {
        return label;
    }
}
