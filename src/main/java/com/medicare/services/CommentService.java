package com.medicare.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.medicare.models.Comment;
import com.medicare.utils.MyConnection;

public class CommentService {

    public List<Comment> getCommentsForPartner(int partnerId) {
        List<Comment> comments = new ArrayList<>();
        // Requête SQL corrigée pour joindre partner_rating et user
        String sql = "SELECT pr.id, pr.partner_id, pr.author_id, u.nom as userName, pr.comment as content, pr.rating, pr.sentiment, pr.created_at " +
                     "FROM partner_rating pr " +
                     "JOIN user u ON pr.author_id = u.id " +
                     "WHERE pr.partner_id = ?";

        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partnerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                comments.add(new Comment(
                        rs.getInt("id"),
                        rs.getInt("partner_id"),
                        rs.getInt("author_id"),
                        rs.getString("userName"),
                        rs.getString("content"),
                        rs.getInt("rating"),
                        rs.getString("sentiment"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des commentaires : " + e.getMessage());
            e.printStackTrace();
        }
        return comments;
    }

    public double getAverageRatingForPartner(int partnerId) {
        // Requête corrigée pour utiliser la table partner_rating
        String sql = "SELECT AVG(rating) as avg_rating FROM partner_rating WHERE partner_id = ?";
       try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partnerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul de la note moyenne : " + e.getMessage());
        }
        return 0.0;
    }

    public void addComment(int partnerId, int userId, String content, int rating) {
        // La colonne pour le texte du commentaire est 'comment' dans la table partner_rating
        String sql = "INSERT INTO partner_rating (partner_id, author_id, comment, rating, created_at) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partnerId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, content);
            pstmt.setInt(4, rating);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du commentaire : " + e.getMessage());
            e.printStackTrace();
        }
    }
}