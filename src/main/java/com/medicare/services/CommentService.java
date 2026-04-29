package com.medicare.services;

import com.medicare.models.ForumComment;
import com.medicare.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentService {
    private final NotificationService notificationService = new NotificationService();

    public List<ForumComment> findByTopicId(int topicId) {
        return findByTopicId(topicId, false);
    }

    public List<ForumComment> findByTopicId(int topicId, boolean includeHidden) {
        List<ForumComment> comments = new ArrayList<>();
        String query = includeHidden ? """
                SELECT c.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       t.title AS topic_title
                FROM forum_comment c
                LEFT JOIN user u ON u.id = c.author_id
                LEFT JOIN forum_topic t ON t.id = c.topic_id
                WHERE c.topic_id = ?
                ORDER BY c.created_at ASC
                """ : """
                SELECT c.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       t.title AS topic_title
                FROM forum_comment c
                LEFT JOIN user u ON u.id = c.author_id
                LEFT JOIN forum_topic t ON t.id = c.topic_id
                WHERE c.topic_id = ? AND c.is_hidden = 0
                ORDER BY c.created_at ASC
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapComment(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement des commentaires: " + e.getMessage(), e);
        }

        return comments;
    }

    public List<ForumComment> findModeratedComments(boolean reportedOnly, boolean hiddenOnly) {
        List<ForumComment> comments = new ArrayList<>();
        String query = buildModeratedCommentsQuery(reportedOnly, hiddenOnly);

        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comments.add(mapComment(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement moderation commentaires: " + e.getMessage(), e);
        }

        return comments;
    }

    public int countComments() {
        return countByQuery("SELECT COUNT(*) FROM forum_comment");
    }

    public int countReportedComments() {
        return countByQuery("SELECT COUNT(*) FROM forum_comment WHERE is_reported = 1");
    }

    public List<ForumComment> findRecentModeratedComments(int limit) {
        List<ForumComment> comments = new ArrayList<>();
        String query = """
                SELECT c.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       t.title AS topic_title
                FROM forum_comment c
                LEFT JOIN user u ON u.id = c.author_id
                LEFT JOIN forum_topic t ON t.id = c.topic_id
                WHERE c.is_reported = 1 OR c.is_hidden = 1
                ORDER BY COALESCE(c.reported_at, c.created_at) DESC, c.id DESC
                LIMIT ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, Math.max(limit, 1));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapComment(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement moderation recente commentaires: " + e.getMessage(), e);
        }

        return comments;
    }

    public void setCommentReported(int id, boolean reported, Integer reportedById) {
        String query = """
                UPDATE forum_comment
                SET is_reported = ?, reported_by_id = ?, reported_reason = ?, reported_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setBoolean(1, reported);
            if (reported && reportedById != null) {
                ps.setInt(2, reportedById);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (reported) {
                ps.setString(3, "Moderation JavaFX");
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                ps.setNull(3, Types.VARCHAR);
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur signalement commentaire: " + e.getMessage(), e);
        }
    }

    public void setCommentHidden(int id, boolean hidden) {
        String query = """
                UPDATE forum_comment
                SET is_hidden = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setBoolean(1, hidden);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur moderation commentaire: " + e.getMessage(), e);
        }
    }

    public void addComment(ForumComment comment) {
        String query = """
                INSERT INTO forum_comment (
                    author_id, topic_id, parent_id, reported_by_id, content,
                    created_at, is_reported, is_hidden, reported_reason, reported_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Connection connection = null;

        try {
            connection = getConnection();
            try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, comment.getAuthorId());
            ps.setInt(2, comment.getTopicId());
            if (comment.getParentId() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, comment.getParentId());
            }
            if (comment.getReportedById() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, comment.getReportedById());
            }
            ps.setString(5, required(comment.getContent()));
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBoolean(7, comment.isReported());
            ps.setBoolean(8, comment.isHidden());
            ps.setString(9, trimToNull(comment.getReportedReason()));
            if (comment.getReportedAt() != null) {
                ps.setTimestamp(10, Timestamp.valueOf(comment.getReportedAt()));
            } else {
                ps.setNull(10, Types.TIMESTAMP);
            }
            ps.executeUpdate();
            comment.setId(fetchLastInsertId(connection));
            }
            notifyNewComment(comment);
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur ajout commentaire: " + e.getMessage(), e);
        }
    }

    private void notifyNewComment(ForumComment comment) {
        if (comment == null || comment.getAuthorId() <= 0 || comment.getTopicId() <= 0 || comment.getId() <= 0) {
            return;
        }

        try {
            String actorName = notificationService.findUserDisplayName(comment.getAuthorId());
            String topicTitle = notificationService.findTopicTitle(comment.getTopicId());
            notificationService.createNotificationForAllUsers(
                    comment.getAuthorId(),
                    "Nouveau commentaire forum",
                    actorName + " a ajoute un commentaire sur : " + topicTitle,
                    NotificationService.TYPE_NEW_COMMENT,
                    comment.getTopicId(),
                    comment.getId()
            );
        } catch (Exception e) {
            System.err.println("Notification nouveau commentaire non creee pour commentId=" + comment.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addReply(int topicId, int parentCommentId, int authorId, String content) {
        ForumComment reply = new ForumComment();
        reply.setTopicId(topicId);
        reply.setParentId(parentCommentId);
        reply.setAuthorId(authorId);
        reply.setContent(content);
        addComment(reply);
    }

    public void deleteComment(int id) {
        Connection connection = null;
        boolean initialAutoCommit = true;

        try {
            connection = getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            List<Integer> idsToDelete = collectCommentTreeIds(connection, id);
            for (int index = idsToDelete.size() - 1; index >= 0; index--) {
                int commentId = idsToDelete.get(index);
                executeDelete(connection, "DELETE FROM forum_comment_reaction WHERE comment_id = ?", commentId);
                executeDelete(connection, "DELETE FROM forum_comment WHERE id = ?", commentId);
            }

            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Erreur suppression commentaire: " + e.getMessage(), e);
        } finally {
            resetAutoCommitQuietly(connection, initialAutoCommit);
        }
    }

    private List<Integer> collectCommentTreeIds(Connection connection, int rootCommentId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        collectCommentTreeIds(connection, rootCommentId, ids);
        return ids;
    }

    private void collectCommentTreeIds(Connection connection, int parentCommentId, List<Integer> ids) throws SQLException {
        ids.add(parentCommentId);

        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM forum_comment WHERE parent_id = ?")) {
            ps.setInt(1, parentCommentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    collectCommentTreeIds(connection, rs.getInt("id"), ids);
                }
            }
        }
    }

    private ForumComment mapComment(ResultSet rs) throws SQLException {
        ForumComment comment = new ForumComment();
        comment.setId(rs.getInt("id"));
        comment.setAuthorId(rs.getInt("author_id"));
        comment.setTopicId(rs.getInt("topic_id"));
        int parentId = rs.getInt("parent_id");
        comment.setParentId(rs.wasNull() ? null : parentId);
        int reportedById = rs.getInt("reported_by_id");
        comment.setReportedById(rs.wasNull() ? null : reportedById);
        comment.setContent(rs.getString("content"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            comment.setCreatedAt(createdAt.toLocalDateTime());
        }
        comment.setReported(rs.getBoolean("is_reported"));
        comment.setHidden(rs.getBoolean("is_hidden"));
        comment.setReportedReason(rs.getString("reported_reason"));
        Timestamp reportedAt = rs.getTimestamp("reported_at");
        if (reportedAt != null) {
            comment.setReportedAt(reportedAt.toLocalDateTime());
        }
        comment.setAuthorName(trimToNull(rs.getString("author_name")));
        comment.setAuthorRoles(rs.getString("author_roles"));
        comment.setTopicTitle(trimToNull(rs.getString("topic_title")));
        return comment;
    }

    private String buildModeratedCommentsQuery(boolean reportedOnly, boolean hiddenOnly) {
        String whereClause;
        if (reportedOnly && !hiddenOnly) {
            whereClause = "c.is_reported = 1";
        } else if (!reportedOnly && hiddenOnly) {
            whereClause = "c.is_hidden = 1";
        } else {
            whereClause = "(c.is_reported = 1 OR c.is_hidden = 1)";
        }

        return """
                SELECT c.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       t.title AS topic_title
                FROM forum_comment c
                LEFT JOIN user u ON u.id = c.author_id
                LEFT JOIN forum_topic t ON t.id = c.topic_id
                WHERE %s
                ORDER BY c.reported_at DESC, c.created_at DESC
                """.formatted(whereClause);
    }

    private String required(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Le commentaire ne peut pas etre vide.");
        }
        return value.trim();
    }

    private void executeDelete(Connection connection, String query, int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private int fetchLastInsertId(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT LAST_INSERT_ID()");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countByQuery(String query) {
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement statistiques commentaires: " + e.getMessage(), e);
        }
        return 0;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Connection getConnection() throws SQLException {
        Connection connection = MyConnection.getInstance().getConnection();
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connexion MySQL indisponible.");
        }
        return connection;
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void resetAutoCommitQuietly(Connection connection, boolean autoCommit) {
        if (connection == null) {
            return;
        }

        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ignored) {
        }
    }
}
