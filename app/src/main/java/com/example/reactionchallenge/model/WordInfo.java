package com.example.reactionchallenge.model;

public class WordInfo {
    private final String text;
    private final WordAccent accent;

    public WordInfo(String text, WordAccent accent) {
        this.text = text;
        this.accent = accent;
    }

    public String getText() {
        return text;
    }

    public WordAccent getAccent() {
        return accent;
    }
}
