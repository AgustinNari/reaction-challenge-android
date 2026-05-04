
package com.example.reactionchallenge.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.reactionchallenge.model.GameRecord;
import com.example.reactionchallenge.model.PlayerStats;

import java.util.ArrayList;
import java.util.List;

public class AppDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "reaction_challenge.db";
    private static final int DB_VERSION = 2;

    public static final String TABLE_RECORDS = "game_records";
    public static final String TABLE_PLAYER_STATS = "player_stats";

    public AppDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_RECORDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_name TEXT NOT NULL," +
                "mode TEXT NOT NULL," +
                "difficulty TEXT NOT NULL," +
                "training INTEGER NOT NULL," +
                "completed INTEGER NOT NULL," +
                "score INTEGER NOT NULL," +
                "correct_count INTEGER NOT NULL," +
                "wrong_count INTEGER NOT NULL," +
                "missed_count INTEGER NOT NULL," +
                "avg_reaction_ms INTEGER NOT NULL," +
                "best_reaction_ms INTEGER NOT NULL," +
                "worst_reaction_ms INTEGER NOT NULL," +
                "iterations_per_level INTEGER NOT NULL," +
                "total_stimuli INTEGER NOT NULL," +
                "levels_completed INTEGER NOT NULL," +
                "max_reaction_ms INTEGER NOT NULL," +
                "lives_start INTEGER NOT NULL," +
                "lives_left INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "rule_summary TEXT" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_PLAYER_STATS + " (" +
                "player_name TEXT PRIMARY KEY," +
                "games_played INTEGER NOT NULL," +
                "games_won INTEGER NOT NULL," +
                "games_lost INTEGER NOT NULL," +
                "total_score INTEGER NOT NULL," +
                "best_score INTEGER NOT NULL," +
                "total_correct INTEGER NOT NULL," +
                "total_wrong INTEGER NOT NULL," +
                "total_missed INTEGER NOT NULL," +
                "total_reaction_ms INTEGER NOT NULL," +
                "reaction_samples INTEGER NOT NULL," +
                "best_reaction_ms INTEGER NOT NULL," +
                "worst_reaction_ms INTEGER NOT NULL," +
                "total_stimuli INTEGER NOT NULL," +
                "total_iterations INTEGER NOT NULL," +
                "last_played_at INTEGER NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYER_STATS);
        onCreate(db);
    }

    public long insertRecord(GameRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        long rowId = -1L;
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("player_name", record.playerName);
            values.put("mode", record.mode);
            values.put("difficulty", record.difficulty);
            values.put("training", record.training ? 1 : 0);
            values.put("completed", record.completed ? 1 : 0);
            values.put("score", record.score);
            values.put("correct_count", record.correctCount);
            values.put("wrong_count", record.wrongCount);
            values.put("missed_count", record.missedCount);
            values.put("avg_reaction_ms", record.avgReactionMs);
            values.put("best_reaction_ms", record.bestReactionMs);
            values.put("worst_reaction_ms", record.worstReactionMs);
            values.put("iterations_per_level", record.iterationsPerLevel);
            values.put("total_stimuli", record.totalStimuli);
            values.put("levels_completed", record.levelsCompleted);
            values.put("max_reaction_ms", record.maxReactionMs);
            values.put("lives_start", record.livesStart);
            values.put("lives_left", record.livesLeft);
            values.put("created_at", record.createdAt);
            values.put("rule_summary", record.ruleSummary);
            rowId = db.insert(TABLE_RECORDS, null, values);

            if (!record.training) {
                upsertPlayerStats(db, record);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return rowId;
    }

    private void upsertPlayerStats(SQLiteDatabase db, GameRecord record) {
        PlayerStats stats = getPlayerStats(db, record.playerName);
        boolean exists = stats.playerName != null;
        if (!exists) {
            stats = new PlayerStats();
            stats.playerName = record.playerName;
        }

        stats.gamesPlayed += 1;
        if (record.completed) {
            stats.gamesWon += 1;
        } else {
            stats.gamesLost += 1;
        }
        stats.totalScore += record.score;
        stats.bestScore = Math.max(stats.bestScore, record.score);
        stats.totalCorrect += record.correctCount;
        stats.totalWrong += record.wrongCount;
        stats.totalMissed += record.missedCount;
        stats.totalReactionMs += Math.max(0L, record.avgReactionMs);
        if (record.avgReactionMs > 0) {
            stats.reactionSamples += 1;
        }
        if (stats.bestReactionMs <= 0 || (record.bestReactionMs > 0 && record.bestReactionMs < stats.bestReactionMs)) {
            stats.bestReactionMs = record.bestReactionMs;
        }
        if (record.worstReactionMs > stats.worstReactionMs) {
            stats.worstReactionMs = record.worstReactionMs;
        }
        stats.totalStimuli += record.totalStimuli;
        stats.totalIterations += record.iterationsPerLevel;
        stats.lastPlayedAt = Math.max(stats.lastPlayedAt, record.createdAt);

        ContentValues values = new ContentValues();
        values.put("player_name", stats.playerName);
        values.put("games_played", stats.gamesPlayed);
        values.put("games_won", stats.gamesWon);
        values.put("games_lost", stats.gamesLost);
        values.put("total_score", stats.totalScore);
        values.put("best_score", stats.bestScore);
        values.put("total_correct", stats.totalCorrect);
        values.put("total_wrong", stats.totalWrong);
        values.put("total_missed", stats.totalMissed);
        values.put("total_reaction_ms", stats.totalReactionMs);
        values.put("reaction_samples", stats.reactionSamples);
        values.put("best_reaction_ms", stats.bestReactionMs);
        values.put("worst_reaction_ms", stats.worstReactionMs);
        values.put("total_stimuli", stats.totalStimuli);
        values.put("total_iterations", stats.totalIterations);
        values.put("last_played_at", stats.lastPlayedAt);

        long updated = db.update(TABLE_PLAYER_STATS, values, "player_name=?", new String[]{stats.playerName});
        if (updated == 0) {
            db.insert(TABLE_PLAYER_STATS, null, values);
        }
    }

    public List<GameRecord> getAllRecords() {
        List<GameRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_RECORDS + " ORDER BY score DESC, avg_reaction_ms ASC, created_at DESC", null);
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<GameRecord> getRecordsByPlayer(String playerName) {
        List<GameRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_RECORDS + " WHERE player_name=? ORDER BY score DESC, avg_reaction_ms ASC, created_at DESC",
                new String[]{playerName});
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public PlayerStats getPlayerStats(String playerName) {
        SQLiteDatabase db = getReadableDatabase();
        return getPlayerStats(db, playerName);
    }

    public List<PlayerStats> getAllPlayerStats() {
        List<PlayerStats> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PLAYER_STATS + " ORDER BY total_score DESC, best_score DESC, last_played_at DESC", null);
        try {
            while (c.moveToNext()) {
                list.add(fromStatsCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    private PlayerStats getPlayerStats(SQLiteDatabase db, String playerName) {
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PLAYER_STATS + " WHERE player_name=? LIMIT 1", new String[]{playerName});
        try {
            if (c.moveToFirst()) {
                return fromStatsCursor(c);
            }
        } finally {
            c.close();
        }
        PlayerStats empty = new PlayerStats();
        empty.playerName = playerName;
        return empty;
    }

    private GameRecord fromCursor(Cursor c) {
        GameRecord r = new GameRecord();
        r.id = c.getLong(c.getColumnIndexOrThrow("id"));
        r.playerName = c.getString(c.getColumnIndexOrThrow("player_name"));
        r.mode = c.getString(c.getColumnIndexOrThrow("mode"));
        r.difficulty = c.getString(c.getColumnIndexOrThrow("difficulty"));
        r.training = c.getInt(c.getColumnIndexOrThrow("training")) == 1;
        r.completed = c.getInt(c.getColumnIndexOrThrow("completed")) == 1;
        r.score = c.getInt(c.getColumnIndexOrThrow("score"));
        r.correctCount = c.getInt(c.getColumnIndexOrThrow("correct_count"));
        r.wrongCount = c.getInt(c.getColumnIndexOrThrow("wrong_count"));
        r.missedCount = c.getInt(c.getColumnIndexOrThrow("missed_count"));
        r.avgReactionMs = c.getLong(c.getColumnIndexOrThrow("avg_reaction_ms"));
        r.bestReactionMs = c.getLong(c.getColumnIndexOrThrow("best_reaction_ms"));
        r.worstReactionMs = c.getLong(c.getColumnIndexOrThrow("worst_reaction_ms"));
        r.iterationsPerLevel = c.getInt(c.getColumnIndexOrThrow("iterations_per_level"));
        r.totalStimuli = c.getInt(c.getColumnIndexOrThrow("total_stimuli"));
        r.levelsCompleted = c.getInt(c.getColumnIndexOrThrow("levels_completed"));
        r.maxReactionMs = c.getInt(c.getColumnIndexOrThrow("max_reaction_ms"));
        r.livesStart = c.getInt(c.getColumnIndexOrThrow("lives_start"));
        r.livesLeft = c.getInt(c.getColumnIndexOrThrow("lives_left"));
        r.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        r.ruleSummary = c.getString(c.getColumnIndexOrThrow("rule_summary"));
        return r;
    }

    private PlayerStats fromStatsCursor(Cursor c) {
        PlayerStats s = new PlayerStats();
        s.playerName = c.getString(c.getColumnIndexOrThrow("player_name"));
        s.gamesPlayed = c.getInt(c.getColumnIndexOrThrow("games_played"));
        s.gamesWon = c.getInt(c.getColumnIndexOrThrow("games_won"));
        s.gamesLost = c.getInt(c.getColumnIndexOrThrow("games_lost"));
        s.totalScore = c.getInt(c.getColumnIndexOrThrow("total_score"));
        s.bestScore = c.getInt(c.getColumnIndexOrThrow("best_score"));
        s.totalCorrect = c.getInt(c.getColumnIndexOrThrow("total_correct"));
        s.totalWrong = c.getInt(c.getColumnIndexOrThrow("total_wrong"));
        s.totalMissed = c.getInt(c.getColumnIndexOrThrow("total_missed"));
        s.totalReactionMs = c.getLong(c.getColumnIndexOrThrow("total_reaction_ms"));
        s.reactionSamples = c.getLong(c.getColumnIndexOrThrow("reaction_samples"));
        s.bestReactionMs = c.getLong(c.getColumnIndexOrThrow("best_reaction_ms"));
        s.worstReactionMs = c.getLong(c.getColumnIndexOrThrow("worst_reaction_ms"));
        s.totalStimuli = c.getInt(c.getColumnIndexOrThrow("total_stimuli"));
        s.totalIterations = c.getInt(c.getColumnIndexOrThrow("total_iterations"));
        s.lastPlayedAt = c.getLong(c.getColumnIndexOrThrow("last_played_at"));
        return s;
    }
}
