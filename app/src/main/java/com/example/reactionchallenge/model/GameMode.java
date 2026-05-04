
package com.example.reactionchallenge.model;

public enum GameMode {
    NORMAL("Normal", false),
    INVERSE("Reacción inversa", true);

    private final String label;
    private final boolean inverse;

    GameMode(String label, boolean inverse) {
        this.label = label;
        this.inverse = inverse;
    }

    public String getLabel() {
        return label;
    }

    public boolean isInverse() {
        return inverse;
    }

    public static GameMode fromLabel(String label) {
        for (GameMode mode : values()) {
            if (mode.label.equalsIgnoreCase(label) || mode.name().equalsIgnoreCase(label)) {
                return mode;
            }
        }
        return NORMAL;
    }

    @Override
    public String toString() {
        return label;
    }
}
