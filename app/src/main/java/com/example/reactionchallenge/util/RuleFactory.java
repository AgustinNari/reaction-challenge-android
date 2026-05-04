package com.example.reactionchallenge.util;

import com.example.reactionchallenge.model.Difficulty;
import com.example.reactionchallenge.model.GameMode;
import com.example.reactionchallenge.model.GameRule;
import com.example.reactionchallenge.model.StagePlan;
import com.example.reactionchallenge.model.Stimulus;
import com.example.reactionchallenge.model.StimulusType;
import com.example.reactionchallenge.model.WordAccent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RuleFactory {

    private static final int STAGES_PER_GAME = 3;

    private RuleFactory() {
    }

    public static List<StagePlan> createStagePlans(GameMode mode,
                                                   Difficulty selectedDifficulty,
                                                   int iterationsPerLevel,
                                                   int maxReactionSeconds,
                                                   boolean trainingNoTimeLimit) {
        Random random = new Random();
        int effectiveMaxMs = trainingNoTimeLimit
                ? ((mode != null && mode.isInverse()) ? 45000 : 0)
                : clamp(maxReactionSeconds, 1, 30) * 1000;

        List<Difficulty> stageDifficulties;
        if (selectedDifficulty == Difficulty.DYNAMIC) {
            stageDifficulties = Arrays.asList(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD);
        } else {
            stageDifficulties = Arrays.asList(selectedDifficulty, selectedDifficulty, selectedDifficulty);
        }

        List<StagePlan> plans = new ArrayList<>(STAGES_PER_GAME);
        boolean dynamicMode = selectedDifficulty == Difficulty.DYNAMIC;

        if (dynamicMode) {
            for (int i = 0; i < STAGES_PER_GAME; i++) {
                Difficulty stageDifficulty = stageDifficulties.get(i);
                List<GameRule> rulePool = buildRulePool(stageDifficulty, random);
                GameRule rule = rulePool.get(0);
                Set<StimulusType> allowedTypes = pickAllowedTypes(stageDifficulty, rule.getFocusType(), random);
                boolean hideRuleDuringPlay = stageDifficulty == Difficulty.HARD;
                String introText = buildIntroText(stageDifficulty, allowedTypes, hideRuleDuringPlay, true, i + 1);
                plans.add(new StagePlan(stageDifficulty, rule, allowedTypes, effectiveMaxMs, iterationsPerLevel, hideRuleDuringPlay, introText));
            }
        } else {
            List<GameRule> rulePool = buildRulePool(selectedDifficulty, random);
            for (int i = 0; i < STAGES_PER_GAME; i++) {
                Difficulty stageDifficulty = stageDifficulties.get(i);
                GameRule rule = rulePool.get(i % rulePool.size());
                Set<StimulusType> allowedTypes = pickAllowedTypes(stageDifficulty, rule.getFocusType(), random);
                boolean hideRuleDuringPlay = stageDifficulty == Difficulty.HARD;
                String introText = buildIntroText(stageDifficulty, allowedTypes, hideRuleDuringPlay, false, i + 1);
                plans.add(new StagePlan(stageDifficulty, rule, allowedTypes, effectiveMaxMs, iterationsPerLevel, hideRuleDuringPlay, introText));
            }
        }

        return plans;
    }

    public static StagePlan createStagePlan(GameMode mode,
                                             Difficulty difficulty,
                                             int iterationsPerLevel,
                                             int maxReactionSeconds,
                                             boolean trainingNoTimeLimit) {
        List<StagePlan> plans = createStagePlans(mode, difficulty, iterationsPerLevel, maxReactionSeconds, trainingNoTimeLimit);
        return plans.isEmpty() ? null : plans.get(0);
    }

    private static List<GameRule> buildRulePool(Difficulty difficulty, Random random) {
        List<GameRule> pool = new ArrayList<>();

        switch (difficulty) {
            case EASY:
                pool.addAll(buildEasyRules());
                break;
            case MEDIUM:
                pool.addAll(buildMediumRules());
                break;
            case HARD:
                pool.addAll(buildHardRules());
                break;
            case DYNAMIC:
            default:
                pool.addAll(buildHardRules());
                break;
        }

        Collections.shuffle(pool, random);
        if (pool.size() > 3) {
            return new ArrayList<>(pool.subList(0, 3));
        }
        return pool;
    }

    private static List<GameRule> buildEasyRules() {
        List<GameRule> rules = new ArrayList<>();
        rules.add(new GameRule(
                "Tocá SI si aparece el color rojo",
                StimulusType.COLOR,
                s -> s.getType() == StimulusType.COLOR && s.isColor("Rojo")
        ));
        rules.add(new GameRule(
                "Tocá SI si aparece un color cálido",
                StimulusType.COLOR,
                s -> s.getType() == StimulusType.COLOR && s.isWarmColor()
        ));
        rules.add(new GameRule(
                "Tocá SI si el número es par",
                StimulusType.NUMBER,
                s -> s.getType() == StimulusType.NUMBER && s.isEven()
        ));
        rules.add(new GameRule(
                "Tocá SI si el número es primo",
                StimulusType.NUMBER,
                s -> s.getType() == StimulusType.NUMBER && s.isPrime()
        ));
        rules.add(new GameRule(
                "Tocá SI si la palabra es aguda",
                StimulusType.WORD,
                s -> s.getType() == StimulusType.WORD && s.isWordAccent(WordAccent.AGUDA)
        ));
        rules.add(new GameRule(
                "Tocá SI si la palabra tiene más de 5 letras",
                StimulusType.WORD,
                s -> s.getType() == StimulusType.WORD && s.wordLengthGreaterThan(5)
        ));
        return rules;
    }

    private static List<GameRule> buildMediumRules() {
        List<GameRule> rules = new ArrayList<>();
        rules.add(new GameRule(
                "Tocá SI si el número es primo y mayor que 20",
                StimulusType.NUMBER,
                s -> s.getType() == StimulusType.NUMBER && s.isPrime() && s.isGreaterThan(20)
        ));
        rules.add(new GameRule(
                "Tocá SI si el número es impar y múltiplo de 3",
                StimulusType.NUMBER,
                s -> s.getType() == StimulusType.NUMBER && s.isOdd() && s.isMultipleOf3()
        ));
        rules.add(new GameRule(
                "Tocá SI si la palabra es esdrújula o tiene más de 6 letras",
                StimulusType.WORD,
                s -> s.getType() == StimulusType.WORD && (s.isWordAccent(WordAccent.ESDRUJULA) || s.wordLengthGreaterThan(6))
        ));
        rules.add(new GameRule(
                "Tocá SI si la palabra es grave/llana y tiene 6 letras o menos",
                StimulusType.WORD,
                s -> s.getType() == StimulusType.WORD && s.isWordAccent(WordAccent.GRAVE) && !s.wordLengthGreaterThan(6)
        ));
        rules.add(new GameRule(
                "Tocá SI si el color no es rojo ni amarillo",
                StimulusType.COLOR,
                s -> s.getType() == StimulusType.COLOR && !s.isColor("Rojo") && !s.isColor("Amarillo")
        ));
        rules.add(new GameRule(
                "Tocá SI si el color es verde o azul",
                StimulusType.COLOR,
                s -> s.getType() == StimulusType.COLOR && (s.isColor("Verde") || s.isColor("Azul"))
        ));
        return rules;
    }

    private static List<GameRule> buildHardRules() {
        List<GameRule> rules = new ArrayList<>();
        rules.add(new GameRule(
                "Tocá SI si el número es primo, mayor que 50 y no es múltiplo de 3",
                StimulusType.NUMBER,
                s -> s.getType() == StimulusType.NUMBER && s.isPrime() && s.isGreaterThan(50) && !s.isMultipleOf3()
        ));
        rules.add(new GameRule(
                "Tocá SI si el número es impar, mayor que 60 y no es múltiplo de 5",
                StimulusType.NUMBER,
                s -> s.getType() == StimulusType.NUMBER && s.isOdd() && s.isGreaterThan(60) && !s.isMultipleOf5()
        ));
        rules.add(new GameRule(
                "Tocá SI si la palabra es esdrújula, tiene más de 6 letras y no termina en vocal",
                StimulusType.WORD,
                s -> s.getType() == StimulusType.WORD && s.isWordAccent(WordAccent.ESDRUJULA) && s.wordLengthGreaterThan(6) && !endsWithVowel(s)
        ));
        rules.add(new GameRule(
                "Tocá SI si la palabra tiene más de 7 letras, no es aguda y termina en vocal",
                StimulusType.WORD,
                s -> s.getType() == StimulusType.WORD && s.wordLengthGreaterThan(7) && !s.isWordAccent(WordAccent.AGUDA) && endsWithVowel(s)
        ));
        rules.add(new GameRule(
                "Tocá SI si el color no es rojo, no es verde y no es amarillo",
                StimulusType.COLOR,
                s -> s.getType() == StimulusType.COLOR && !s.isColor("Rojo") && !s.isColor("Verde") && !s.isColor("Amarillo")
        ));
        rules.add(new GameRule(
                "Tocá SI si el color es azul o violeta y no es cálido",
                StimulusType.COLOR,
                s -> s.getType() == StimulusType.COLOR && (s.isColor("Azul") || s.isColor("Violeta")) && !s.isWarmColor()
        ));
        return rules;
    }


    private static Set<StimulusType> pickAllowedTypes(Difficulty difficulty, StimulusType focusType, Random random) {
        List<StimulusType> all = new ArrayList<>(Arrays.asList(StimulusType.COLOR, StimulusType.NUMBER, StimulusType.WORD));
        all.remove(focusType);
        Collections.shuffle(all, random);

        switch (difficulty) {
            case EASY:
                return EnumSet.of(focusType);
            case MEDIUM:
                EnumSet<StimulusType> medium = EnumSet.of(focusType);
                medium.add(all.get(0));
                return medium;
            case HARD:
            case DYNAMIC:
            default:
                return EnumSet.of(StimulusType.COLOR, StimulusType.NUMBER, StimulusType.WORD);
        }
    }

    private static String buildIntroText(Difficulty difficulty,
                                         Set<StimulusType> allowedTypes,
                                         boolean hideRuleDuringPlay,
                                         boolean dynamicMode,
                                         int stageNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("Etapa ").append(stageNumber).append(": ").append(difficulty.getLabel()).append('.');
        sb.append(allowedTypes.size() == 1 ? " Aparecerá sólo ": " Se pueden mezclar ").append(allowedTypes.size()).append(allowedTypes.size() == 1 ? " tipo de estímulo" : " tipos de estímulo").append('.');
        sb.append(allowedTypes.size() == 1 ? " Aparecerá: ": " Aparecerán: ").append(joinTypes(allowedTypes)).append('.');
        if (dynamicMode) {
            if (stageNumber == 1) {
                sb.append(" Esta partida dinámica empieza con una etapa más simple y sube la dificultad en las siguientes.");
            } else if (stageNumber == 2) {
                sb.append(" La complejidad sigue aumentando en la última etapa.");
            } else {
                sb.append(" Esta es la etapa más exigente de la partida dinámica.");
            }
        }
        if (hideRuleDuringPlay) {
            sb.append(" La regla se mostrará antes de empezar, pero se ocultará durante la ejecución.");
        }
        return sb.toString();
    }

    private static String joinTypes(Set<StimulusType> types) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (StimulusType type : types) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(type.getLabel());
            i++;
        }
        return sb.toString();
    }

    private static boolean endsWithVowel(Stimulus stimulus) {
        if (stimulus == null || stimulus.getWordInfo() == null) {
            return false;
        }
        String text = stimulus.getWordInfo().getText();
        if (text == null || text.isEmpty()) return false;
        char c = Character.toLowerCase(text.charAt(text.length() - 1));
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
