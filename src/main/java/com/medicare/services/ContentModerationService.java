package com.medicare.services;

import com.medicare.models.ContentModerationResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentModerationService {
    private static final String DEFAULT_ENDPOINT = "http://127.0.0.1:5000/moderate-text";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(6);

    private static final List<String> INSULT_TERMS = List.of(
            "merde", "connard", "connasse", "pute", "encule", "salope", "abruti", "idiot",
            "fuck", "shit", "bitch", "asshole", "bastard"
    );

    private static final List<String> DANGEROUS_PHRASES = List.of(
            "comment me suicider", "comment se suicider", "je vais me suicider", "envie de tuer",
            "comment tuer", "fabriquer une bombe", "attaque terroriste", "comment faire du poison",
            "comment blesser", "comment pirater un compte", "arme artisanale"
    );

    private static final List<String> SUSPICIOUS_LINK_MARKERS = List.of(
            "bit.ly", "tinyurl", "t.me", "wa.me", "whatsapp", "telegram", "discord.gg"
    );

    private static final List<String> SPAM_KEYWORDS = List.of(
            "gagnez de l'argent", "argent facile", "cliquez ici", "offre limitee",
            "promotion exclusive", "crypto", "casino", "pariez", "gratuit gratuit"
    );

    private static final Pattern LINK_PATTERN = Pattern.compile("(https?://\\S+|www\\.\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{Nd}']+");
    private static final Pattern REPEATED_PUNCTUATION_PATTERN = Pattern.compile("([!?])\\1{4,}");
    private static final Pattern REPEATED_CHAR_PATTERN = Pattern.compile("(\\p{L})\\1{5,}", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final String endpointUrl;

    public ContentModerationService() {
        this.endpointUrl = resolveEndpointUrl();
    }

    public ContentModerationResult moderate(String text) {
        if (text == null || text.isBlank()) {
            return ContentModerationResult.clean();
        }

        ContentModerationResult remote = moderateRemotely(text);
        if (remote != null) {
            return remote;
        }

        return analyzeLocally(text);
    }

    ContentModerationResult analyzeLocally(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return ContentModerationResult.clean();
        }

        if (containsWholeWord(normalized, INSULT_TERMS)) {
            return new ContentModerationResult(
                    true,
                    "insult",
                    "Votre message contient un langage inapproprie. Merci de le modifier avant publication.",
                    0.95d,
                    "high",
                    false
            );
        }

        if (containsPhrase(normalized, DANGEROUS_PHRASES)) {
            return new ContentModerationResult(
                    true,
                    "dangerous",
                    "Votre message contient un contenu non autorise.",
                    0.98d,
                    "high",
                    false
            );
        }

        SpamAnalysis spamAnalysis = analyzeSpam(normalized, text);
        if (spamAnalysis.blocked()) {
            return new ContentModerationResult(
                    true,
                    "spam",
                    "Votre contenu semble etre du spam. Veuillez reformuler votre message.",
                    spamAnalysis.score(),
                    spamAnalysis.severity(),
                    false
            );
        }

        if (spamAnalysis.shouldAutoReport()) {
            return new ContentModerationResult(
                    false,
                    "spam",
                    "",
                    spamAnalysis.score(),
                    spamAnalysis.severity(),
                    true
            );
        }

        return ContentModerationResult.clean();
    }

    private ContentModerationResult moderateRemotely(String text) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString("{\"text\":" + toJsonString(text) + "}", StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return parseModerationResponse(response.body());
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private ContentModerationResult parseModerationResponse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        Boolean hasToxic = extractBoolean(json, "hasToxicContent");
        String type = defaultIfBlank(extractString(json, "type"), "clean");
        String message = defaultIfBlank(extractString(json, "message"), "");
        double score = extractNumber(json, "score");
        String severity = defaultIfBlank(extractString(json, "severity"), "none");
        Boolean shouldAutoReport = extractBoolean(json, "shouldAutoReport");

        if (hasToxic == null) {
            return null;
        }

        return new ContentModerationResult(
                hasToxic,
                type,
                message,
                score,
                severity,
                shouldAutoReport != null && shouldAutoReport
        );
    }

    private SpamAnalysis analyzeSpam(String normalized, String original) {
        int linkCount = countLinks(original);
        boolean suspiciousLink = containsMarker(normalized, SUSPICIOUS_LINK_MARKERS);
        boolean spamKeyword = containsPhrase(normalized, SPAM_KEYWORDS);
        boolean repeatedPunctuation = REPEATED_PUNCTUATION_PATTERN.matcher(original).find();
        boolean repeatedChars = REPEATED_CHAR_PATTERN.matcher(normalized).find();
        boolean repeatedWords = hasRepeatedWordBurst(normalized);
        boolean duplicatedSentence = hasDuplicatedSentence(normalized);
        double uppercaseRatio = uppercaseRatio(original);

        if (linkCount >= 3 || repeatedWords || duplicatedSentence) {
            return new SpamAnalysis(true, false, 0.88d, "high");
        }

        if ((linkCount >= 2 && (spamKeyword || suspiciousLink)) || (uppercaseRatio > 0.55d && repeatedPunctuation)) {
            return new SpamAnalysis(true, false, 0.82d, "medium");
        }

        if ((linkCount == 1 && suspiciousLink) || spamKeyword || repeatedChars || repeatedPunctuation || uppercaseRatio > 0.45d) {
            return new SpamAnalysis(false, true, 0.56d, "low");
        }

        return new SpamAnalysis(false, false, 0.0d, "none");
    }

    private int countLinks(String text) {
        int count = 0;
        Matcher matcher = LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private boolean hasRepeatedWordBurst(String normalized) {
        Map<String, Integer> counts = new HashMap<>();
        List<String> tokens = extractTokens(normalized);
        String previous = null;
        int streak = 1;

        for (String token : tokens) {
            counts.merge(token, 1, Integer::sum);

            if (token.equals(previous)) {
                streak++;
                if (streak >= 4) {
                    return true;
                }
            } else {
                streak = 1;
                previous = token;
            }
        }

        int tokenCount = tokens.size();
        if (tokenCount < 8) {
            return false;
        }

        for (int count : counts.values()) {
            if (count >= 5 && ((double) count / (double) tokenCount) >= 0.35d) {
                return true;
            }
        }

        return false;
    }

    private boolean hasDuplicatedSentence(String normalized) {
        String[] parts = normalized.split("[.!?\\n]+");
        Map<String, Integer> counts = new HashMap<>();
        for (String part : parts) {
            String sentence = part.trim();
            if (sentence.length() < 12) {
                continue;
            }
            int count = counts.merge(sentence, 1, Integer::sum);
            if (count >= 2) {
                return true;
            }
        }
        return false;
    }

    private double uppercaseRatio(String text) {
        int letters = 0;
        int uppercase = 0;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isLetter(current)) {
                letters++;
                if (Character.isUpperCase(current)) {
                    uppercase++;
                }
            }
        }
        if (letters == 0) {
            return 0.0d;
        }
        return (double) uppercase / (double) letters;
    }

    private boolean containsWholeWord(String normalized, List<String> terms) {
        Set<String> tokens = new HashSet<>(extractTokens(normalized));
        for (String term : terms) {
            if (tokens.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPhrase(String normalized, List<String> phrases) {
        for (String phrase : phrases) {
            if (normalized.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsMarker(String normalized, List<String> markers) {
        for (String marker : markers) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractTokens(String normalized) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.length() >= 3) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private String resolveEndpointUrl() {
        String systemProperty = System.getProperty("medicare.contentModerationUrl");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty.trim();
        }

        String environment = System.getenv("MEDICARE_CONTENT_MODERATION_URL");
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }

        return DEFAULT_ENDPOINT;
    }

    private String toJsonString(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        builder.append('"');
        return builder.toString();
    }

    private Boolean extractBoolean(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private double extractNumber(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        if (!matcher.find()) {
            return 0.0d;
        }
        return Double.parseDouble(matcher.group(1));
    }

    private String extractString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private record SpamAnalysis(boolean blocked, boolean shouldAutoReport, double score, String severity) {
    }
}
