package com.medicare.services;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.medicare.interfaces.Crud;
import com.medicare.models.Collaboration;
import com.medicare.utils.MyConnection;

public class CollaborationService implements Crud<Collaboration> {

    public void add(Collaboration collaboration) {
        String query = "INSERT INTO collaboration (partner_id, user_id, date_debut, date_fin, titre, description, statut, image_name, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, collaboration.getPartnerId());
            pst.setObject(2, collaboration.getUserId());
            pst.setDate(3, Date.valueOf(collaboration.getDateDebut()));
            pst.setDate(4, Date.valueOf(collaboration.getDateFin()));
            pst.setString(5, collaboration.getTitre());
            pst.setString(6, collaboration.getDescription());
            pst.setString(7, collaboration.getStatut());
            pst.setString(8, collaboration.getImageName());
            pst.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
            System.out.println("Collaboration added successfully!");
        } catch (SQLException e) {
            System.err.println("Error adding collaboration: " + e.getMessage());
        }
    }

    public void update(Collaboration collaboration) {
        String query = "UPDATE collaboration SET partner_id = ?, user_id = ?, date_debut = ?, date_fin = ?, titre = ?, description = ?, statut = ?, image_name = ?, updated_at = ? WHERE id = ?";
        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, collaboration.getPartnerId());
            pst.setObject(2, collaboration.getUserId());
            pst.setDate(3, Date.valueOf(collaboration.getDateDebut()));
            pst.setDate(4, Date.valueOf(collaboration.getDateFin()));
            pst.setString(5, collaboration.getTitre());
            pst.setString(6, collaboration.getDescription());
            pst.setString(7, collaboration.getStatut());
            pst.setString(8, collaboration.getImageName());
            pst.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            pst.setInt(10, collaboration.getId());
            pst.executeUpdate();
            System.out.println("Collaboration updated successfully!");
        } catch (SQLException e) {
            System.err.println("Error updating collaboration: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM collaboration WHERE id = ?";
        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Collaboration deleted successfully!");
        } catch (SQLException e) {
            System.err.println("Error deleting collaboration: " + e.getMessage());
        }
    }

    public List<Collaboration> getAll() {
        List<Collaboration> collaborations = new ArrayList<>();
        String query = "SELECT c.*, p.name as partner_name, u.nom as user_name FROM collaboration c " +
                       "LEFT JOIN partner p ON c.partner_id = p.id " +
                       "LEFT JOIN user u ON c.user_id = u.id";
        try (Connection cnx = MyConnection.getInstance().getCnx();
             Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Collaboration collaboration = new Collaboration();
                collaboration.setId(rs.getInt("id"));
                collaboration.setPartnerId(rs.getInt("partner_id"));
                collaboration.setUserId(rs.getInt("user_id"));
                collaboration.setTitre(rs.getString("titre"));
                collaboration.setDescription(rs.getString("description"));
                collaboration.setStatut(rs.getString("statut"));
                collaboration.setImageName(rs.getString("image_name"));

                // Safely handle nullable dates
                Date dateDebut = rs.getDate("date_debut");
                if (dateDebut != null) {
                    collaboration.setDateDebut(dateDebut.toLocalDate());
                }

                Date dateFin = rs.getDate("date_fin");
                if (dateFin != null) {
                    collaboration.setDateFin(dateFin.toLocalDate());
                }

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    collaboration.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                
                // Set joined names
                collaboration.setPartnerName(rs.getString("partner_name"));
                collaboration.setUserName(rs.getString("user_name"));

                collaborations.add(collaboration);
            }
        } catch (SQLException e) {
            System.err.println("Error getting collaborations: " + e.getMessage());
        }
        return collaborations;
    }
}