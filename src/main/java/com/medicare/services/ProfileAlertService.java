package com.medicare.services;

import com.medicare.models.User;

/**
 * Smart alert engine for profile completion.
 * 5 levels: CRITICAL → HIGH → MEDIUM → LOW → NONE.
 * Called on every login and after each dismiss.
 */
public class ProfileAlertService {

    // --- Weight of each section (must sum to 100) ---
    public static int getCompletionPercent(User user) {
        int score = 0;
        if (user.getNom()    != null && !user.getNom().isEmpty())    score += 17;
        if (user.getBloodType() != null)                             score += 17;
        if (user.getCity()   != null && !user.getCity().isEmpty())   score += 17;
        if (user.getNumero() != null && !user.getNumero().isEmpty()) score += 17;
        if (user.getPhoto()  != null && !user.getPhoto().isEmpty())  score += 16;
        if (user.isWantsEmailNotifications())                        score += 16;
        return score;
    }

    public enum AlertLevel { CRITICAL, HIGH, MEDIUM, LOW, NONE }

    public record SmartAlert(AlertLevel level, String title, String message, String color) {}

    // Called on every login and after each dismiss
    public static SmartAlert getSmartAlert(User user) {
        int pct = getCompletionPercent(user);

        if (pct == 0) return new SmartAlert(AlertLevel.NONE, null, null, null);

        if (pct < 30)
            return new SmartAlert(AlertLevel.CRITICAL,
                "Profile incomplete — features locked",
                "Complete at least 3 sections to unlock appointment booking.",
                "#FFEBEE");  // red bg

        if (pct < 50 && user.getBloodType() == null)
            return new SmartAlert(AlertLevel.HIGH,
                "Missing medical info",
                "Without blood type and allergies, doctors can't prepare for emergencies.",
                "#FFF3E0");  // orange bg

        if (pct < 50 && (user.getCity() == null || user.getCity().isEmpty()))
            return new SmartAlert(AlertLevel.HIGH,
                "No location set",
                "The nearest doctor finder needs your city. Takes 10 seconds.",
                "#FFF3E0");  // orange bg

        if (pct < 50)
            return new SmartAlert(AlertLevel.MEDIUM,
                "Profile below 50%",
                "You're almost there — complete one more section to unlock all features.",
                "#FCE4EC");  // pink bg

        if (!user.isWantsEmailNotifications())
            return new SmartAlert(AlertLevel.LOW,
                "Notifications are off",
                "Enable reminders so you never miss an appointment or medication.",
                "#FFF3E0");  // orange bg

        return new SmartAlert(AlertLevel.NONE, null, null, null);
    }
}
