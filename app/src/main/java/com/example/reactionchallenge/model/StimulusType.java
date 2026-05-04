package com.example.reactionchallenge.model;

public enum StimulusType {
    COLOR("Color"),
    NUMBER("Número"),
    WORD("Palabra");

    private final String label;

    StimulusType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
