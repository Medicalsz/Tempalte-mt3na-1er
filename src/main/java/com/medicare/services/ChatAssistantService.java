package com.medicare.services;

import com.medicare.models.ChatAssistantRecommendation;
import com.medicare.models.ChatAssistantResponse;
import com.medicare.models.ForumComment;
import com.medicare.models.ForumTopic;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ChatAssistantService {
    private static final String DEFAULT_ENDPOINT = "http://127.0.0.1:5000/chat-assistant";
    private static final String DEFAULT_PYTHON_COMMAND = "python";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration STARTUP_WAIT = Duration.ofSeconds(12);
    private static final Object START_LOCK = new Object();

    private static Process assistantProcess;
    private static long lastStartAttemptAt;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final String endpointUrl;
    private final String healthUrl;
    private final String pythonCommand;
    private final Path assistantDirectory;
    private final Path runtimeOutLog;
    private final Path runtimeErrLog;

    public ChatAssistantService() {
        this.endpointUrl = resolveEndpointUrl();
        this.healthUrl = resolveHealthUrl();
        this.pythonCommand = resolvePythonCommand();
        this.assistantDirectory = resolveAssistantDirectory();
        this.runtimeOutLog = resolveRuntimeLogPath("chat-assistant-runtime.out.log");
        this.runtimeErrLog = resolveRuntimeLogPath("chat-assistant-runtime.err.log");
    }

    public void warmUp() {
        ensureAssistantAvailable();
    }

    public ChatAssistantResponse askAssistant(String message,
                                             ForumTopic topic,
                                             List<ForumComment> comments,
                                             List<ForumTopic> relatedTopics) {
        if (message == null || message.isBlank()) {
            throw new IllegalStateException("Le message du chatbot ne peut pas etre vide.");
        }
        if (topic == null) {
            throw new IllegalStateException("Le sujet du forum est introuvable.");
        }

        ensureAssistantAvailable();
        String payload = buildPayload(message, topic, comments, relatedTopics);
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Le service chatbot a repondu avec le statut HTTP " + response.statusCode() + ".");
            }
            return parseResponse(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Le service chatbot local est indisponible. Verifiez que Flask est demarre sur " + endpointUrl + ".", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La requete vers le chatbot a ete interrompue.", e);
        }
    }

    private String resolveEndpointUrl() {
        String systemProperty = System.getProperty("medicare.chatAssistantUrl");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty.trim();
        }

        String environment = System.getenv("MEDICARE_CHAT_ASSISTANT_URL");
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }

        return DEFAULT_ENDPOINT;
    }

    private String resolveHealthUrl() {
        URI endpoint = URI.create(endpointUrl);
        return new StringBuilder()
                .append(endpoint.getScheme() != null ? endpoint.getScheme() : "http")
                .append("://")
                .append(endpoint.getHost() != null ? endpoint.getHost() : "127.0.0.1")
                .append(endpoint.getPort() > 0 ? ":" + endpoint.getPort() : "")
                .append("/health")
                .toString();
    }

    private String resolvePythonCommand() {
        String systemProperty = System.getProperty("medicare.chatAssistantPython");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty.trim();
        }

        String environment = System.getenv("MEDICARE_CHAT_ASSISTANT_PYTHON");
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }

        return DEFAULT_PYTHON_COMMAND;
    }

    private Path resolveAssistantDirectory() {
        String systemProperty = System.getProperty("medicare.chatAssistantDir");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return Path.of(systemProperty.trim()).toAbsolutePath().normalize();
        }

        String environment = System.getenv("MEDICARE_CHAT_ASSISTANT_DIR");
        if (environment != null && !environment.isBlank()) {
            return Path.of(environment.trim()).toAbsolutePath().normalize();
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                workingDirectory.resolve("ai_summarizer"),
                workingDirectory.resolveSibling("Medicare - Copie").resolve("Medicare").resolve("ai_summarizer"),
                workingDirectory.resolveSibling("Medicare").resolve("ai_summarizer")
        );

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("app.py"))) {
                return candidate;
            }
        }

        return candidates.get(1);
    }

    private Path resolveRuntimeLogPath(String fileName) {
        Path workingDirectory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        return workingDirectory.resolve(fileName);
    }

    private void ensureAssistantAvailable() {
        if (isAssistantReachable()) {
            return;
        }

        attemptLocalAssistantStart();
        waitForAssistantStartup();
    }

    private boolean isAssistantReachable() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(healthUrl))
                .timeout(HEALTH_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void attemptLocalAssistantStart() {
        if (!usesLocalAssistantEndpoint()) {
            return;
        }

        synchronized (START_LOCK) {
            if (isAssistantReachable()) {
                return;
            }
            if (assistantProcess != null && assistantProcess.isAlive()) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastStartAttemptAt < 1500) {
                return;
            }
            lastStartAttemptAt = now;

            if (!Files.isDirectory(assistantDirectory) || !Files.exists(assistantDirectory.resolve("app.py"))) {
                return;
            }

            try {
                Files.createDirectories(runtimeOutLog.getParent());

                ProcessBuilder builder = new ProcessBuilder(pythonCommand, "app.py");
                builder.directory(assistantDirectory.toFile());
                builder.environment().put("PYTHONIOENCODING", "utf-8");
                builder.redirectOutput(ProcessBuilder.Redirect.appendTo(runtimeOutLog.toFile()));
                builder.redirectError(ProcessBuilder.Redirect.appendTo(runtimeErrLog.toFile()));
                assistantProcess = builder.start();
            } catch (IOException ignored) {
                assistantProcess = null;
            }
        }
    }

    private void waitForAssistantStartup() {
        long deadline = System.nanoTime() + STARTUP_WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            if (isAssistantReachable()) {
                return;
            }

            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("La requete vers le chatbot a ete interrompue.", e);
            }
        }

        throw new IllegalStateException(buildUnavailableMessage());
    }

    private boolean usesLocalAssistantEndpoint() {
        URI endpoint = URI.create(endpointUrl);
        String host = endpoint.getHost();
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host);
    }

    private String buildUnavailableMessage() {
        StringBuilder builder = new StringBuilder("Le service chatbot local est indisponible. Verifiez que Flask est demarre sur ")
                .append(endpointUrl)
                .append(".");

        if (usesLocalAssistantEndpoint()) {
            builder.append(" Demarrage automatique tente");
            if (assistantDirectory != null) {
                builder.append(" depuis ").append(assistantDirectory);
            }
            builder.append(".");
        }

        return builder.toString();
    }

    private String buildPayload(String message,
                                ForumTopic topic,
                                List<ForumComment> comments,
                                List<ForumTopic> relatedTopics) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"message\":").append(toJsonString(message.trim())).append(',');
        builder.append("\"topic\":").append(buildTopicJson(topic, comments)).append(',');
        builder.append("\"related_topics\":").append(buildRelatedTopicsJson(relatedTopics));
        builder.append('}');
        return builder.toString();
    }

    private String buildTopicJson(ForumTopic topic, List<ForumComment> comments) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"id\":").append(topic.getId()).append(',');
        builder.append("\"title\":").append(toJsonString(topic.getTitle())).append(',');
        builder.append("\"content\":").append(toJsonString(topic.getContent())).append(',');
        builder.append("\"summary\":").append(toJsonString(topic.getSummary())).append(',');
        builder.append("\"tags\":").append(buildTagsJson(topic)).append(',');
        builder.append("\"comments\":").append(buildCommentsJson(comments));
        builder.append('}');
        return builder.toString();
    }

    private String buildTagsJson(ForumTopic topic) {
        List<String> tags = new ArrayList<>();
        String tagsDisplay = topic.getTagsDisplay();
        if (!tagsDisplay.isBlank()) {
            for (String rawTag : tagsDisplay.split(",")) {
                String cleaned = rawTag.trim();
                if (!cleaned.isEmpty()) {
                    tags.add(toJsonString(cleaned.replace("#", "")));
                }
            }
        }
        return '[' + String.join(",", tags) + ']';
    }

    private String buildCommentsJson(List<ForumComment> comments) {
        List<String> values = new ArrayList<>();
        if (comments != null) {
            for (ForumComment comment : comments) {
                if (comment.getContent() != null && !comment.getContent().isBlank()) {
                    values.add(toJsonString(comment.getContent()));
                }
            }
        }
        return '[' + String.join(",", values) + ']';
    }

    private String buildRelatedTopicsJson(List<ForumTopic> relatedTopics) {
        List<String> values = new ArrayList<>();
        if (relatedTopics != null) {
            for (ForumTopic relatedTopic : relatedTopics) {
                StringBuilder topicJson = new StringBuilder();
                topicJson.append('{');
                topicJson.append("\"id\":").append(relatedTopic.getId()).append(',');
                topicJson.append("\"title\":").append(toJsonString(relatedTopic.getTitle())).append(',');
                topicJson.append("\"content\":").append(toJsonString(relatedTopic.getContent()));
                topicJson.append('}');
                values.add(topicJson.toString());
            }
        }
        return '[' + String.join(",", values) + ']';
    }

    private ChatAssistantResponse parseResponse(String json) {
        ChatAssistantResponse response = new ChatAssistantResponse();
        response.setReply(defaultIfBlank(extractString(json, "reply"), "Je peux vous aider a analyser ce sujet, mais je n'ai pas recu de reponse exploitable."));
        response.setIntent(defaultIfBlank(extractString(json, "intent"), "unknown"));
        response.setConfidence(extractNumber(json, "confidence"));
        response.setRecommendations(extractRecommendations(json));
        return response;
    }

    private List<ChatAssistantRecommendation> extractRecommendations(String json) {
        String rawArray = extractRawValue(json, "recommendations");
        if (rawArray == null || rawArray.isBlank() || "null".equals(rawArray)) {
            return List.of();
        }

        List<ChatAssistantRecommendation> recommendations = new ArrayList<>();
        int index = 0;
        while (index < rawArray.length()) {
            char current = rawArray.charAt(index);
            if (current == '{') {
                BalancedValue objectValue = readBalanced(rawArray, index, '{', '}');
                String objectJson = objectValue.text();
                String title = extractString(objectJson, "title");
                int id = (int) extractNumber(objectJson, "id");
                if (title != null && !title.isBlank()) {
                    recommendations.add(new ChatAssistantRecommendation(id, title));
                }
                index = objectValue.nextIndex();
                continue;
            }
            index++;
        }
        return recommendations;
    }

    private String extractString(String json, String key) {
        int start = findValueStart(json, key);
        if (start < 0) {
            return null;
        }

        int valueStart = skipWhitespace(json, start);
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }

        return parseJsonString(json, valueStart).text();
    }

    private double extractNumber(String json, String key) {
        int start = findValueStart(json, key);
        if (start < 0) {
            return 0.0d;
        }

        int index = skipWhitespace(json, start);
        int end = index;
        while (end < json.length()) {
            char current = json.charAt(end);
            if ((current >= '0' && current <= '9') || current == '-' || current == '+'
                    || current == '.' || current == 'e' || current == 'E') {
                end++;
                continue;
            }
            break;
        }

        if (end == index) {
            return 0.0d;
        }

        try {
            return Double.parseDouble(json.substring(index, end));
        } catch (NumberFormatException ignored) {
            return 0.0d;
        }
    }

    private String extractRawValue(String json, String key) {
        int start = findValueStart(json, key);
        if (start < 0) {
            return null;
        }

        int index = skipWhitespace(json, start);
        if (index >= json.length()) {
            return null;
        }

        char current = json.charAt(index);
        if (current == '[') {
            return readBalanced(json, index, '[', ']').text();
        }
        if (current == '{') {
            return readBalanced(json, index, '{', '}').text();
        }
        if (current == '"') {
            return parseJsonString(json, index).text();
        }

        int end = index;
        while (end < json.length() && ",}]".indexOf(json.charAt(end)) == -1) {
            end++;
        }
        return json.substring(index, end).trim();
    }

    private int findValueStart(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
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

    private BalancedValue readBalanced(String json, int start, char opening, char closing) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {
            char current = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (current == '\\') {
                    escaped = true;
                    continue;
                }
                if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == opening) {
                depth++;
            } else if (current == closing) {
                depth--;
                if (depth == 0) {
                    return new BalancedValue(json.substring(start, i + 1), i + 1);
                }
            }
        }

        return new BalancedValue(json.substring(start), json.length());
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

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private record ParsedString(String text, int nextIndex) {
    }

    private record BalancedValue(String text, int nextIndex) {
    }
}
