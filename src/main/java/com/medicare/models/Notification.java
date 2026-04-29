package com.medicare.models;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int userId;
    private Integer actorId;
    private String actorName;
    private String title;
    private String message;
    private String type;
    private Integer relatedTopicId;
    private Integer relatedCommentId;
    private boolean read;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getActorId() { return actorId; }
    public void setActorId(Integer actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getRelatedTopicId() { return relatedTopicId; }
    public void setRelatedTopicId(Integer relatedTopicId) { this.relatedTopicId = relatedTopicId; }

    public Integer getRelatedCommentId() { return relatedCommentId; }
    public void setRelatedCommentId(Integer relatedCommentId) { this.relatedCommentId = relatedCommentId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
