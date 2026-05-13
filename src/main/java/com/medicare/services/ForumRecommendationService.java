package com.medicare.services;

import com.medicare.models.ForumTopic;
import com.medicare.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ForumRecommendationService {

    private static final int RECENT_DAYS = 30;

    public List<ForumTopic> recommendForTopic(int topicId, int limit) {
        ForumTopic current = findTopicById(topicId);
        if (current == null) {
            return List.of();
        }

        List<ForumTopic> candidates = findVisibleCandidates(topicId);
        candidates.sort(
                Comparator.comparingDouble((ForumTopic candidate) -> calculateScore(current, candidate))
                        .reversed()
                        .thenComparing(ForumTopic::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ForumTopic::getId, Comparator.reverseOrder())
        );

        int max = Math.max(1, limit);
        return candidates.size() > max ? new ArrayList<>(candidates.subList(0, max)) : candidates;
    }

    public double calculateScore(ForumTopic current, ForumTopic candidate) {
        if (current == null || candidate == null || current.getId() == candidate.getId() || candidate.isHidden()) {
            return 0;
        }

        double score = 0;

        Set<String> currentTags = extractTags(current.getTagsDisplay());
        Set<String> candidateTags = extractTags(candidate.getTagsDisplay());
        for (String tag : currentTags) {
            if (candidateTags.contains(tag)) {
                score += 5;
            }
        }

        if (sameType(current, candidate)) {
            score += 3;
        }

        Set<String> currentTitleWords = extractImportantWords(current.getTitle());
        Set<String> candidateTitleWords = extractImportantWords(candidate.getTitle());
        for (String word : currentTitleWords) {
            if (candidateTitleWords.contains(word)) {
                score += 2;
            }
        }

        Set<String> currentContentWords = extractImportantWords(current.getContent());
        Set<String> candidateContentWords = extractImportantWords(candidate.getContent());
        for (String word : currentContentWords) {
            if (candidateContentWords.contains(word)) {
                score += 1;
            }
        }

        if (isRecent(candidate)) {
            score += 1;
        }

        return score;
    }

    private ForumTopic findTopicById(int topicId) {
        String query = """
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
                WHERE t.id = ?
                LIMIT 1
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTopic(rs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement sujet recommendation: " + e.getMessage(), e);
        }
        return null;
    }

    private List<ForumTopic> findVisibleCandidates(int excludedTopicId) {
        List<ForumTopic> topics = new ArrayList<>();
        String query = """
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
                WHERE t.id <> ? AND t.is_hidden = 0
                ORDER BY t.created_at DESC
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, excludedTopicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapTopic(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur chargement recommandations forum: " + e.getMessage(), e);
        }
        return topics;
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

    private Set<String> extractTags(String tagsDisplay) {
        Set<String> tags = new HashSet<>();
        if (tagsDisplay == null || tagsDisplay.isBlank()) {
            return tags;
        }
        for (String rawTag : tagsDisplay.split(",")) {
            String cleaned = normalizeToken(rawTag.replace("#", ""));
            if (!cleaned.isEmpty()) {
                tags.add(cleaned);
            }
        }
        return tags;
    }

    private Set<String> extractImportantWords(String value) {
        Set<String> words = new HashSet<>();
        if (value == null || value.isBlank()) {
            return words;
        }
        for (String rawWord : value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+")) {
            String word = normalizeToken(rawWord);
            if (word.length() < 3 || isStopWord(word)) {
                continue;
            }
            words.add(word);
        }
        return words;
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean sameType(ForumTopic current, ForumTopic candidate) {
        String currentType = current.getType() == null ? "" : current.getType();
        String candidateType = candidate.getType() == null ? "" : candidate.getType();
        return currentType.equalsIgnoreCase(candidateType);
    }

    private boolean isRecent(ForumTopic topic) {
        return topic.getCreatedAt() != null && topic.getCreatedAt().isAfter(LocalDateTime.now().minusDays(RECENT_DAYS));
    }

    private boolean isStopWord(String word) {
        return switch (word) {
            case "les", "des", "une", "avec", "pour", "dans", "sur", "par", "est", "sont",
                    "mais", "plus", "comme", "vous", "nous", "leur", "cela", "ceci", "quel",
                    "quelle", "quels", "quelles", "sujet", "forum", "article", "video", "commentaire",
                    "the", "and", "for", "with", "this", "that", "from", "etre", "avoir" -> true;
            default -> false;
        };
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
