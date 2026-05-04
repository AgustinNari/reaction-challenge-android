package com.example.reactionchallenge.model;

public class Stimulus {
    private final StimulusType type;
    private final String displayText;
    private final String colorName;
    private final int backgroundColor;
    private final Integer numberValue;
    private final WordInfo wordInfo;

    public Stimulus(StimulusType type, String displayText, String colorName, int backgroundColor, Integer numberValue, WordInfo wordInfo) {
        this.type = type;
        this.displayText = displayText;
        this.colorName = colorName;
        this.backgroundColor = backgroundColor;
        this.numberValue = numberValue;
        this.wordInfo = wordInfo;
    }

    public StimulusType getType() {
        return type;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getColorName() {
        return colorName;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public Integer getNumberValue() {
        return numberValue;
    }

    public WordInfo getWordInfo() {
        return wordInfo;
    }

    public boolean isPrime() {
        if (numberValue == null || numberValue < 2) return false;
        int n = numberValue;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

    public boolean isOdd() {
        return numberValue != null && numberValue % 2 != 0;
    }

    public boolean isEven() {
        return numberValue != null && numberValue % 2 == 0;
    }

    public boolean isMultipleOf3() {
        return numberValue != null && numberValue % 3 == 0;
    }

    public boolean isMultipleOf5() {
        return numberValue != null && numberValue % 5 == 0;
    }

    public boolean isGreaterThan(int value) {
        return numberValue != null && numberValue > value;
    }

    public boolean isColor(String color) {
        return colorName != null && colorName.equalsIgnoreCase(color);
    }

    public boolean isWarmColor() {
        return colorName != null && (
                colorName.equalsIgnoreCase("Rojo")
                        || colorName.equalsIgnoreCase("Naranja")
                        || colorName.equalsIgnoreCase("Amarillo"));
    }

    public boolean isWordAccent(WordAccent accent) {
        return wordInfo != null && wordInfo.getAccent() == accent;
    }

    public boolean wordLengthGreaterThan(int size) {
        return wordInfo != null && wordInfo.getText().length() > size;
    }

    public String details() {
        if (type == StimulusType.NUMBER) {
            return "Número " + numberValue;
        } else if (type == StimulusType.WORD) {
            return "Palabra " + wordInfo.getText();
        } else {
            return "Color " + colorName;
        }
    }
}
