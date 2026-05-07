package com.medicare.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.medicare.models.Score;

public class LeaderboardService {

    private static final List<Score> scores = new ArrayList<>();
    private static int totalGamesPlayed = 0;
    private static final int MAX_SCORES = 10;

    public void incrementGamesPlayed() {
        totalGamesPlayed++;
    }

    public void addScore(Score score) {
        scores.add(score);
        Collections.sort(scores);
        // Keep only the top N scores
        if (scores.size() > MAX_SCORES) {
            scores.remove(scores.size() - 1);
        }
    }

    public List<Score> getTopScores() {
        return new ArrayList<>(scores); // Return a copy
    }

    public int getTotalGamesPlayed() {
        return totalGamesPlayed;
    }
}