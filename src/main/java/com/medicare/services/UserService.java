package com.medicare.services;

import com.medicare.interfaces.Crud;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserService implements Crud<User> {

    private final Connection cnx;

    public UserService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public User login(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
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

    public boolean register(User user) {
        if (emailExists(user.getEmail())) {
            return false;
        }
        add(user);
        return user.getId() > 0;
    }

    public boolean emailExists(String email) {
        return emailExists(email, null);
    }

    public boolean emailExists(String email, Integer excludeUserId) {
        String query = "SELECT id FROM user WHERE email = ?";
        if (excludeUserId != null) {
            query += " AND id <> ?";
        }

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            if (excludeUserId != null) {
                ps.setInt(2, excludeUserId);
            }
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Erreur emailExists: " + e.getMessage());
        }
        return false;
    }

    public User getById(int userId) {
        String query = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getById: " + e.getMessage());
        }
        return null;
    }

    public boolean updateProfile(User user, String newPassword) {
        if (user == null || user.getId() <= 0) {
            return false;
        }
        if (emailExists(user.getEmail(), user.getId())) {
            return false;
        }

        try {
            Set<String> userColumns = getTableColumns("user");
            StringBuilder query = new StringBuilder(
                "UPDATE user SET nom = ?, prenom = ?, email = ?, numero = ?, adresse = ?, photo = ?"
            );
            boolean updatePassword = newPassword != null && !newPassword.isBlank();
            if (userColumns.contains("email_privacy")) {
                query.append(", email_privacy = ?");
            }
            if (userColumns.contains("phone_privacy")) {
                query.append(", phone_privacy = ?");
            }
            if (userColumns.contains("adress_privacy")) {
                query.append(", adress_privacy = ?");
            } else if (userColumns.contains("adresse_privacy")) {
                query.append(", adresse_privacy = ?");
            }
            query.append(updatePassword ? ", password = ?" : "");
            query.append(" WHERE id = ?");

            try (PreparedStatement ps = cnx.prepareStatement(query.toString())) {
                int index = 1;
                ps.setString(index++, user.getNom());
                ps.setString(index++, user.getPrenom());
                ps.setString(index++, user.getEmail());
                ps.setString(index++, user.getNumero());
                ps.setString(index++, user.getAdresse());
                ps.setString(index++, user.getPhoto());
                if (userColumns.contains("email_privacy")) {
                    ps.setString(index++, normalizePrivacy(user.getEmailPrivacy()));
                }
                if (userColumns.contains("phone_privacy")) {
                    ps.setString(index++, normalizePrivacy(user.getPhonePrivacy()));
                }
                if (userColumns.contains("adress_privacy") || userColumns.contains("adresse_privacy")) {
                    ps.setString(index++, normalizePrivacy(user.getAdressePrivacy()));
                }
                if (updatePassword) {
                    ps.setString(index++, hashPassword(newPassword));
                }
                ps.setInt(index, user.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur updateProfile: " + e.getMessage());
        }
        return false;
    }

    public boolean createDoctorRequest(int userId, String certificatePath, List<String> cinImagePaths) {
        if (userId <= 0 || certificatePath == null || certificatePath.isBlank() || cinImagePaths == null || cinImagePaths.size() != 2) {
            return false;
        }

        try {
            Set<String> columns = getTableColumns("demande_medecin");
            if (columns.isEmpty() || !columns.contains("user_id")) {
                System.out.println("Table demande_medecin introuvable ou user_id absent.");
                return false;
            }

            Map<String, Object> values = new LinkedHashMap<>();
            values.put("user_id", userId);

            String certificateColumn = findFirstMatching(columns,
                "certificat_pdf", "certificat", "certificate_pdf", "certificate",
                "document_pdf", "document", "preuve_pdf", "justificatif_pdf");
            if (certificateColumn != null) {
                values.put(certificateColumn, certificatePath);
            }

            String cinFrontColumn = findFirstMatching(columns,
                "cin_recto", "cin_face_1", "cin_image_1", "cin1",
                "photo_cin_recto", "image_cin_recto", "image1");
            if (cinFrontColumn != null) {
                values.put(cinFrontColumn, cinImagePaths.get(0));
            }

            String cinBackColumn = findFirstMatching(columns,
                "cin_verso", "cin_face_2", "cin_image_2", "cin2",
                "photo_cin_verso", "image_cin_verso", "image2");
            if (cinBackColumn != null) {
                values.put(cinBackColumn, cinImagePaths.get(1));
            }

            String statusColumn = findFirstMatching(columns, "statut", "status", "etat");
            if (statusColumn != null) {
                values.put(statusColumn, "en_attente");
            }

            String query = buildInsertQuery("demande_medecin", values.keySet());
            try (PreparedStatement ps = cnx.prepareStatement(query)) {
                int index = 1;
                for (Object value : values.values()) {
                    ps.setObject(index++, value);
                }
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur createDoctorRequest: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void add(User user) {
        String query = "INSERT INTO user (nom, prenom, email, password, numero, adresse, photo, roles, is_verified) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, hashPassword(user.getPassword()));
            ps.setString(5, user.getNumero());
            ps.setString(6, user.getAdresse());
            ps.setString(7, user.getPhoto());
            ps.setString(8, normalizeRoles(user.getRoles()));
            ps.setBoolean(9, user.isVerified());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int userId = keys.getInt(1);
                user.setId(userId);
                createPatientIfMissing(userId);
            }
        } catch (SQLException e) {
            System.out.println("Erreur add user: " + e.getMessage());
        }
    }

    @Override
    public void update(User user) {
        boolean updatePassword = user.getPassword() != null && !user.getPassword().isBlank();
        StringBuilder query = new StringBuilder(
            "UPDATE user SET nom = ?, prenom = ?, email = ?, numero = ?, adresse = ?, photo = ?, roles = ?, is_verified = ?"
        );
        if (updatePassword) {
            query.append(", password = ?");
        }
        query.append(" WHERE id = ?");

        try (PreparedStatement ps = cnx.prepareStatement(query.toString())) {
            int index = 1;
            ps.setString(index++, user.getNom());
            ps.setString(index++, user.getPrenom());
            ps.setString(index++, user.getEmail());
            ps.setString(index++, user.getNumero());
            ps.setString(index++, user.getAdresse());
            ps.setString(index++, user.getPhoto());
            ps.setString(index++, normalizeRoles(user.getRoles()));
            ps.setBoolean(index++, user.isVerified());
            if (updatePassword) {
                ps.setString(index++, hashPassword(user.getPassword()));
            }
            ps.setInt(index, user.getId());
            ps.executeUpdate();
            createPatientIfMissing(user.getId());
        } catch (SQLException e) {
            System.out.println("Erreur update user: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        deleteUser(id);
    }

    @Override
    public List<User> getAll() {
        List<User> list = new ArrayList<>();
        String query = "SELECT * FROM user ORDER BY id DESC";
        try (Statement st = cnx.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAllUsers: " + e.getMessage());
        }
        return list;
    }

    public List<User> getAllUsers() {
        return getAll();
    }

    public boolean toggleVerified(int userId, boolean verified) {
        String query = "UPDATE user SET is_verified = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
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
        try {
            cnx.createStatement().executeUpdate("DELETE FROM rendez_vous WHERE patient_id IN (SELECT id FROM patient WHERE user_id = " + userId + ")");
            cnx.createStatement().executeUpdate("DELETE FROM rendez_vous WHERE medecin_id IN (SELECT id FROM medecin WHERE user_id = " + userId + ")");
            cnx.createStatement().executeUpdate("DELETE FROM disponibilite WHERE medecin_id IN (SELECT id FROM medecin WHERE user_id = " + userId + ")");
            cnx.createStatement().executeUpdate("DELETE FROM patient WHERE user_id = " + userId);
            cnx.createStatement().executeUpdate("DELETE FROM medecin WHERE user_id = " + userId);
            cnx.createStatement().executeUpdate("DELETE FROM demande_medecin WHERE user_id = " + userId);

            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM user WHERE id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur deleteUser: " + e.getMessage());
        }
        return false;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setNumero(rs.getString("numero"));
        user.setAdresse(rs.getString("adresse"));
        user.setPhoto(rs.getString("photo"));
        user.setRoles(rs.getString("roles"));
        ResultSetMetaData meta = rs.getMetaData();
        if (hasColumn(meta, "email_privacy")) {
            user.setEmailPrivacy(rs.getString("email_privacy"));
        }
        if (hasColumn(meta, "phone_privacy")) {
            user.setPhonePrivacy(rs.getString("phone_privacy"));
        }
        if (hasColumn(meta, "adress_privacy")) {
            user.setAdressePrivacy(rs.getString("adress_privacy"));
        } else if (hasColumn(meta, "adresse_privacy")) {
            user.setAdressePrivacy(rs.getString("adresse_privacy"));
        }
        user.setIsVerified(rs.getBoolean("is_verified"));
        return user;
    }

    private void createPatientIfMissing(int userId) throws SQLException {
        try (PreparedStatement check = cnx.prepareStatement("SELECT id FROM patient WHERE user_id = ?")) {
            check.setInt(1, userId);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                return;
            }
        }

        try (PreparedStatement ps = cnx.prepareStatement("INSERT INTO patient (user_id) VALUES (?)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private String hashPassword(String password) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(13));
        return hash.replace("$2a$", "$2y$");
    }

    private String normalizeRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return "[\"ROLE_USER\"]";
        }
        return roles;
    }

    private String normalizePrivacy(String privacy) {
        if (privacy == null || privacy.isBlank()) {
            return "private";
        }
        return "public".equalsIgnoreCase(privacy) ? "public" : "private";
    }

    private Set<String> getTableColumns(String tableName) throws SQLException {
        Set<String> columns = new LinkedHashSet<>();
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM " + tableName + " WHERE 1 = 0")) {
            ResultSetMetaData meta = ps.executeQuery().getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnName(i).toLowerCase());
            }
        }
        return columns;
    }

    private String findFirstMatching(Set<String> columns, String... candidates) {
        for (String candidate : candidates) {
            if (columns.contains(candidate.toLowerCase())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean hasColumn(ResultSetMetaData meta, String columnName) throws SQLException {
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(meta.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    private String buildInsertQuery(String tableName, Set<String> columns) {
        StringBuilder query = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder(" VALUES (");
        int i = 0;
        for (String column : columns) {
            if (i > 0) {
                query.append(", ");
                values.append(", ");
            }
            query.append(column);
            values.append("?");
            i++;
        }
        query.append(")");
        values.append(")");
        return query.append(values).toString();
    }
}
