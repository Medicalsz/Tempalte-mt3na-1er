package com.medicare.services;

import com.medicare.models.Notification;
import com.medicare.utils.MyConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    public static final String TYPE_NEW_TOPIC = "NEW_TOPIC";
    public static final String TYPE_NEW_COMMENT = "NEW_COMMENT";
    private boolean notificationTableReady;

    public NotificationService() {
        prepareNotificationTable();
    }

    public int createNotificationForAllUsers(int actorId,
                                             String title,
                                             String message,
                                             String type,
                                             Integer relatedTopicId,
                                             Integer relatedCommentId) {
        prepareNotificationTable();
        String usersQuery = "SELECT id FROM user WHERE id <> ?";

        try {
            Connection connection = getConnection();
            String insertQuery = buildInsertNotificationQuery(connection);
            String actorName = findUserDisplayName(actorId);
            int recipientCount = 0;
            try (PreparedStatement userStatement = connection.prepareStatement(usersQuery);
                 PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                userStatement.setInt(1, actorId);
                try (ResultSet rs = userStatement.executeQuery()) {
                    while (rs.next()) {
                        recipientCount++;
                        bindInsertNotification(
                                insertStatement,
                                connection,
                                rs.getInt("id"),
                                actorId,
                                actorName,
                                title,
                                message,
                                type,
                                relatedTopicId,
                                relatedCommentId
                        );
                        insertStatement.addBatch();
                    }
                }
                insertStatement.executeBatch();
            }
            System.out.println("[NotificationService] Notification creee type=" + type
                    + " actorId=" + actorId
                    + " recipients=" + recipientCount
                    + " topicId=" + relatedTopicId
                    + " commentId=" + relatedCommentId);
            return recipientCount;
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur creation notifications: " + e.getMessage(), e);
        }
    }

    public List<Notification> findByUserId(int userId) {
        prepareNotificationTable();
        List<Notification> notifications = new ArrayList<>();

        try {
            Connection connection = getConnection();
            String query = buildFindByUserQuery(connection);
            try (PreparedStatement ps = connection.prepareStatement(query)) {
            int index = 1;
            if (columnExists(connection, "user_id")) {
                ps.setInt(index++, userId);
            }
            if (columnExists(connection, "recipient_id")) {
                ps.setInt(index, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement notifications: " + e.getMessage(), e);
        }
        return notifications;
    }

    public int countUnreadByUserId(int userId) {
        prepareNotificationTable();
        try {
            Connection connection = getConnection();
            String query = buildUnreadCountQuery(connection);
            try (PreparedStatement ps = connection.prepareStatement(query)) {
            int index = 1;
            if (columnExists(connection, "user_id")) {
                ps.setInt(index++, userId);
            }
            if (columnExists(connection, "recipient_id")) {
                ps.setInt(index, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                int unread = rs.next() ? rs.getInt(1) : 0;
                System.out.println("[NotificationService] Non lues userId=" + userId + " count=" + unread);
                return unread;
            }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur comptage notifications: " + e.getMessage(), e);
        }
    }

    public void markAsRead(int notificationId) {
        prepareNotificationTable();
        String query = "UPDATE notification SET is_read = 1 WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lecture notification: " + e.getMessage(), e);
        }
    }

    public void markAllAsRead(int userId) {
        prepareNotificationTable();
        try {
            Connection connection = getConnection();
            String query = buildMarkAllReadQuery(connection);
            try (PreparedStatement ps = connection.prepareStatement(query)) {
            int index = 1;
            if (columnExists(connection, "user_id")) {
                ps.setInt(index++, userId);
            }
            if (columnExists(connection, "recipient_id")) {
                ps.setInt(index, userId);
            }
            ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lecture notifications: " + e.getMessage(), e);
        }
    }

    public String findUserDisplayName(int userId) {
        prepareNotificationTable();
        String query = "SELECT prenom, nom FROM user WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = ((rs.getString("prenom") == null ? "" : rs.getString("prenom")) + " " +
                            (rs.getString("nom") == null ? "" : rs.getString("nom"))).trim();
                    return name.isEmpty() ? "Un utilisateur" : name;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement auteur notification: " + e.getMessage(), e);
        }
        return "Un utilisateur";
    }

    public String findTopicTitle(int topicId) {
        prepareNotificationTable();
        String query = "SELECT title FROM forum_topic WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return trimToDefault(rs.getString("title"), "ce sujet");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement sujet notification: " + e.getMessage(), e);
        }
        return "ce sujet";
    }

    private void ensureNotificationTable() {
        String query = """
                CREATE TABLE IF NOT EXISTS notification (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    actor_id INT NULL,
                    title VARCHAR(255) NOT NULL,
                    message TEXT NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    related_topic_id INT NULL,
                    related_comment_id INT NULL,
                    is_read TINYINT(1) NOT NULL DEFAULT 0,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_notification_user_read (user_id, is_read),
                    INDEX idx_notification_created_at (created_at)
                )
                """;

        try (Statement statement = getConnection().createStatement()) {
            statement.execute(query);
            ensureNotificationColumns();
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur preparation table notification: " + e.getMessage(), e);
        }
    }

    private void ensureNotificationColumns() throws SQLException {
        Connection connection = getConnection();
        addColumnIfMissing(connection, "id", "INT AUTO_INCREMENT PRIMARY KEY FIRST");
        addColumnIfMissing(connection, "user_id", "INT NULL");
        addColumnIfMissing(connection, "actor_id", "INT NULL");
        addColumnIfMissing(connection, "title", "VARCHAR(255) NULL");
        addColumnIfMissing(connection, "message", "TEXT NULL");
        addColumnIfMissing(connection, "type", "VARCHAR(50) NULL");
        addColumnIfMissing(connection, "related_topic_id", "INT NULL");
        addColumnIfMissing(connection, "related_comment_id", "INT NULL");
        addColumnIfMissing(connection, "is_read", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, "created_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
        addIndexIfMissing(connection, "idx_notification_user_read", "CREATE INDEX idx_notification_user_read ON notification (user_id, is_read)");
        addIndexIfMissing(connection, "idx_notification_created_at", "CREATE INDEX idx_notification_created_at ON notification (created_at)");
    }

    private String buildInsertNotificationQuery(Connection connection) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        addInsertColumnIfExists(connection, columns, values, "user_id");
        addInsertColumnIfExists(connection, columns, values, "recipient_id");
        addInsertColumnIfExists(connection, columns, values, "actor_id");
        addInsertColumnIfExists(connection, columns, values, "sender_id");
        addInsertColumnIfExists(connection, columns, values, "author_name");
        addInsertColumnIfExists(connection, columns, values, "title");
        addInsertColumnIfExists(connection, columns, values, "message");
        addInsertColumnIfExists(connection, columns, values, "type");
        addInsertColumnIfExists(connection, columns, values, "related_topic_id");
        addInsertColumnIfExists(connection, columns, values, "topic_id");
        addInsertColumnIfExists(connection, columns, values, "related_comment_id");
        addInsertColumnIfExists(connection, columns, values, "comment_id");
        addInsertColumnIfExists(connection, columns, values, "link");
        addInsertColumnIfExists(connection, columns, values, "is_read");
        addInsertColumnIfExists(connection, columns, values, "created_at");

        return "INSERT INTO notification (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", values) + ")";
    }

    private void addInsertColumnIfExists(Connection connection, List<String> columns, List<String> values, String columnName)
            throws SQLException {
        if (columnExists(connection, columnName)) {
            columns.add("`" + columnName + "`");
            values.add("?");
        }
    }

    private void bindInsertNotification(PreparedStatement ps,
                                        Connection connection,
                                        int recipientId,
                                        int actorId,
                                        String actorName,
                                        String title,
                                        String message,
                                        String type,
                                        Integer relatedTopicId,
                                        Integer relatedCommentId) throws SQLException {
        int index = 1;
        if (columnExists(connection, "user_id")) {
            ps.setInt(index++, recipientId);
        }
        if (columnExists(connection, "recipient_id")) {
            ps.setInt(index++, recipientId);
        }
        if (columnExists(connection, "actor_id")) {
            ps.setInt(index++, actorId);
        }
        if (columnExists(connection, "sender_id")) {
            ps.setInt(index++, actorId);
        }
        if (columnExists(connection, "author_name")) {
            ps.setString(index++, required(actorName, "Un utilisateur"));
        }
        if (columnExists(connection, "title")) {
            ps.setString(index++, required(title, "Notification"));
        }
        if (columnExists(connection, "message")) {
            ps.setString(index++, required(message, title));
        }
        if (columnExists(connection, "type")) {
            ps.setString(index++, required(type, "INFO"));
        }
        if (columnExists(connection, "related_topic_id")) {
            setNullableInt(ps, index++, relatedTopicId);
        }
        if (columnExists(connection, "topic_id")) {
            setNullableInt(ps, index++, relatedTopicId);
        }
        if (columnExists(connection, "related_comment_id")) {
            setNullableInt(ps, index++, relatedCommentId);
        }
        if (columnExists(connection, "comment_id")) {
            setNullableInt(ps, index++, relatedCommentId);
        }
        if (columnExists(connection, "link")) {
            ps.setString(index++, buildNotificationLink(relatedTopicId));
        }
        if (columnExists(connection, "is_read")) {
            ps.setBoolean(index++, false);
        }
        if (columnExists(connection, "created_at")) {
            ps.setTimestamp(index, Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    private String buildFindByUserQuery(Connection connection) throws SQLException {
        String actorJoinColumn = columnExists(connection, "actor_id") ? "n.actor_id" :
                (columnExists(connection, "sender_id") ? "n.sender_id" : "NULL");
        String actorNameExpression = columnExists(connection, "author_name")
                ? "COALESCE(NULLIF(CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')), ' '), n.author_name, 'Un utilisateur')"
                : "COALESCE(NULLIF(CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')), ' '), 'Un utilisateur')";
        return """
                SELECT n.*,
                       %s AS actor_name
                FROM notification n
                LEFT JOIN user u ON u.id = %s
                WHERE %s
                ORDER BY %s, n.created_at DESC, n.id DESC
                """.formatted(actorNameExpression, actorJoinColumn, userWhereClause(connection, "n."), readOrderExpression(connection));
    }

    private String buildUnreadCountQuery(Connection connection) throws SQLException {
        return "SELECT COUNT(*) FROM notification WHERE " + userWhereClause(connection, "") + " AND " + unreadExpression(connection);
    }

    private String buildMarkAllReadQuery(Connection connection) throws SQLException {
        return "UPDATE notification SET " + readSetExpression(connection) + " WHERE " + userWhereClause(connection, "");
    }

    private String userWhereClause(Connection connection, String prefix) throws SQLException {
        List<String> clauses = new ArrayList<>();
        if (columnExists(connection, "user_id")) {
            clauses.add(prefix + "user_id = ?");
        }
        if (columnExists(connection, "recipient_id")) {
            clauses.add(prefix + "recipient_id = ?");
        }
        if (clauses.isEmpty()) {
            return "1 = 0";
        }
        return "(" + String.join(" OR ", clauses) + ")";
    }

    private String unreadExpression(Connection connection) throws SQLException {
        if (columnExists(connection, "is_read")) {
            return "is_read = 0";
        }
        if (columnExists(connection, "read")) {
            return "`read` = 0";
        }
        return "1 = 0";
    }

    private String readSetExpression(Connection connection) throws SQLException {
        if (columnExists(connection, "is_read")) {
            return "is_read = 1";
        }
        if (columnExists(connection, "read")) {
            return "`read` = 1";
        }
        return "is_read = 1";
    }

    private String readOrderExpression(Connection connection) throws SQLException {
        if (columnExists(connection, "is_read")) {
            return "n.is_read ASC";
        }
        if (columnExists(connection, "read")) {
            return "n.`read` ASC";
        }
        return "n.id DESC";
    }

    private String buildNotificationLink(Integer relatedTopicId) {
        if (relatedTopicId == null || relatedTopicId <= 0) {
            return "/forum";
        }
        return "/forum/topic/" + relatedTopicId;
    }

    private void addColumnIfMissing(Connection connection, String columnName, String definition) throws SQLException {
        if (columnExists(connection, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE notification ADD COLUMN " + columnName + " " + definition);
        }
    }

    private boolean columnExists(Connection connection, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, "notification", columnName)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, "notification", columnName.toUpperCase())) {
            return rs.next();
        }
    }

    private void addIndexIfMissing(Connection connection, String indexName, String createSql) throws SQLException {
        if (indexExists(connection, indexName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
    }

    private boolean indexExists(Connection connection, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(connection.getCatalog(), null, "notification", false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void prepareNotificationTable() {
        if (notificationTableReady) {
            return;
        }

        try {
            ensureNotificationTable();
            notificationTableReady = true;
        } catch (Exception e) {
            System.err.println("Table notification non disponible: " + e.getMessage());
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setUserId(rs.getInt("user_id"));
        int actorId = rs.getInt("actor_id");
        notification.setActorId(rs.wasNull() ? null : actorId);
        notification.setActorName(trimToDefault(rs.getString("actor_name"), "Un utilisateur"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setType(rs.getString("type"));
        int topicId = rs.getInt("related_topic_id");
        notification.setRelatedTopicId(rs.wasNull() ? null : topicId);
        int commentId = rs.getInt("related_comment_id");
        notification.setRelatedCommentId(rs.wasNull() ? null : commentId);
        notification.setRead(rs.getBoolean("is_read"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }
        return notification;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private String required(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String trimToDefault(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private Connection getConnection() throws SQLException {
        Connection connection = MyConnection.getInstance().getConnection();
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connexion MySQL indisponible.");
        }
        return connection;
    }
}
