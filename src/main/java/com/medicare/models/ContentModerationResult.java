package com.medicare.models;

public class ContentModerationResult {
    private final boolean hasToxicContent;
    private final String type;
    private final String message;
    private final double score;
    private final String severity;
    private final boolean shouldAutoReport;

    public ContentModerationResult(boolean hasToxicContent,
                                   String type,
                                   String message,
                                   double score,
                                   String severity,
                                   boolean shouldAutoReport) {
        this.hasToxicContent = hasToxicContent;
        this.type = type;
        this.message = message;
        this.score = score;
        this.severity = severity;
        this.shouldAutoReport = shouldAutoReport;
    }

    public static ContentModerationResult clean() {
        return new ContentModerationResult(false, "clean", "", 0.0d, "none", false);
    }

    public boolean hasToxicContent() {
        return hasToxicContent;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public double getScore() {
        return score;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean shouldAutoReport() {
        return shouldAutoReport;
    }
}
