package com.medicare.controllers;

import com.medicare.services.OpportunityRadarService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeController {

    // Helper record to hold the parts of an insight
    private record Insight(String title, String content, String suggestion) {}

    @FXML
    private VBox insightsContainer;

    private OpportunityRadarService radarService;

    @FXML
    public void initialize() {
        this.radarService = new OpportunityRadarService();
        loadInsightsInBackground();
    }

    private void loadInsightsInBackground() {
        // Show a loading indicator while the AI is working
        Label loadingLabel = new Label("Génération des aperçus par l'IA...");
        loadingLabel.getStyleClass().add("insight-card-content");
        insightsContainer.getChildren().setAll(loadingLabel);

        // Create a background task to call the AI service without freezing the UI
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // This runs on a background thread and uses the cache
                return radarService.generateInsights();
            }
        };

        // When the task is finished, update the UI on the JavaFX Application Thread
        task.setOnSucceeded(event -> {
            String rawInsights = task.getValue();
            parseAndDisplayInsights(rawInsights);
        });

        // Handle any errors that occur during the background task
        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            Label errorLabel = new Label("Erreur: Impossible de charger les aperçus de l'IA.");
            errorLabel.setStyle("-fx-text-fill: red;");
            insightsContainer.getChildren().setAll(errorLabel);
        });

        // Start the background task
        new Thread(task).start();
    }

    private void parseAndDisplayInsights(String rawInsights) {
        insightsContainer.getChildren().clear();
        List<Insight> parsedInsights = new ArrayList<>();

        StringBuilder contentBuilder = new StringBuilder();
        String currentTitle = null;
        String currentSuggestion = null;

        for (String line : rawInsights.lines().toList()) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            if (trimmedLine.startsWith("OPPORTUNITÉ:") || trimmedLine.startsWith("SYNERGIE:") || trimmedLine.startsWith("RISQUE:")) {
                // When we find a new title, save the previous insight if it exists
                if (currentTitle != null) {
                    parsedInsights.add(new Insight(currentTitle, contentBuilder.toString().trim(), currentSuggestion));
                    contentBuilder.setLength(0); // Reset for the next insight
                }
                currentTitle = trimmedLine;
                currentSuggestion = null; // Reset suggestion
            } else if (trimmedLine.startsWith("Suggestion:")) {
                currentSuggestion = trimmedLine.substring("Suggestion:".length()).trim();
            } else if (currentTitle != null) {
                // This line is part of the content of the current insight
                if (contentBuilder.length() > 0) {
                    contentBuilder.append(" ");
                }
                contentBuilder.append(trimmedLine);
            }
        }

        // Add the last insight after the loop finishes
        if (currentTitle != null) {
            parsedInsights.add(new Insight(currentTitle, contentBuilder.toString().trim(), currentSuggestion));
        }

        if (parsedInsights.isEmpty()) {
            Label rawTextLabel = new Label(rawInsights);
            rawTextLabel.setWrapText(true);
            rawTextLabel.getStyleClass().add("insight-card-content");
            insightsContainer.getChildren().add(rawTextLabel);
        } else {
            for (Insight insight : parsedInsights) {
                FontAwesomeSolid icon = FontAwesomeSolid.LIGHTBULB;
                if (insight.title.startsWith("SYNERGIE")) icon = FontAwesomeSolid.HANDSHAKE;
                else if (insight.title.startsWith("RISQUE")) icon = FontAwesomeSolid.EXCLAMATION_TRIANGLE;

                Node card = createInsightCard(insight.title, insight.content, insight.suggestion, icon);
                insightsContainer.getChildren().add(card);
            }
        }
    }

    private Node createInsightCard(String title, String content, String suggestion, FontAwesomeSolid icon) {
        VBox card = new VBox();
        card.getStyleClass().add("insight-card");

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(18);
        fontIcon.getStyleClass().add("insight-card-title");

        Label titleLabel = new Label(title, fontIcon);
        titleLabel.getStyleClass().add("insight-card-title");
        titleLabel.setAlignment(Pos.CENTER_LEFT);

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("insight-card-content");

        Label suggestionLabel = new Label("Suggestion: " + (suggestion != null ? suggestion : "N/A"));
        suggestionLabel.setWrapText(true);
        suggestionLabel.getStyleClass().add("insight-card-suggestion");

        card.getChildren().addAll(titleLabel, contentLabel, suggestionLabel);
        return card;
    }
}