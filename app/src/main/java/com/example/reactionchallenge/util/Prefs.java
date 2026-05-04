
package com.example.reactionchallenge.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.reactionchallenge.model.Difficulty;
import com.example.reactionchallenge.model.GameConfig;
import com.example.reactionchallenge.model.GameMode;

public class Prefs {
    private static final String PREFS = "reaction_challenge_prefs";
    private static final String KEY_PLAYER = "player_name";
    private static final String KEY_MODE = "mode";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_ITERATIONS = "iterations";
    private static final String KEY_MAX_TIME = "max_time";
    private static final String KEY_SOUND = "sound";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_MENU_MUSIC = "menu_music";
    private static final String KEY_GAME_MUSIC = "game_music";
    private static final String KEY_LEGACY_MUSIC = "music";
    private static final String KEY_TRAINING = "training";
    private static final String KEY_TRAINING_NO_FAIL = "training_no_fail";
    private static final String KEY_TRAINING_NO_TIME = "training_no_time";
    private static final String KEY_TRAINING_HELP = "training_help";

    private static SharedPreferences sp(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getPlayerName(Context context) {
        return sp(context).getString(KEY_PLAYER, "");
    }

    public static void setPlayerName(Context context, String name) {
        sp(context).edit().putString(KEY_PLAYER, name == null ? "" : name.trim()).apply();
    }

    public static void clearPlayerName(Context context) {
        sp(context).edit().remove(KEY_PLAYER).apply();
    }

    public static GameConfig loadConfig(Context context) {
        SharedPreferences p = sp(context);
        GameConfig c = new GameConfig();
        c.playerName = getPlayerName(context);
        c.mode = GameMode.fromLabel(p.getString(KEY_MODE, GameMode.NORMAL.getLabel()));
        c.difficulty = Difficulty.fromLabel(p.getString(KEY_DIFFICULTY, Difficulty.EASY.getLabel()));
        c.iterationsPerLevel = clamp(p.getInt(KEY_ITERATIONS, 20), 1, 100);
        c.maxReactionSeconds = clamp(p.getInt(KEY_MAX_TIME, c.difficulty.getDefaultMaxSeconds()), 1, 30);
        c.soundEnabled = p.getBoolean(KEY_SOUND, true);
        c.vibrationEnabled = p.getBoolean(KEY_VIBRATION, true);

        boolean legacyMusic = p.contains(KEY_LEGACY_MUSIC) ? p.getBoolean(KEY_LEGACY_MUSIC, true) : true;
        c.menuMusicEnabled = p.getBoolean(KEY_MENU_MUSIC, legacyMusic);
        c.gameMusicEnabled = p.getBoolean(KEY_GAME_MUSIC, legacyMusic);

        c.trainingEnabled = p.getBoolean(KEY_TRAINING, false);
        c.trainingNoFail = p.getBoolean(KEY_TRAINING_NO_FAIL, true);
        c.trainingNoTimeLimit = p.getBoolean(KEY_TRAINING_NO_TIME, false);
        c.trainingShowHelp = p.getBoolean(KEY_TRAINING_HELP, true);
        return c;
    }

    public static void saveConfig(Context context, GameConfig c) {
        SharedPreferences.Editor editor = sp(context).edit()
                .putString(KEY_MODE, c.mode == null ? GameMode.NORMAL.getLabel() : c.mode.getLabel())
                .putString(KEY_DIFFICULTY, c.difficulty == null ? Difficulty.EASY.getLabel() : c.difficulty.getLabel())
                .putInt(KEY_ITERATIONS, clamp(c.iterationsPerLevel, 1, 100))
                .putInt(KEY_MAX_TIME, clamp(c.maxReactionSeconds, 1, 30))
                .putBoolean(KEY_SOUND, c.soundEnabled)
                .putBoolean(KEY_VIBRATION, c.vibrationEnabled)
                .putBoolean(KEY_MENU_MUSIC, c.menuMusicEnabled)
                .putBoolean(KEY_GAME_MUSIC, c.gameMusicEnabled)
                .putBoolean(KEY_LEGACY_MUSIC, c.menuMusicEnabled)
                .putBoolean(KEY_TRAINING, c.trainingEnabled)
                .putBoolean(KEY_TRAINING_NO_FAIL, c.trainingNoFail)
                .putBoolean(KEY_TRAINING_NO_TIME, c.trainingNoTimeLimit)
                .putBoolean(KEY_TRAINING_HELP, c.trainingShowHelp);
        editor.apply();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
