package com.example.reactionchallenge.util;

import com.example.reactionchallenge.model.WordAccent;
import com.example.reactionchallenge.model.WordInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WordBank {
    private static final List<WordInfo> WORDS = new ArrayList<>();

    static {
        WORDS.add(new WordInfo("casa", WordAccent.GRAVE));
        WORDS.add(new WordInfo("camión", WordAccent.AGUDA));
        WORDS.add(new WordInfo("música", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("árbol", WordAccent.GRAVE));
        WORDS.add(new WordInfo("teléfono", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("papel", WordAccent.AGUDA));
        WORDS.add(new WordInfo("fácil", WordAccent.GRAVE));
        WORDS.add(new WordInfo("rápido", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("lámpara", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("corazón", WordAccent.AGUDA));
        WORDS.add(new WordInfo("miércoles", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("cantar", WordAccent.AGUDA));
        WORDS.add(new WordInfo("mesa", WordAccent.GRAVE));
        WORDS.add(new WordInfo("pingüino", WordAccent.GRAVE));
        WORDS.add(new WordInfo("pájaro", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("sílaba", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("número", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("reloj", WordAccent.AGUDA));
        WORDS.add(new WordInfo("ventana", WordAccent.GRAVE));
        WORDS.add(new WordInfo("compás", WordAccent.AGUDA));
        WORDS.add(new WordInfo("brújula", WordAccent.ESDRUJULA));
        WORDS.add(new WordInfo("camino", WordAccent.GRAVE));
        WORDS.add(new WordInfo("café", WordAccent.AGUDA));
        WORDS.add(new WordInfo("sábado", WordAccent.ESDRUJULA));
    }

    public static WordInfo randomWord(Random random) {
        return WORDS.get(random.nextInt(WORDS.size()));
    }

    public static List<WordInfo> allWords() {
        return Collections.unmodifiableList(WORDS);
    }
}
