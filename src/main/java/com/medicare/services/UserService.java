package com.medicare.services;

import com.medicare.models.User;
import com.medicare.utils.MyConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    public User login(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ? LIMIT 1";

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, email == null ? null : email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && passwordMatches(password, rs.getString("password"))) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur login: " + e.getMessage());
        }

        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String query = "SELECT * FROM user ORDER BY id DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("Erreur findAll users: " + e.getMessage());
        }

        return list;
    }

    public boolean addUser(User user) {
        try {
            Integer createdUserId = insertUser(getConnection(), user);
            if (createdUserId == null) {
                return false;
            }

            user.setId(createdUserId);
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur addUser: " + e.getMessage());
        }
        return false;
    }

    public boolean register(User user) {
        Connection connection = null;
        boolean initialAutoCommit = true;

        try {
            connection = getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            Integer createdUserId = insertUser(connection, user);
            if (createdUserId == null) {
                connection.rollback();
                return false;
            }

            user.setId(createdUserId);

            if (shouldCreatePatient(user)) {
                try (PreparedStatement psPatient = connection.prepareStatement(
                        "INSERT INTO patient (user_id) VALUES (?)")) {
                    psPatient.setInt(1, createdUserId);
                    psPatient.executeUpdate();
                    System.out.println("Patient cree pour user_id=" + createdUserId);
                }
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            System.out.println("Erreur register: " + e.getMessage());
        } finally {
            resetAutoCommitQuietly(connection, initialAutoCommit);
        }
        return false;
    }

    public boolean emailExists(String email) {
        try {
            return emailExists(getConnection(), email);
        } catch (SQLException e) {
            System.out.println("Erreur emailExists: " + e.getMessage());
        }
        return false;
    }

    public List<User> getAllUsers() {
        return findAll();
    }

    public boolean toggleVerified(int userId, boolean verified) {
        String query = "UPDATE user SET is_verified = ? WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setBoolean(1, verified);
            ps.setInt(2, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur toggleVerified: " + e.getMessage());
        }

        return false;
    }

    public boolean deleteUser(int userId) {
        Connection connection = null;
        boolean initialAutoCommit = true;

        try {
            connection = getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            executeDelete(connection, "DELETE FROM rendez_vous WHERE patient_id IN (SELECT id FROM patient WHERE user_id = ?)", userId);
            executeDelete(connection, "DELETE FROM rendez_vous WHERE medecin_id IN (SELECT id FROM medecin WHERE user_id = ?)", userId);
            executeDelete(connection, "DELETE FROM disponibilite WHERE medecin_id IN (SELECT id FROM medecin WHERE user_id = ?)", userId);
            executeDelete(connection, "DELETE FROM patient WHERE user_id = ?", userId);
            executeDelete(connection, "DELETE FROM medecin WHERE user_id = ?", userId);
            executeDelete(connection, "DELETE FROM demande_medecin WHERE user_id = ?", userId);
            executeDelete(connection, "DELETE FROM user WHERE id = ?", userId);

            connection.commit();
            return true;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            System.out.println("Erreur deleteUser: " + e.getMessage());
        } finally {
            resetAutoCommitQuietly(connection, initialAutoCommit);
        }

        return false;
    }

    private Integer insertUser(Connection connection, User user) throws SQLException {
        if (user == null || isBlank(user.getEmail()) || isBlank(user.getPassword())) {
            return null;
        }

        if (emailExists(connection, user.getEmail())) {
            return null;
        }

        String query = "INSERT INTO user (nom, prenom, email, password, numero, roles, is_verified) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, safeText(user.getNom()));
            ps.setString(2, safeText(user.getPrenom()));
            ps.setString(3, user.getEmail().trim());
            ps.setString(4, toSymfonyCompatibleHash(user.getPassword()));
            ps.setString(5, safeText(user.getNumero()));
            ps.setString(6, defaultRoles(user.getRoles()));
            ps.setBoolean(7, user.isVerified());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return null;
    }

    private boolean emailExists(Connection connection, String email) throws SQLException {
        String query = "SELECT id FROM user WHERE email = ? LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, email == null ? null : email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setNumero(rs.getString("numero"));
        user.setRoles(rs.getString("roles"));
        user.setIsVerified(rs.getBoolean("is_verified"));
        if (hasColumn(rs, "adresse")) {
            user.setAdresse(rs.getString("adresse"));
        }
        if (hasColumn(rs, "photo")) {
            user.setPhoto(rs.getString("photo"));
        }
        return user;
    }

    private boolean passwordMatches(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (looksLikeBcryptHash(storedPassword)) {
            try {
                return BCrypt.checkpw(plainPassword, normalizeBcryptPrefix(storedPassword));
            } catch (IllegalArgumentException e) {
                System.out.println("Hash bcrypt invalide: " + e.getMessage());
                return false;
            }
        }

        return storedPassword.equals(plainPassword);
    }

    private String toSymfonyCompatibleHash(String password) {
        if (looksLikeBcryptHash(password)) {
            return "$2y$" + password.substring(4);
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        return "$2y$" + hash.substring(4);
    }

    private boolean looksLikeBcryptHash(String password) {
        return password != null
                && password.length() >= 4
                && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private String normalizeBcryptPrefix(String hash) {
        if (hash.startsWith("$2y$") || hash.startsWith("$2b$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }

    private boolean shouldCreatePatient(User user) {
        String roles = defaultRoles(user.getRoles());
        return roles.contains("ROLE_USER") && !roles.contains("ROLE_MEDECIN") && !roles.contains("ROLE_ADMIN");
    }

    private String defaultRoles(String roles) {
        return isBlank(roles) ? "[\"ROLE_USER\"]" : roles;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Connection getConnection() throws SQLException {
        Connection connection = MyConnection.getInstance().getConnection();
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connexion MySQL indisponible.");
        }
        return connection;
    }

    private void executeDelete(Connection connection, String query, int userId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } catch (SQLException e) {
            System.out.println("Erreur rollback deleteUser: " + e.getMessage());
        }
    }

    private void resetAutoCommitQuietly(Connection connection, boolean autoCommit) {
        if (connection == null) {
            return;
        }

        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            System.out.println("Erreur reset autoCommit: " + e.getMessage());
        }
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }
}
