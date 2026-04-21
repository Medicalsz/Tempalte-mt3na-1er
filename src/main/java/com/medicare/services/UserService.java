package com.medicare.services;

import com.medicare.interfaces.Crud;
import com.medicare.models.LoginResult;
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
    private static final String DEFAULT_ADMIN_EMAIL = "admin@medicare.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String DEFAULT_ADMIN_ROLES = "[\"ROLE_ADMIN\"]";

    private final Connection cnx;

    public UserService() {
        cnx = MyConnection.getInstance().getCnx();
        ensureDefaultAdminAccount();
    }

    public User login(String email, String password) {
        LoginResult result = loginByAccountType(email, password);
        return result != null ? result.getUser() : null;
    }

    public LoginResult loginByAccountType(String email, String password) {
        LoginResult adminResult = loginFromAdminTable(email, password);
        if (adminResult != null) {
            return adminResult;
        }

        LoginResult userResult = loginFromUserTable(email, password);
        if (userResult != null) {
            return userResult;
        }

        return loginFromMedecinTable(email, password);
    }

    private LoginResult loginFromAdminTable(String email, String password) {
        Set<String> columns = getTableColumnsSafely("admin");
        if (columns.isEmpty() || !columns.contains("email") || !columns.contains("password")) {
            return null;
        }

        String query = "SELECT * FROM admin WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && passwordMatches(password, rs.getString("password"))) {
                User user = new User();
                user.setId(columns.contains("id") ? rs.getInt("id") : 0);
                user.setNom(readFirstAvailable(rs, columns, "nom", "last_name", "lastname", "name", "username"));
                user.setPrenom(readFirstAvailable(rs, columns, "prenom", "first_name", "firstname"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setNumero(readFirstAvailable(rs, columns, "numero", "phone", "telephone"));
                user.setAdresse(readFirstAvailable(rs, columns, "adresse", "address"));
                user.setPhoto(readFirstAvailable(rs, columns, "photo", "avatar", "image"));
                user.setRoles(DEFAULT_ADMIN_ROLES);
                user.setIsVerified(true);
                return new LoginResult(user, "admin", -1);
            }
        } catch (SQLException e) {
            System.out.println("Erreur login admin: " + e.getMessage());
        }
        return null;
    }

    private LoginResult loginFromUserTable(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && passwordMatches(password, rs.getString("password"))) {
                return new LoginResult(mapUser(rs), "user", -1);
            }
        } catch (SQLException e) {
            System.out.println("Erreur login user: " + e.getMessage());
        }
        return null;
    }

    private LoginResult loginFromMedecinTable(String email, String password) {
        Set<String> columns = getTableColumnsSafely("medecin");
        if (columns.isEmpty()) {
            return null;
        }

        if (columns.contains("user_id")) {
            String query = "SELECT m.id AS medecin_id, u.* FROM medecin m " +
                "JOIN user u ON m.user_id = u.id WHERE u.email = ?";
            try (PreparedStatement ps = cnx.prepareStatement(query)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && passwordMatches(password, rs.getString("password"))) {
                    User user = mapUser(rs);
                    if (user.getRoles() == null || !user.getRoles().contains("ROLE_MEDECIN")) {
                        user.setRoles("[\"ROLE_MEDECIN\"]");
                    }
                    return new LoginResult(user, "medecin", rs.getInt("medecin_id"));
                }
            } catch (SQLException e) {
                System.out.println("Erreur login medecin via user: " + e.getMessage());
            }
        }

        if (!columns.contains("email") || !columns.contains("password")) {
            return null;
        }

        String query = "SELECT * FROM medecin WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && passwordMatches(password, rs.getString("password"))) {
                User user = new User();
                user.setId(columns.contains("id") ? rs.getInt("id") : 0);
                user.setNom(readFirstAvailable(rs, columns, "nom", "last_name", "lastname", "name"));
                user.setPrenom(readFirstAvailable(rs, columns, "prenom", "first_name", "firstname"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setNumero(readFirstAvailable(rs, columns, "numero", "phone", "telephone"));
                user.setAdresse(readFirstAvailable(rs, columns, "adresse", "address"));
                user.setPhoto(readFirstAvailable(rs, columns, "photo", "avatar", "image"));
                user.setRoles("[\"ROLE_MEDECIN\"]");
                user.setIsVerified(true);
                int medecinId = columns.contains("id") ? rs.getInt("id") : -1;
                return new LoginResult(user, "medecin", medecinId);
            }
        } catch (SQLException e) {
            System.out.println("Erreur login medecin: " + e.getMessage());
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

    private void ensureDefaultAdminAccount() {
        if (cnx == null) {
            return;
        }

        ensureDefaultAdminInAdminTable();
        ensureDefaultAdminInUserTable();
    }

    private void ensureDefaultAdminInUserTable() {
        String selectQuery = "SELECT id, password, roles, is_verified FROM user WHERE email = ?";
        try (PreparedStatement select = cnx.prepareStatement(selectQuery)) {
            select.setString(1, DEFAULT_ADMIN_EMAIL);
            ResultSet rs = select.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String storedHash = rs.getString("password");
                String storedRoles = rs.getString("roles");
                boolean verified = rs.getBoolean("is_verified");
                boolean passwordMatches = storedHash != null
                    && BCrypt.checkpw(DEFAULT_ADMIN_PASSWORD, storedHash.replace("$2y$", "$2a$"));
                boolean rolesMatch = storedRoles != null && storedRoles.contains("ROLE_ADMIN");

                if (!passwordMatches || !rolesMatch || !verified) {
                    try (PreparedStatement update = cnx.prepareStatement(
                        "UPDATE user SET nom = ?, prenom = ?, password = ?, roles = ?, is_verified = ? WHERE id = ?"
                    )) {
                        update.setString(1, "Admin");
                        update.setString(2, "Medicare");
                        update.setString(3, hashPassword(DEFAULT_ADMIN_PASSWORD));
                        update.setString(4, DEFAULT_ADMIN_ROLES);
                        update.setBoolean(5, true);
                        update.setInt(6, userId);
                        update.executeUpdate();
                    }
                }
                return;
            }

            try (PreparedStatement insert = cnx.prepareStatement(
                "INSERT INTO user (nom, prenom, email, password, numero, adresse, photo, roles, is_verified) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )) {
                insert.setString(1, "Admin");
                insert.setString(2, "Medicare");
                insert.setString(3, DEFAULT_ADMIN_EMAIL);
                insert.setString(4, hashPassword(DEFAULT_ADMIN_PASSWORD));
                insert.setString(5, "");
                insert.setString(6, "");
                insert.setString(7, null);
                insert.setString(8, DEFAULT_ADMIN_ROLES);
                insert.setBoolean(9, true);
                insert.executeUpdate();

                ResultSet keys = insert.getGeneratedKeys();
                if (keys.next()) {
                    createPatientIfMissing(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur ensureDefaultAdminAccount: " + e.getMessage());
        }
    }

    private void ensureDefaultAdminInAdminTable() {
        Set<String> columns = getTableColumnsSafely("admin");
        if (columns.isEmpty() || !columns.contains("email") || !columns.contains("password")) {
            return;
        }

        try (PreparedStatement select = cnx.prepareStatement("SELECT * FROM admin WHERE email = ?")) {
            select.setString(1, DEFAULT_ADMIN_EMAIL);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                if (!passwordMatches(DEFAULT_ADMIN_PASSWORD, rs.getString("password"))) {
                    String updateQuery = "UPDATE admin SET password = ? WHERE email = ?";
                    try (PreparedStatement update = cnx.prepareStatement(updateQuery)) {
                        update.setString(1, hashPassword(DEFAULT_ADMIN_PASSWORD));
                        update.setString(2, DEFAULT_ADMIN_EMAIL);
                        update.executeUpdate();
                    }
                }
                return;
            }

            StringBuilder query = new StringBuilder("INSERT INTO admin (");
            StringBuilder values = new StringBuilder(" VALUES (");
            List<Object> params = new ArrayList<>();
            boolean first = true;

            first = appendColumnValue(query, values, params, first, "email", DEFAULT_ADMIN_EMAIL);
            first = appendColumnValue(query, values, params, first, "password", hashPassword(DEFAULT_ADMIN_PASSWORD));
            if (columns.contains("nom")) {
                first = appendColumnValue(query, values, params, first, "nom", "Admin");
            }
            if (columns.contains("prenom")) {
                first = appendColumnValue(query, values, params, first, "prenom", "Medicare");
            }
            if (columns.contains("role")) {
                first = appendColumnValue(query, values, params, first, "role", "ROLE_ADMIN");
            }
            if (columns.contains("roles")) {
                first = appendColumnValue(query, values, params, first, "roles", DEFAULT_ADMIN_ROLES);
            }
            if (columns.contains("is_verified")) {
                first = appendColumnValue(query, values, params, first, "is_verified", true);
            }

            query.append(")");
            values.append(")");
            query.append(values);

            try (PreparedStatement insert = cnx.prepareStatement(query.toString())) {
                int index = 1;
                for (Object param : params) {
                    insert.setObject(index++, param);
                }
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Erreur ensureDefaultAdminInAdminTable: " + e.getMessage());
        }
    }

    private String hashPassword(String password) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(13));
        return hash.replace("$2a$", "$2y$");
    }

    private boolean passwordMatches(String plainPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, storedHash.replace("$2y$", "$2a$"));
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

    private Set<String> getTableColumnsSafely(String tableName) {
        try {
            return getTableColumns(tableName);
        } catch (SQLException e) {
            return new LinkedHashSet<>();
        }
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

    private String readFirstAvailable(ResultSet rs, Set<String> columns, String... candidates) throws SQLException {
        for (String candidate : candidates) {
            if (columns.contains(candidate.toLowerCase())) {
                return rs.getString(candidate);
            }
        }
        return null;
    }

    private boolean appendColumnValue(
        StringBuilder query,
        StringBuilder values,
        List<Object> params,
        boolean first,
        String column,
        Object value
    ) {
        if (!first) {
            query.append(", ");
            values.append(", ");
        }
        query.append(column);
        values.append("?");
        params.add(value);
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
