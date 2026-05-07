package com.medicare.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OpenRouterService {
    public static final String DEFAULT_MODEL = "openrouter/free";
    private static final int DEFAULT_MAX_COMPLETION_TOKENS = 520;
    private static final double DEFAULT_TEMPERATURE = 0.45d;

    private static final String CHAT_COMPLETIONS_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(40);
    private static final int MAX_RETRIES = 2;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public void validateConfiguration() {
        resolveApiKey();
    }

    public ChatCompletionResult sendMessage(String prompt) {
        return sendMessage(prompt, DEFAULT_MAX_COMPLETION_TOKENS, DEFAULT_TEMPERATURE);
    }

    public ChatCompletionResult sendMessage(String prompt, int maxCompletionTokens, double temperature) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalStateException("Le prompt du chatbot ne peut pas etre vide.");
        }
        if (maxCompletionTokens <= 0) {
            throw new IllegalArgumentException("Le nombre maximum de tokens doit etre strictement positif.");
        }
        if (Double.isNaN(temperature) || temperature < 0.0d || temperature > 2.0d) {
            throw new IllegalArgumentException("La temperature doit etre comprise entre 0.0 et 2.0.");
        }

        String apiKey = resolveApiKey();
        String payload = buildPayload(prompt, resolveModel(), maxCompletionTokens, temperature);
        int attempt = 0;

        while (true) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(CHAT_COMPLETIONS_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return extractAssistantReply(response.body());
                }

                if (isRetryableStatus(response.statusCode()) && attempt < MAX_RETRIES) {
                    sleepBeforeRetry(attempt);
                    attempt++;
                    continue;
                }

                throw new IllegalStateException(buildApiErrorMessage(response.statusCode(), response.body()));
            } catch (IOException e) {
                if (attempt < MAX_RETRIES) {
                    sleepBeforeRetry(attempt);
                    attempt++;
                    continue;
                }
                throw new IllegalStateException("Impossible de joindre l'API externe gratuite pour le moment. Verifiez votre connexion reseau et reessayez.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("La requete vers l'API externe a ete interrompue.", e);
            }
        }
    }

    private String resolveApiKey() {
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("La variable d'environnement OPENROUTER_API_KEY est introuvable. Creez une cle gratuite OpenRouter puis configurez-la avant d'utiliser l'assistant medical.");
        }
        return apiKey.trim();
    }

    private String resolveModel() {
        String configuredModel = System.getenv("OPENROUTER_MODEL");
        if (configuredModel == null || configuredModel.isBlank()) {
            return DEFAULT_MODEL;
        }
        return configuredModel.trim();
    }

    private String buildPayload(String prompt, String model, int maxCompletionTokens, double temperature) {
        return new StringBuilder()
                .append('{')
                .append("\"model\":").append(toJsonString(model)).append(',')
                .append("\"max_completion_tokens\":").append(maxCompletionTokens).append(',')
                .append("\"temperature\":").append(formatDecimal(temperature)).append(',')
                .append("\"messages\":[{")
                .append("\"role\":\"user\",")
                .append("\"content\":").append(toJsonString(prompt))
                .append("}]")
                .append('}')
                .toString();
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 409 || statusCode == 429 || statusCode >= 500;
    }

    private void sleepBeforeRetry(int attempt) {
        long waitMillis = 900L * (attempt + 1L);
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La requete vers l'API externe a ete interrompue.", e);
        }
    }

    private ChatCompletionResult extractAssistantReply(String json) {
        String resolvedModel = defaultIfBlank(extractString(json, "model"), DEFAULT_MODEL);

        int choicesIndex = json.indexOf("\"choices\"");
        if (choicesIndex >= 0) {
            int messageIndex = json.indexOf("\"message\"", choicesIndex);
            if (messageIndex >= 0) {
                String content = defaultIfBlank(extractStringAfterIndex(json, "content", messageIndex), null);
                if (content != null) {
                    return new ChatCompletionResult(content.trim(), resolvedModel);
                }
            }

            String fallbackText = defaultIfBlank(extractStringAfterIndex(json, "text", choicesIndex), null);
            if (fallbackText != null) {
                return new ChatCompletionResult(fallbackText.trim(), resolvedModel);
            }
        }

        throw new IllegalStateException("L'API externe a repondu sans texte exploitable.");
    }

    private String buildApiErrorMessage(int statusCode, String body) {
        String apiMessage = defaultIfBlank(extractString(body, "message"), "Aucun detail supplementaire fourni.");
        return switch (statusCode) {
            case 400 -> "La requete envoyee a l'API externe est invalide. " + apiMessage;
            case 401 -> "La cle OPENROUTER_API_KEY est invalide ou refusee par OpenRouter.";
            case 402 -> "Le compte OpenRouter ne peut pas traiter cette requete pour le moment. " + apiMessage;
            case 403 -> "L'acces a l'API externe est refuse pour cette cle.";
            case 404 -> "Le endpoint OpenRouter configure est introuvable.";
            case 408, 409 -> "L'API externe est momentanement surchargee ou lente. Merci de reessayer dans quelques instants.";
            case 429 -> "La limite de l'API gratuite a ete atteinte. Patientez un peu puis reessayez.";
            default -> statusCode >= 500
                    ? "L'API externe gratuite est indisponible pour le moment. Merci de reessayer plus tard."
                    : "Erreur OpenRouter HTTP " + statusCode + ". " + apiMessage;
        };
    }

    private String extractString(String json, String key) {
        return extractStringAfterIndex(json, key, 0);
    }

    private String extractStringAfterIndex(String json, String key, int fromIndex) {
        int start = findValueStart(json, key, fromIndex);
        if (start < 0) {
            return null;
        }

        int valueStart = skipWhitespace(json, start);
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }

        return parseJsonString(json, valueStart).text();
    }

    private int findValueStart(String json, String key, int fromIndex) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search, Math.max(0, fromIndex));
        if (keyIndex < 0) {
            return -1;
        }

        int colonIndex = json.indexOf(':', keyIndex + search.length());
        return colonIndex < 0 ? -1 : colonIndex + 1;
    }

    private int skipWhitespace(String value, int index) {
        int cursor = index;
        while (cursor < value.length() && Character.isWhitespace(value.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private ParsedString parseJsonString(String json, int quoteIndex) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;

        for (int i = quoteIndex + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (escaped) {
                builder.append(unescape(current, json, i));
                if (current == 'u' && i + 4 < json.length()) {
                    i += 4;
                }
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                return new ParsedString(builder.toString(), i + 1);
            }

            builder.append(current);
        }

        return new ParsedString(builder.toString(), json.length());
    }

    private String unescape(char escapedChar, String json, int currentIndex) {
        return switch (escapedChar) {
            case '"', '\\', '/' -> String.valueOf(escapedChar);
            case 'b' -> "\b";
            case 'f' -> "\f";
            case 'n' -> "\n";
            case 'r' -> "\r";
            case 't' -> "\t";
            case 'u' -> decodeUnicode(json, currentIndex);
            default -> String.valueOf(escapedChar);
        };
    }

    private String decodeUnicode(String json, int currentIndex) {
        if (currentIndex + 4 >= json.length()) {
            return "u";
        }

        String hex = json.substring(currentIndex + 1, currentIndex + 5);
        try {
            return String.valueOf((char) Integer.parseInt(hex, 16));
        } catch (NumberFormatException ignored) {
            return "u" + hex;
        }
    }

    private String toJsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                default -> {
                    if (current < 32) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        builder.append('"');
        return builder.toString();
    }

    private String formatDecimal(double value) {
        String raw = Double.toString(value);
        return raw.endsWith(".0") ? raw.substring(0, raw.length() - 2) : raw;
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    public record ChatCompletionResult(String reply, String model) {
    }

    private record ParsedString(String text, int nextIndex) {
    }
}
