
package com.example.reactionchallenge.model;

public class GameRecord {
    public long id;
    public String playerName;
    public String mode;
    public String difficulty;
    public boolean training;
    public boolean completed;
    public int score;
    public int correctCount;
    public int wrongCount;
    public int missedCount;
    public long avgReactionMs;
    public long bestReactionMs;
    public long worstReactionMs;
    public int iterationsPerLevel;
    public int totalStimuli;
    public int levelsCompleted;
    public int maxReactionMs;
    public int livesStart;
    public int livesLeft;
    public long createdAt;
    public String ruleSummary;
}
