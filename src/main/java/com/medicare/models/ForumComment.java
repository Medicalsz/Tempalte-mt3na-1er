package com.medicare.models;

import java.time.LocalDateTime;

public class ForumComment {
    private int id;
    private int authorId;
    private int topicId;
    private Integer parentId;
    private Integer reportedById;
    private String content;
    private LocalDateTime createdAt;
    private boolean reported;
    private boolean hidden;
    private String reportedReason;
    private LocalDateTime reportedAt;
    private String authorName;
    private String authorRoles;
    private String topicTitle;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public int getTopicId() { return topicId; }
    public void setTopicId(int topicId) { this.topicId = topicId; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public Integer getReportedById() { return reportedById; }
    public void setReportedById(Integer reportedById) { this.reportedById = reportedById; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

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

    public String getTopicTitle() { return topicTitle; }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }

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
}
