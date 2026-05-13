package com.medicare.models;

import java.time.LocalDateTime;

public class ForumTopic {
    private int id;
    private int authorId;
    private Integer reportedById;
    private String title;
    private String content;
    private String type;
    private String videoUrl;
    private String summary;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean reported;
    private boolean hidden;
    private String reportedReason;
    private LocalDateTime reportedAt;
    private String authorName;
    private String authorRoles;
    private int commentCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public Integer getReportedById() { return reportedById; }
    public void setReportedById(Integer reportedById) { this.reportedById = reportedById; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isReported() { return reported; }
    public void setReported(boolean reported) { this.reported = reported; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public String getReportedReason() { return reportedReason; }
    public void setReportedReason(String reportedReason) { this.reportedReason = reportedReason; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRoles() { return authorRoles; }
    public void setAuthorRoles(String authorRoles) { this.authorRoles = authorRoles; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean hasModerationFlag() {
        return reported || hidden;
    }

    public String getModerationSummary() {
        if (!hasModerationFlag()) {
            return "";
        }

        if (reported && hidden) {
            return "Signale - Masque";
        }
        if (reported) {
            return "Signale";
        }
        return "Masque";
    }

    public String getDisplayType() {
        return "video".equalsIgnoreCase(type) ? "Video" : "Article";
    }

    public boolean isVideo() {
        return "video".equalsIgnoreCase(type);
    }

    public String getDisplaySummary() {
        if (summary != null && !summary.isBlank()) {
            return summary.trim();
        }

        if (content == null || content.isBlank()) {
            return "";
        }

        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() <= 180 ? compact : compact.substring(0, 177) + "...";
    }

    public String getTagsDisplay() {
        if (tags == null || tags.isBlank()) {
            return "";
        }

        String normalized = tags.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace("\",\"", ", ");
        normalized = normalized.replace("\", \"", ", ");
        normalized = normalized.replace("\"", "");
        normalized = decodeUnicodeEscapes(normalized);
        return normalized.trim();
    }

    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String lower = query.toLowerCase();
        return contains(title, lower)
                || contains(content, lower)
                || contains(summary, lower)
                || contains(getTagsDisplay(), lower)
                || contains(authorName, lower)
                || contains(getDisplayType(), lower);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String decodeUnicodeEscapes(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\\' && i + 5 < value.length() && value.charAt(i + 1) == 'u') {
                String hex = value.substring(i + 2, i + 6);
                try {
                    builder.append((char) Integer.parseInt(hex, 16));
                    i += 5;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            builder.append(current);
        }
        return builder.toString();
    }
}
