package com.medicare.services;

import com.medicare.utils.MyConnection;
import java.sql.*;
import java.util.*;

public class NotificationService {

    public static final String TYPE_NEW_TOPIC = "NEW_TOPIC";
    public static final String TYPE_NEW_COMMENT = "NEW_COMMENT";

    public record Notification(int id, int userId, Integer medecinId, String type,
                               String titre, String message, boolean isRead, String createdAt) {}

    private final Connection cnx;

    public NotificationService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public void createWelcome(int userId, boolean isMedecin) {
        String message = isMedecin
            ? "Votre compte médecin a été créé avec succès. Complétez votre profil pour commencer à recevoir des patients."
            : "Bienvenue ! Complétez votre profil pour accéder à toutes les fonctionnalités Medicare.";
        insert(userId, null, "system", "🎉 Bienvenue sur Medicare !", message);
    }

    public void createProfileIncomplete(int userId) {
        insert(userId, null, "reminder",
               "⚠️ Profil incomplet",
               "Votre profil est incomplet. Ajoutez vos informations médicales et personnelles pour une meilleure expérience.");
    }

    public void createVerificationPending(int userId) {
        insert(userId, null, "system",
               "⏳ Vérification en attente",
               "Votre demande de vérification a été envoyée à l'administrateur. Vous serez notifié dès qu'elle sera traitée.");
    }

    public void createVerificationApproved(int userId) {
        insert(userId, null, "system",
               "✅ Compte vérifié !",
               "Félicitations ! Votre compte a été vérifié par notre équipe. Vous avez maintenant accès à toutes les fonctionnalités.");
    }

    public void createRoleSelected(int userId, String role) {
        boolean isMedecin = "medecin".equalsIgnoreCase(role);
        String titre = isMedecin ? "🩺 Compte médecin activé" : "👤 Compte patient activé";
        String message = isMedecin
            ? "Votre compte a été configuré en tant que médecin. Une vérification par notre équipe administrative est requise avant activation complète."
            : "Votre compte patient est prêt. Complétez votre profil pour débloquer toutes les fonctionnalités.";
        insert(userId, null, "system", titre, message);
    }

    public void createDonationConfirmed(int userId, String causeNom, double montant) {
        insert(userId, null, "donation",
               "✅ Don confirmé !",
               String.format("Votre don de %.0f DT pour la cause \"%s\" a été confirmé par l'administrateur. Merci pour votre générosité !", montant, causeNom));
    }

    public int getUnreadCount(int userId) {
        String q = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Erreur getUnreadCount: " + e.getMessage());
        }
        return 0;
    }

    public List<Notification> getAll(int userId) {
        List<Notification> list = new ArrayList<>();
        String q = "SELECT * FROM notification WHERE user_id = ? ORDER BY created_at DESC LIMIT 25";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Notification(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getObject("medecin_id", Integer.class),
                    rs.getString("type"),
                    rs.getString("titre"),
                    rs.getString("message"),
                    rs.getBoolean("is_read"),
                    rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAll notifications: " + e.getMessage());
        }
        return list;
    }

    public void markAllRead(int userId) {
        String q = "UPDATE notification SET is_read = 1 WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur markAllRead: " + e.getMessage());
        }
    }

    public int createNotificationForAllUsers(int actorId, String title, String message,
                                             String type, Integer relatedTopicId, Integer relatedCommentId) {
        String query = "SELECT id FROM user WHERE id <> ?";
        int count = 0;
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, actorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                insert(rs.getInt("id"), null, type, title, message);
                count++;
            }
        } catch (SQLException e) {
            System.out.println("Erreur createNotificationForAllUsers: " + e.getMessage());
        }
        return count;
    }

    public String findUserDisplayName(int userId) {
        String query = "SELECT prenom, nom FROM user WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String prenom = rs.getString("prenom") == null ? "" : rs.getString("prenom");
                String nom = rs.getString("nom") == null ? "" : rs.getString("nom");
                String name = (prenom + " " + nom).trim();
                return name.isEmpty() ? "Un utilisateur" : name;
            }
        } catch (SQLException e) {
            System.out.println("Erreur findUserDisplayName: " + e.getMessage());
        }
        return "Un utilisateur";
    }

    public String findTopicTitle(int topicId) {
        String query = "SELECT title FROM forum_topic WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String title = rs.getString("title");
                return title == null || title.isBlank() ? "ce sujet" : title.trim();
            }
        } catch (SQLException e) {
            System.out.println("Erreur findTopicTitle: " + e.getMessage());
        }
        return "ce sujet";
    }

    private void insert(int userId, Integer medecinId, String type, String titre, String message) {
        String q = "INSERT INTO notification (user_id, medecin_id, type, titre, message) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, userId);
            if (medecinId != null) ps.setInt(2, medecinId);
            else ps.setNull(2, Types.INTEGER);
            ps.setString(3, type);
            ps.setString(4, titre);
            ps.setString(5, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur insert notification: " + e.getMessage());
        }
    }
}
