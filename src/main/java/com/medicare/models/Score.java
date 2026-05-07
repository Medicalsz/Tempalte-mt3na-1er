package com.medicare.models;

public class Score implements Comparable<Score> {
    private String playerName;
    private long timeInMillis;

    public Score(String playerName, long timeInMillis) {
        this.playerName = playerName;
        this.timeInMillis = timeInMillis;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public String getFormattedTime() {
        long seconds = timeInMillis / 1000;
        long millis = timeInMillis % 1000;
        return String.format("%d.%03ds", seconds, millis);
    }

    @Override
    public int compareTo(Score other) {
        return Long.compare(this.timeInMillis, other.timeInMillis);
    }
}