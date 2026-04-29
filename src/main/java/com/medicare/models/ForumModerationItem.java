package com.medicare.models;

import java.time.LocalDateTime;

public class ForumModerationItem {
    private final String typeLabel;
    private final String contentLabel;
    private final String authorLabel;
    private final String statusLabel;
    private final String dateLabel;
    private final LocalDateTime sortDate;

    public ForumModerationItem(String typeLabel,
                               String contentLabel,
                               String authorLabel,
                               String statusLabel,
                               String dateLabel,
                               LocalDateTime sortDate) {
        this.typeLabel = typeLabel;
        this.contentLabel = contentLabel;
        this.authorLabel = authorLabel;
        this.statusLabel = statusLabel;
        this.dateLabel = dateLabel;
        this.sortDate = sortDate;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getContentLabel() {
        return contentLabel;
    }

    public String getAuthorLabel() {
        return authorLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public LocalDateTime getSortDate() {
        return sortDate;
    }
}
