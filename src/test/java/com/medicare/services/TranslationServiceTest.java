package com.medicare.services;

import com.medicare.models.ForumComment;
import com.medicare.models.ForumTopic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationServiceTest {

    private final TranslationService service = new TranslationService();

    @Test
    void resolveSupportedLanguageAcceptsUiLabels() {
        assertEquals("fr", service.resolveSupportedLanguage("Francais").code());
        assertEquals("fr", service.resolveSupportedLanguage("\uD83C\uDDEB\uD83C\uDDF7 Fran\u00e7ais").code());
        assertEquals("en", service.resolveSupportedLanguage("English").code());
        assertEquals("en", service.resolveSupportedLanguage("\uD83C\uDDEC\uD83C\uDDE7 English").code());
        assertEquals("ar", service.resolveSupportedLanguage("\u0627\u0644\u0639\u0631\u0628\u064a\u0629").code());
        assertEquals("ar", service.resolveSupportedLanguage("\uD83C\uDDF8\uD83C\uDDE6 \u0627\u0644\u0639\u0631\u0628\u064a\u0629").code());
    }

    @Test
    void prepareTextForTranslationTruncatesLongInput() {
        String text = "a".repeat(TranslationService.MAX_TEXT_LENGTH + 250);

        TranslationService.PreparedText preparedText = service.prepareTextForTranslation(text);

        assertTrue(preparedText.truncated());
        assertTrue(preparedText.text().length() <= TranslationService.MAX_TEXT_LENGTH + 3);
    }

    @Test
    void translateTextRejectsBlankInput() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.translateText("   ", "English")
        );

        assertEquals("Le texte a traduire ne peut pas etre vide.", exception.getMessage());
    }

    @Test
    void translateTopicRejectsEmptyTopic() {
        ForumTopic topic = new ForumTopic();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.translateTopic(topic, "English")
        );

        assertEquals("Le sujet ne contient aucun texte a traduire.", exception.getMessage());
    }

    @Test
    void translateCommentRejectsEmptyComment() {
        ForumComment comment = new ForumComment();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.translateComment(comment, "English")
        );

        assertEquals("Le texte a traduire ne peut pas etre vide.", exception.getMessage());
    }
}
