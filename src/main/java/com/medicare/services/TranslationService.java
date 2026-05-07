package com.medicare.services;

import com.medicare.models.ForumComment;
import com.medicare.models.ForumTopic;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class TranslationService {
    static final int MAX_TEXT_LENGTH = 3200;
    private static final int TRANSLATION_MAX_TOKENS = 900;
    private static final double TRANSLATION_TEMPERATURE = 0.2d;

    private static final SupportedLanguage FRENCH = new SupportedLanguage(
            "fr",
            "\uD83C\uDDEB\uD83C\uDDF7 Fran\u00e7ais",
            "French",
            false
    );
    private static final SupportedLanguage ENGLISH = new SupportedLanguage(
            "en",
            "\uD83C\uDDEC\uD83C\uDDE7 English",
            "English",
            false
    );
    private static final SupportedLanguage ARABIC = new SupportedLanguage(
            "ar",
            "\uD83C\uDDF8\uD83C\uDDE6 \u0627\u0644\u0639\u0631\u0628\u064a\u0629",
            "Arabic",
            true
    );

    private final OpenRouterService openRouterService = new OpenRouterService();

    public String translateText(String text, String targetLanguage) {
        SupportedLanguage language = resolveSupportedLanguage(targetLanguage);
        PreparedText preparedText = prepareTextForTranslation(text);
        ensureApiConfigured();
        return requestTranslation(preparedText, language);
    }

    public TopicTranslation translateTopic(ForumTopic topic, String targetLanguage) {
        if (topic == null) {
            throw new IllegalStateException("Le sujet a traduire est introuvable.");
        }

        SupportedLanguage language = resolveSupportedLanguage(targetLanguage);
        PreparedText title = prepareOptionalText(topic.getTitle());
        PreparedText summary = prepareOptionalText(topic.getSummary());
        PreparedText content = prepareOptionalText(topic.getContent());
        if (title.isEmpty() && summary.isEmpty() && content.isEmpty()) {
            throw new IllegalStateException("Le sujet ne contient aucun texte a traduire.");
        }

        ensureApiConfigured();
        String translatedTitle = translatePreparedText(title, language);
        String translatedSummary = translatePreparedText(summary, language);
        String translatedContent = translatePreparedText(content, language);

        return new TopicTranslation(
                language.code(),
                language.displayLabel(),
                language.rightToLeft(),
                translatedTitle,
                translatedSummary,
                translatedContent,
                title.truncated() || summary.truncated() || content.truncated()
        );
    }

    public CommentTranslation translateComment(ForumComment comment, String targetLanguage) {
        if (comment == null) {
            throw new IllegalStateException("Le commentaire a traduire est introuvable.");
        }

        SupportedLanguage language = resolveSupportedLanguage(targetLanguage);
        PreparedText content = prepareTextForTranslation(comment.getContent());
        ensureApiConfigured();

        return new CommentTranslation(
                language.code(),
                language.displayLabel(),
                language.rightToLeft(),
                requestTranslation(content, language),
                content.truncated()
        );
    }

    SupportedLanguage resolveSupportedLanguage(String targetLanguage) {
        if (targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalStateException("Veuillez choisir une langue de traduction.");
        }

        String normalized = normalizeLanguage(targetLanguage);
        return switch (normalized) {
            case "fr", "francais", "french" -> FRENCH;
            case "en", "english", "anglais" -> ENGLISH;
            case "ar", "arabic", "\u0627\u0644\u0639\u0631\u0628\u064a\u0629" -> ARABIC;
            default -> throw new IllegalStateException("La langue de traduction demandee n'est pas prise en charge.");
        };
    }

    PreparedText prepareTextForTranslation(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Le texte a traduire ne peut pas etre vide.");
        }
        return prepareOptionalText(text);
    }

    PreparedText prepareOptionalText(String text) {
        if (text == null || text.isBlank()) {
            return PreparedText.empty();
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.length() <= MAX_TEXT_LENGTH) {
            return new PreparedText(normalized, false);
        }

        int cutIndex = MAX_TEXT_LENGTH;
        while (cutIndex > MAX_TEXT_LENGTH - 120 && cutIndex > 0 && !Character.isWhitespace(normalized.charAt(cutIndex - 1))) {
            cutIndex--;
        }
        if (cutIndex <= 0) {
            cutIndex = MAX_TEXT_LENGTH;
        }

        String truncated = normalized.substring(0, cutIndex).trim();
        if (!truncated.endsWith("...")) {
            truncated += "...";
        }
        return new PreparedText(truncated, true);
    }

    private String translatePreparedText(PreparedText preparedText, SupportedLanguage language) {
        if (preparedText.isEmpty()) {
            return "";
        }
        return requestTranslation(preparedText, language);
    }

    private String requestTranslation(PreparedText preparedText, SupportedLanguage language) {
        String prompt = buildTranslationPrompt(preparedText.text(), language.promptLanguage());
        OpenRouterService.ChatCompletionResult apiResult = openRouterService.sendMessage(
                prompt,
                TRANSLATION_MAX_TOKENS,
                TRANSLATION_TEMPERATURE
        );

        String translatedText = sanitizeTranslatedText(apiResult.reply());
        if (translatedText.isBlank()) {
            throw new IllegalStateException("L'API de traduction n'a retourne aucun texte exploitable.");
        }
        return translatedText;
    }

    private String buildTranslationPrompt(String text, String targetLanguage) {
        return "Translate the following medical forum text to " + targetLanguage + ". "
                + "Keep the meaning clear, simple and respectful. "
                + "Preserve useful line breaks when relevant. "
                + "Return only the translated text.\n\n"
                + "Text:\n"
                + text;
    }

    private String sanitizeTranslatedText(String text) {
        if (text == null) {
            return "";
        }

        String sanitized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (sanitized.startsWith("```")) {
            sanitized = sanitized.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "");
            sanitized = sanitized.replaceFirst("\\s*```$", "");
        }

        if ((sanitized.startsWith("\"") && sanitized.endsWith("\""))
                || (sanitized.startsWith("'") && sanitized.endsWith("'"))) {
            sanitized = sanitized.substring(1, sanitized.length() - 1).trim();
        }

        return sanitized.trim();
    }

    private void ensureApiConfigured() {
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("La variable d'environnement OPENROUTER_API_KEY est introuvable. Configurez-la avant d'utiliser la traduction.");
        }
    }

    private String normalizeLanguage(String targetLanguage) {
        String withoutAccents = Normalizer.normalize(targetLanguage, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public List<SupportedLanguage> supportedLanguages() {
        return List.of(FRENCH, ENGLISH, ARABIC);
    }

    public record SupportedLanguage(String code, String displayLabel, String promptLanguage, boolean rightToLeft) {
        @Override
        public String toString() {
            return displayLabel;
        }
    }

    public record PreparedText(String text, boolean truncated) {
        static PreparedText empty() {
            return new PreparedText("", false);
        }

        boolean isEmpty() {
            return text == null || text.isBlank();
        }
    }

    public record TopicTranslation(
            String languageCode,
            String languageLabel,
            boolean rightToLeft,
            String translatedTitle,
            String translatedSummary,
            String translatedContent,
            boolean partialTranslation
    ) {
    }

    public record CommentTranslation(
            String languageCode,
            String languageLabel,
            boolean rightToLeft,
            String translatedContent,
            boolean partialTranslation
    ) {
    }
}
