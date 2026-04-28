package com.medicare.services;

/**
 * An interface for chatbot services. This allows for multiple implementations
 * (e.g., Gemini, OpenAI, etc.) that can be used interchangeably by the application.
 */
public interface ChatbotServiceProvider {

    /**
     * Gets a response from the chatbot service based on the user's input.
     *
     * @param userInput The text message from the user.
     * @return The response from the AI service.
     */
    String getResponse(String userInput);
}