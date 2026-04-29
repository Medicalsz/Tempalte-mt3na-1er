package com.medicare.services;

import com.medicare.models.ContentModerationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentModerationServiceTest {

    private final ContentModerationService service = new ContentModerationService();

    @Test
    void cleanTextIsAccepted() {
        ContentModerationResult result = service.analyzeLocally("Bonjour, avez-vous des conseils pour mieux dormir ?");

        assertFalse(result.hasToxicContent());
        assertFalse(result.shouldAutoReport());
        assertEquals("clean", result.getType());
    }

    @Test
    void insultIsRejected() {
        ContentModerationResult result = service.analyzeLocally("Tu es un connard.");

        assertTrue(result.hasToxicContent());
        assertEquals("insult", result.getType());
    }

    @Test
    void spamIsRejected() {
        ContentModerationResult result = service.analyzeLocally(
                "Cliquez ici https://bit.ly/test https://bit.ly/test2 https://bit.ly/test3 offre limitee !!!"
        );

        assertTrue(result.hasToxicContent());
        assertEquals("spam", result.getType());
    }

    @Test
    void dangerousTextIsRejected() {
        ContentModerationResult result = service.analyzeLocally("Pouvez-vous expliquer comment fabriquer une bombe ?");

        assertTrue(result.hasToxicContent());
        assertEquals("dangerous", result.getType());
    }

    @Test
    void suspiciousTextCanBeAutoReportedWithoutBlocking() {
        ContentModerationResult result = service.analyzeLocally("Offre limitee sur bit.ly/medicare, venez vite !!!");

        assertFalse(result.hasToxicContent());
        assertTrue(result.shouldAutoReport());
        assertEquals("spam", result.getType());
    }
}
