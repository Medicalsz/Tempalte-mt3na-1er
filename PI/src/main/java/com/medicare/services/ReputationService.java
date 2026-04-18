package com.medicare.services;

import com.medicare.models.Badge;
import com.medicare.models.PartnerRating;
import com.medicare.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReputationService {

    private Connection connection;

    public ReputationService() {
        connection = MyConnection.getInstance().getCnx();
    }

    public List<PartnerRating> getRatingsForPartner(int partnerId) {
        List<PartnerRating> ratings = new ArrayList<>();
        String sql = "SELECT pr.*, u.nom, u.prenom " +
                     "FROM partner_rating pr " +
                     "LEFT JOIN user u ON pr.comment = u.id " + // Assuming the comment field stores the user ID
                     "WHERE pr.partner_id = ? " +
                     "ORDER BY pr.created_at DESC";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, partnerId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                PartnerRating rating = new PartnerRating();
                rating.setId(rs.getInt("id"));
                rating.setPartnerId(rs.getInt("partner_id"));
                rating.setRating(rs.getInt("rating"));
                rating.setSentiment(rs.getString("sentiment"));
                if (rs.getTimestamp("created_at") != null) {
                    rating.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }

                // Check if a user was joined
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                if (nom != null && prenom != null) {
                    rating.setUserName(prenom + " " + nom);
                    // If the comment was just a user ID, we don't need to display it
                    rating.setComment(null);
                } else {
                    rating.setUserName("Anonyme");
                    rating.setComment(rs.getString("comment"));
                }

                ratings.add(rating);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching ratings for partner " + partnerId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return ratings;
    }

    public List<Badge> getBadgesForPartner(int partnerId) {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT b.id, b.name, b.description FROM badges b " +
                     "JOIN partner_badges pb ON b.id = pb.badge_id " +
                     "WHERE pb.partner_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, partnerId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Badge badge = new Badge();
                badge.setId(rs.getString("id"));
                badge.setName(rs.getString("name"));
                badge.setDescription(rs.getString("description"));
                badges.add(badge);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching badges for partner " + partnerId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return badges;
    }

    public void addRating(int partnerId, int rating, String comment) {
        String sql = "INSERT INTO partner_rating (partner_id, rating, comment, created_at) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, partnerId);
            statement.setInt(2, rating);
            statement.setString(3, comment);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding rating for partner " + partnerId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}