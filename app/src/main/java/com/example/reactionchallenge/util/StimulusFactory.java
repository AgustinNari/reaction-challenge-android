package com.example.reactionchallenge.util;

import android.graphics.Color;

import com.example.reactionchallenge.model.Stimulus;
import com.example.reactionchallenge.model.StimulusType;
import com.example.reactionchallenge.model.WordInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class StimulusFactory {
    private static final String[] COLOR_NAMES = new String[]{
            "Rojo", "Azul", "Verde", "Amarillo", "Naranja", "Violeta", "Negro", "Blanco", "Celeste", "Rosa"
    };

    private static final String[] COLOR_VALUES = new String[]{
            "#E53935", "#1E88E5", "#43A047", "#FDD835", "#FB8C00", "#8E24AA", "#212121", "#FAFAFA", "#4FC3F7", "#EC407A"
    };

    public static Stimulus randomFromAllowedTypes(Random random, Set<StimulusType> allowedTypes) {
        List<StimulusType> types = new ArrayList<>(allowedTypes);
        if (types.isEmpty()) {
            types.add(StimulusType.COLOR);
            types.add(StimulusType.NUMBER);
            types.add(StimulusType.WORD);
        }
        StimulusType type = types.get(random.nextInt(types.size()));
        return randomStimulus(random, type);
    }

    public static Stimulus randomStimulus(Random random, StimulusType type) {
        if (type == StimulusType.NUMBER) {
            int value = 1 + random.nextInt(150);
            int bgIndex = random.nextInt(COLOR_VALUES.length);
            return new Stimulus(
                    StimulusType.NUMBER,
                    String.valueOf(value),
                    COLOR_NAMES[bgIndex],
                    Color.parseColor(COLOR_VALUES[bgIndex]),
                    value,
                    null
            );
        } else if (type == StimulusType.WORD) {
            WordInfo word = WordBank.randomWord(random);
            int bgIndex = random.nextInt(COLOR_VALUES.length);
            return new Stimulus(
                    StimulusType.WORD,
                    word.getText(),
                    COLOR_NAMES[bgIndex],
                    Color.parseColor(COLOR_VALUES[bgIndex]),
                    null,
                    word
            );
        } else {
            int idx = random.nextInt(COLOR_VALUES.length);
            return new Stimulus(
                    StimulusType.COLOR,
                    COLOR_NAMES[idx],
                    COLOR_NAMES[idx],
                    Color.parseColor(COLOR_VALUES[idx]),
                    null,
                    null
            );
        }
    }
}
