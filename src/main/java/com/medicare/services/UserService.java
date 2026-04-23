package com.medicare.services;

import com.medicare.models.User;
import com.medicare.utils.MyConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class UserService {

    private final Connection cnx;

    public UserService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    /**
     * Connexion : cherche l'utilisateur par email puis vérifie le hash bcrypt.
     * Symfony stocke les hash avec le préfixe $2y$, jBCrypt attend $2a$ → on remplace.
     */
    public User login(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                // Symfony utilise $2y$, jBCrypt utilise $2a$ — compatibles, juste le préfixe change
                String javaHash = storedHash.replace("$2y$", "$2a$");

                if (BCrypt.checkpw(password, javaHash)) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur login: " + e.getMessage());
        }
        return null;
    }

    /**
     * Inscription : crée un nouveau compte avec hash bcrypt compatible Symfony.
     */
    public boolean register(User user) {
        String query = "INSERT INTO user (nom, prenom, email, password, numero, adresse, photo, roles, is_verified) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            // Vérifier si l'email existe déjà
            if (emailExists(user.getEmail())) {
                return false;
            }

            // Hash bcrypt avec préfixe $2y$ pour rester compatible Symfony
            String hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(13));
            hash = hash.replace("$2a$", "$2y$");

            PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, hash);
            ps.setString(5, user.getNumero());
            ps.setString(6, user.getAdresse());
            ps.setString(7, user.getPhoto());
            ps.setString(8, "[\"ROLE_USER\"]");
            ps.setBoolean(9, false);
            ps.executeUpdate();

            // Récupère l'id du user créé et crée l'entrée patient
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int userId = keys.getInt(1);
                PreparedStatement psPatient = cnx.prepareStatement("INSERT INTO patient (user_id) VALUES (?)");
                psPatient.setInt(1, userId);
                psPatient.executeUpdate();
                System.out.println("Patient cree pour user_id=" + userId);
            }

            return true;
        } catch (SQLException e) {
            System.out.println("Erreur register: " + e.getMessage());
        }
        return false;
    }

    public boolean emailExists(String email) {
        String query = "SELECT id FROM user WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Erreur emailExists: " + e.getMessage());
        }
        return false;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setNumero(rs.getString("numero"));
        u.setAdresse(rs.getString("adresse"));
        u.setPhoto(rs.getString("photo"));
        u.setRoles(rs.getString("roles"));
        u.setIsVerified(rs.getBoolean("is_verified"));
        return u;
    }

    public User getUserById(int id) {
        String q = "SELECT * FROM user WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) { System.out.println("Erreur getUserById: " + e.getMessage()); }
        return null;
    }

    public User getUserByPatientId(int patientId) {
        String q = "SELECT u.* FROM user u JOIN patient p ON p.user_id = u.id WHERE p.id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) { System.out.println("Erreur getUserByPatientId: " + e.getMessage()); }
        return null;
    }

    public User getUserByMedecinId(int medecinId) {
        String q = "SELECT u.* FROM user u JOIN medecin m ON m.user_id = u.id WHERE m.id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) { System.out.println("Erreur getUserByMedecinId: " + e.getMessage()); }
        return null;
    }

    // ==================== ADMIN ====================

    public java.util.List<User> getAllUsers() {
        java.util.List<User> list = new java.util.ArrayList<>();
        String q = "SELECT * FROM user ORDER BY id DESC";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(q);
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) { System.out.println("Erreur getAllUsers: " + e.getMessage()); }
        return list;
    }

    public boolean toggleVerified(int userId, boolean verified) {
        String q = "UPDATE user SET is_verified = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setBoolean(1, verified);
            ps.setInt(2, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur toggleVerified: " + e.getMessage()); }
        return false;
    }

    public boolean deleteUser(int userId) {
        try {
            // Supprimer d'abord les entrées liées
            cnx.createStatement().executeUpdate("DELETE FROM rendez_vous WHERE patient_id IN (SELECT id FROM patient WHERE user_id = " + userId + ")");
            cnx.createStatement().executeUpdate("DELETE FROM rendez_vous WHERE medecin_id IN (SELECT id FROM medecin WHERE user_id = " + userId + ")");
            cnx.createStatement().executeUpdate("DELETE FROM disponibilite WHERE medecin_id IN (SELECT id FROM medecin WHERE user_id = " + userId + ")");
            cnx.createStatement().executeUpdate("DELETE FROM patient WHERE user_id = " + userId);
            cnx.createStatement().executeUpdate("DELETE FROM medecin WHERE user_id = " + userId);
            cnx.createStatement().executeUpdate("DELETE FROM demande_medecin WHERE user_id = " + userId);

            PreparedStatement ps = cnx.prepareStatement("DELETE FROM user WHERE id = ?");
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur deleteUser: " + e.getMessage()); }
        return false;
    }
}
