package com.medicare.services;

import com.medicare.models.ForumTopic;
import com.medicare.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ForumDashboardService {

    public static final String SORT_RECENT = "Plus recent";
    public static final String SORT_OLDEST = "Plus ancien";
    public static final String SORT_MOST_COMMENTED = "Plus commente";
    public static final String SORT_REPORTED_FIRST = "Signales d'abord";
    public static final String SORT_HIDDEN_FIRST = "Masques d'abord";
    public static final String SORT_ARTICLE = "Type Article";
    public static final String SORT_VIDEO = "Type Video";

    public List<ForumTopic> findAllTopicsForDashboard() {
        return searchTopics("", SORT_RECENT);
    }

    public List<ForumTopic> findAllTopicsSorted(String sortMode) {
        return searchTopics("", sortMode);
    }

    public List<ForumTopic> searchTopics(String keyword, String sortMode) {
        List<ForumTopic> topics = new ArrayList<>();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String typeFilter = resolveTypeFilter(sortMode);
        String orderBy = resolveOrderBy(sortMode);

        StringBuilder query = new StringBuilder("""
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
                WHERE 1 = 1
                """);

        if (!normalizedKeyword.isEmpty()) {
            query.append("""
                    AND (
                        LOWER(t.title) LIKE ?
                        OR LOWER(CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, ''))) LIKE ?
                        OR LOWER(COALESCE(t.tags, '')) LIKE ?
                    )
                    """);
        }

        if (typeFilter != null) {
            query.append("AND LOWER(COALESCE(t.type, 'text')) = ?\n");
        }

        query.append(orderBy);

        try (PreparedStatement ps = getConnection().prepareStatement(query.toString())) {
            int index = 1;
            if (!normalizedKeyword.isEmpty()) {
                String like = "%" + normalizedKeyword.toLowerCase() + "%";
                ps.setString(index++, like);
                ps.setString(index++, like);
                ps.setString(index++, like);
            }
            if (typeFilter != null) {
                ps.setString(index, typeFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapTopic(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement dashboard sujets forum: " + e.getMessage(), e);
        }

        return topics;
    }

    private String resolveOrderBy(String sortMode) {
        if (SORT_OLDEST.equals(sortMode)) {
            return "ORDER BY t.created_at ASC, t.id ASC";
        }
        if (SORT_MOST_COMMENTED.equals(sortMode)) {
            return "ORDER BY comment_count DESC, t.created_at DESC, t.id DESC";
        }
        if (SORT_REPORTED_FIRST.equals(sortMode)) {
            return "ORDER BY t.is_reported DESC, COALESCE(t.reported_at, t.updated_at, t.created_at) DESC, t.id DESC";
        }
        if (SORT_HIDDEN_FIRST.equals(sortMode)) {
            return "ORDER BY t.is_hidden DESC, t.updated_at DESC, t.created_at DESC, t.id DESC";
        }
        return "ORDER BY t.created_at DESC, t.id DESC";
    }

    private String resolveTypeFilter(String sortMode) {
        if (SORT_VIDEO.equals(sortMode)) {
            return "video";
        }
        if (SORT_ARTICLE.equals(sortMode)) {
            return "text";
        }
        return null;
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
}
