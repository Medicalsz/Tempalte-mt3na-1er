package com.medicare.controllers;

import com.medicare.services.ChatbotService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ChatbotController {

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox chatArea;
    @FXML
    private TextField inputField;

    private final ChatbotService chatbotService = new ChatbotService();

    @FXML
    public void initialize() {
        // Add a welcome message when the chat window opens
        addMessage("Hello! How can I help you today?", "bot");
    }

    @FXML
    private void onSendMessage() {
        String messageText = inputField.getText().trim();
        if (!messageText.isEmpty()) {
            addMessage(messageText, "user");
            inputField.clear();

            // --- AI Integration Point ---
            // For now, we'll just echo a simple reply.
            // Later, we will call the AI service here.
            generateBotResponse(messageText);
        }
    }

    private void generateBotResponse(String userMessage) {
        // Show a "typing..." indicator
        addMessage("...", "bot-typing");

        // Run the API call on a background thread to avoid freezing the UI
        new Thread(() -> {
            String botResponse = chatbotService.getResponse(userMessage);
            
            // Update the UI on the JavaFX Application Thread
            Platform.runLater(() -> {
                // Remove the "typing..." indicator
                removeLastMessage();
                addMessage(botResponse, "bot");
            });
        }).start();
    }

    private void addMessage(String text, String sender) {
        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("message-bubble");

        // Bind the max width of the label to 75% of the chat area's width to fix wrapping
        messageLabel.maxWidthProperty().bind(chatArea.widthProperty().multiply(0.75));

        HBox messageContainer = new HBox(messageLabel);

        if ("user".equals(sender)) {
            messageContainer.getStyleClass().add("user-message");
        } else if ("bot".equals(sender)) {
            messageContainer.getStyleClass().add("bot-message");
        } else if ("bot-typing".equals(sender)) {
            messageContainer.getStyleClass().add("bot-message");
            messageContainer.setId("typing-indicator"); // Add an ID to find it later
        }

        chatArea.getChildren().add(messageContainer);

        // Auto-scroll to the bottom
        // Use Platform.runLater to ensure the layout has been updated before scrolling
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void removeLastMessage() {
        // Find and remove the "typing..." indicator
        chatArea.getChildren().removeIf(node -> "typing-indicator".equals(node.getId()));
    }
}