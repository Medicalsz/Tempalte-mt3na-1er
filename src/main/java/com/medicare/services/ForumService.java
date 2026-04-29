package com.medicare.services;

import com.medicare.models.ForumTopic;
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

public class ForumService {
    private final NotificationService notificationService = new NotificationService();

    public List<ForumTopic> findAll() {
        return findAll(false);
    }

    public List<ForumTopic> findAll(boolean includeHidden) {
        List<ForumTopic> topics = new ArrayList<>();
        String query = includeHidden ? """
                SELECT t.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       (
                           SELECT COUNT(*)
                           FROM forum_comment c
                           WHERE c.topic_id = t.id
                       ) AS comment_count
                FROM forum_topic t
                LEFT JOIN user u ON u.id = t.author_id
                ORDER BY t.created_at DESC
                """ : """
                SELECT t.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       (
                           SELECT COUNT(*)
                           FROM forum_comment c
                           WHERE c.topic_id = t.id AND c.is_hidden = 0
                       ) AS comment_count
                FROM forum_topic t
                LEFT JOIN user u ON u.id = t.author_id
                WHERE t.is_hidden = 0
                ORDER BY t.created_at DESC
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                topics.add(mapTopic(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement des sujets forum: " + e.getMessage(), e);
        }

        return topics;
    }

    public ForumTopic findById(int id) {
        return findById(id, false);
    }

    public ForumTopic findById(int id, boolean includeHidden) {
        String query = includeHidden ? """
                SELECT t.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       (
                           SELECT COUNT(*)
                           FROM forum_comment c
                           WHERE c.topic_id = t.id
                       ) AS comment_count
                FROM forum_topic t
                LEFT JOIN user u ON u.id = t.author_id
                WHERE t.id = ?
                LIMIT 1
                """ : """
                SELECT t.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       (
                           SELECT COUNT(*)
                           FROM forum_comment c
                           WHERE c.topic_id = t.id AND c.is_hidden = 0
                       ) AS comment_count
                FROM forum_topic t
                LEFT JOIN user u ON u.id = t.author_id
                WHERE t.id = ? AND t.is_hidden = 0
                LIMIT 1
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTopic(rs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement du sujet forum: " + e.getMessage(), e);
        }

        return null;
    }

    public List<ForumTopic> findModeratedTopics(boolean reportedOnly, boolean hiddenOnly) {
        List<ForumTopic> topics = new ArrayList<>();
        String query = buildModeratedTopicsQuery(reportedOnly, hiddenOnly);

        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                topics.add(mapTopic(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement moderation sujets forum: " + e.getMessage(), e);
        }

        return topics;
    }

    public int countTopics() {
        return countByQuery("SELECT COUNT(*) FROM forum_topic");
    }

    public int countReportedTopics() {
        return countByQuery("SELECT COUNT(*) FROM forum_topic WHERE is_reported = 1");
    }

    public int countHiddenTopics() {
        return countByQuery("SELECT COUNT(*) FROM forum_topic WHERE is_hidden = 1");
    }

    public List<ForumTopic> findTopCommentedTopics(int limit) {
        List<ForumTopic> topics = new ArrayList<>();
        String query = """
                SELECT t.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       COALESCE(stats.comment_count, 0) AS comment_count
                FROM forum_topic t
                LEFT JOIN user u ON u.id = t.author_id
                LEFT JOIN (
                    SELECT topic_id, COUNT(*) AS comment_count
                    FROM forum_comment
                    GROUP BY topic_id
                ) stats ON stats.topic_id = t.id
                ORDER BY comment_count DESC, t.created_at DESC
                LIMIT ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, Math.max(limit, 1));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapTopic(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement top sujets forum: " + e.getMessage(), e);
        }

        return topics;
    }

    public List<ForumTopic> findRecentModeratedTopics(int limit) {
        List<ForumTopic> topics = new ArrayList<>();
        String query = """
                SELECT t.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       (
                           SELECT COUNT(*)
                           FROM forum_comment c
                           WHERE c.topic_id = t.id
                       ) AS comment_count
                FROM forum_topic t
                LEFT JOIN user u ON u.id = t.author_id
                WHERE t.is_reported = 1 OR t.is_hidden = 1
                ORDER BY COALESCE(t.reported_at, t.updated_at, t.created_at) DESC, t.id DESC
                LIMIT ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, Math.max(limit, 1));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapTopic(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement moderation recente sujets forum: " + e.getMessage(), e);
        }

        return topics;
    }

    public void setTopicHidden(int id, boolean hidden) {
        String query = """
                UPDATE forum_topic
                SET is_hidden = ?, updated_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setBoolean(1, hidden);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur moderation sujet forum: " + e.getMessage(), e);
        }
    }

    public void setTopicReported(int id, boolean reported, Integer reportedById) {
        String query = """
                UPDATE forum_topic
                SET is_reported = ?, reported_by_id = ?, reported_reason = ?, reported_at = ?, updated_at = ?
                WHERE id = ?
                """;

        LocalDateTime now = LocalDateTime.now();

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setBoolean(1, reported);
            if (reported && reportedById != null) {
                ps.setInt(2, reportedById);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (reported) {
                ps.setString(3, "Moderation JavaFX");
                ps.setTimestamp(4, Timestamp.valueOf(now));
            } else {
                ps.setNull(3, Types.VARCHAR);
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setInt(6, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur signalement sujet forum: " + e.getMessage(), e);
        }
    }

    public void addTopic(ForumTopic topic) {
        String query = """
                INSERT INTO forum_topic (
                    author_id, reported_by_id, title, content, type, video_url, summary, tags,
                    created_at, updated_at, is_reported, is_hidden, reported_reason, reported_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();
        Connection connection = null;

        try {
            connection = getConnection();
            try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, topic.getAuthorId());
            if (topic.getReportedById() != null) {
                ps.setInt(2, topic.getReportedById());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, required(topic.getTitle(), "Le titre est obligatoire."));
            ps.setString(4, required(topic.getContent(), "Le contenu est obligatoire."));
            ps.setString(5, normalizeType(topic.getType()));
            ps.setString(6, nullable(topic.getVideoUrl()));
            ps.setString(7, nullable(topic.getSummary()));
            ps.setString(8, toJsonTags(topic.getTags()));
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.setTimestamp(10, Timestamp.valueOf(now));
            ps.setBoolean(11, topic.isReported());
            ps.setBoolean(12, topic.isHidden());
            ps.setString(13, nullable(topic.getReportedReason()));
            if (topic.getReportedAt() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(topic.getReportedAt()));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }
            ps.executeUpdate();
            topic.setId(fetchLastInsertId(connection));
            }
            notifyNewTopic(topic);
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur ajout sujet forum: " + e.getMessage(), e);
        }
    }

    private void notifyNewTopic(ForumTopic topic) {
        if (topic == null || topic.getAuthorId() <= 0 || topic.getId() <= 0) {
            return;
        }

        try {
            String actorName = notificationService.findUserDisplayName(topic.getAuthorId());
            String title = topic.getTitle() == null || topic.getTitle().isBlank() ? "Nouveau sujet" : topic.getTitle().trim();
            notificationService.createNotificationForAllUsers(
                    topic.getAuthorId(),
                    "Nouveau sujet forum",
                    actorName + " a publie un nouveau sujet : " + title,
                    NotificationService.TYPE_NEW_TOPIC,
                    topic.getId(),
                    null
            );
        } catch (Exception e) {
            System.err.println("Notification nouveau sujet non creee pour topicId=" + topic.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateTopic(ForumTopic topic) {
        String query = """
                UPDATE forum_topic
                SET title = ?, content = ?, type = ?, video_url = ?, summary = ?, tags = ?, updated_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, required(topic.getTitle(), "Le titre est obligatoire."));
            ps.setString(2, required(topic.getContent(), "Le contenu est obligatoire."));
            ps.setString(3, normalizeType(topic.getType()));
            ps.setString(4, nullable(topic.getVideoUrl()));
            ps.setString(5, nullable(topic.getSummary()));
            ps.setString(6, toJsonTags(topic.getTags()));
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(8, topic.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur modification sujet forum: " + e.getMessage(), e);
        }
    }

    public void deleteTopic(int id) {
        Connection connection = null;
        boolean initialAutoCommit = true;

        try {
            connection = getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            executeDelete(connection, """
                    DELETE reaction
                    FROM forum_comment_reaction reaction
                    JOIN forum_comment comment ON comment.id = reaction.comment_id
                    WHERE comment.topic_id = ?
                    """, id);
            executeDelete(connection, "DELETE FROM forum_comment WHERE topic_id = ?", id);
            executeDelete(connection, "DELETE FROM forum_topic_reaction WHERE topic_id = ?", id);
            executeDelete(connection, "DELETE FROM forum_topic WHERE id = ?", id);

            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Erreur suppression sujet forum: " + e.getMessage(), e);
        } finally {
            resetAutoCommitQuietly(connection, initialAutoCommit);
        }
    }

    private ForumTopic mapTopic(ResultSet rs) throws SQLException {
        ForumTopic topic = new ForumTopic();
        topic.setId(rs.getInt("id"));
        topic.setAuthorId(rs.getInt("author_id"));
        int reportedById = rs.getInt("reported_by_id");
        topic.setReportedById(rs.wasNull() ? null : reportedById);
        topic.setTitle(rs.getString("title"));
        topic.setContent(rs.getString("content"));
        topic.setType(rs.getString("type"));
        topic.setVideoUrl(rs.getString("video_url"));
        topic.setSummary(rs.getString("summary"));
        topic.setTags(rs.getString("tags"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            topic.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            topic.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        topic.setReported(rs.getBoolean("is_reported"));
        topic.setHidden(rs.getBoolean("is_hidden"));
        topic.setReportedReason(rs.getString("reported_reason"));
        Timestamp reportedAt = rs.getTimestamp("reported_at");
        if (reportedAt != null) {
            topic.setReportedAt(reportedAt.toLocalDateTime());
        }
        topic.setAuthorName(trimToNull(rs.getString("author_name")));
        topic.setAuthorRoles(rs.getString("author_roles"));
        topic.setCommentCount(rs.getInt("comment_count"));
        return topic;
    }

    private String buildModeratedTopicsQuery(boolean reportedOnly, boolean hiddenOnly) {
        String whereClause;
        if (reportedOnly && !hiddenOnly) {
            whereClause = "t.is_reported = 1";
        } else if (!reportedOnly && hiddenOnly) {
            whereClause = "t.is_hidden = 1";
        } else {
            whereClause = "(t.is_reported = 1 OR t.is_hidden = 1)";
        }

        return """
                SELECT t.*,
                       CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS author_name,
                       u.roles AS author_roles,
                       (
                           SELECT COUNT(*)
                           FROM forum_comment c
                           WHERE c.topic_id = t.id
                       ) AS comment_count
                FROM forum_topic t
                LEFT JOIN user u ON u.id = t.author_id
                WHERE %s
                ORDER BY t.reported_at DESC, t.updated_at DESC, t.created_at DESC
                """.formatted(whereClause);
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
            throw new IllegalStateException("Erreur chargement statistiques sujets forum: " + e.getMessage(), e);
        }
        return 0;
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private String nullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeType(String type) {
        if ("video".equalsIgnoreCase(type)) {
            return "video";
        }
        return "text";
    }

    private String toJsonTags(String rawTags) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return "[]";
        }
        String trimmed = rawTags.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }

        String[] parts = trimmed.split(",");
        List<String> tags = new ArrayList<>();
        for (String part : parts) {
            String clean = part.trim();
            if (!clean.isEmpty()) {
                tags.add("\"" + clean.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
            }
        }
        return tags.isEmpty() ? "[]" : "[" + String.join(",", tags) + "]";
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
