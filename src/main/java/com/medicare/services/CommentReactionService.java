package com.medicare.services;

import com.medicare.models.ForumCommentReaction;
import com.medicare.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CommentReactionService {

    public static final String TYPE_LIKE = "like";
    public static final String TYPE_LOVE = "love";

    private static final Set<String> SUPPORTED_TYPES = Set.of(TYPE_LIKE, TYPE_LOVE);

    public String getUserReactionForComment(int commentId, int userId) {
        ForumCommentReaction reaction = findReaction(commentId, userId);
        return reaction != null ? reaction.getType() : null;
    }

    public int countByType(int commentId, String type) {
        String normalizedType = requireSupportedType(type);
        String query = """
                SELECT COUNT(*)
                FROM forum_comment_reaction
                WHERE comment_id = ? AND type = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, commentId);
            ps.setString(2, normalizedType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur comptage reactions commentaire: " + e.getMessage(), e);
        }
    }

    public void toggleReaction(int commentId, int userId, String type) {
        String normalizedType = requireSupportedType(type);
        Connection connection = null;
        boolean initialAutoCommit = true;

        try {
            connection = getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            ForumCommentReaction existingReaction = findReaction(connection, commentId, userId);

            if (existingReaction == null) {
                insertReaction(connection, commentId, userId, normalizedType);
            } else if (normalizedType.equals(existingReaction.getType())) {
                deleteReaction(connection, existingReaction.getId());
            } else {
                updateReaction(connection, existingReaction.getId(), normalizedType);
            }

            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Erreur mise a jour reaction commentaire: " + e.getMessage(), e);
        } finally {
            resetAutoCommitQuietly(connection, initialAutoCommit);
        }
    }

    public Map<String, Integer> getReactionCounts(int commentId) {
        Map<String, Integer> counts = createDefaultCounts();
        String query = """
                SELECT type, COUNT(*) AS total
                FROM forum_comment_reaction
                WHERE comment_id = ?
                GROUP BY type
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = sanitizeType(rs.getString("type"));
                    if (SUPPORTED_TYPES.contains(type)) {
                        counts.put(type, rs.getInt("total"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement reactions commentaire: " + e.getMessage(), e);
        }

        return counts;
    }

    private ForumCommentReaction findReaction(int commentId, int userId) {
        try {
            return findReaction(getConnection(), commentId, userId);
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lecture reaction utilisateur: " + e.getMessage(), e);
        }
    }

    private ForumCommentReaction findReaction(Connection connection, int commentId, int userId) throws SQLException {
        String query = """
                SELECT id, comment_id, user_id, type, created_at
                FROM forum_comment_reaction
                WHERE comment_id = ? AND user_id = ?
                LIMIT 1
                """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, commentId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapReaction(rs) : null;
            }
        }
    }

    private void insertReaction(Connection connection, int commentId, int userId, String type) throws SQLException {
        String query = """
                INSERT INTO forum_comment_reaction (comment_id, user_id, type, created_at)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, commentId);
            ps.setInt(2, userId);
            ps.setString(3, type);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private void updateReaction(Connection connection, int reactionId, String type) throws SQLException {
        String query = """
                UPDATE forum_comment_reaction
                SET type = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, type);
            ps.setInt(2, reactionId);
            ps.executeUpdate();
        }
    }

    private void deleteReaction(Connection connection, int reactionId) throws SQLException {
        String query = "DELETE FROM forum_comment_reaction WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, reactionId);
            ps.executeUpdate();
        }
    }

    private ForumCommentReaction mapReaction(ResultSet rs) throws SQLException {
        ForumCommentReaction reaction = new ForumCommentReaction();
        reaction.setId(rs.getInt("id"));
        reaction.setCommentId(rs.getInt("comment_id"));
        reaction.setUserId(rs.getInt("user_id"));
        reaction.setType(sanitizeType(rs.getString("type")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            reaction.setCreatedAt(createdAt.toLocalDateTime());
        }
        return reaction;
    }

    private Map<String, Integer> createDefaultCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(TYPE_LIKE, 0);
        counts.put(TYPE_LOVE, 0);
        return counts;
    }

    private String requireSupportedType(String type) {
        String normalizedType = sanitizeType(type);
        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            throw new IllegalStateException("Type de reaction non supporte: " + type);
        }
        return normalizedType;
    }

    private String sanitizeType(String type) {
        if (type == null) {
            throw new IllegalStateException("Le type de reaction est requis.");
        }

        return type.trim().toLowerCase(Locale.ROOT);
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
