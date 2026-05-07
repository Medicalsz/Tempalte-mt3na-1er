package com.medicare.services;

import com.medicare.interfaces.Crud;
import com.medicare.models.LoginResult;
import com.medicare.models.User;
import com.medicare.utils.MyConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.*;

public class UserService implements Crud<User> {
    private static final String DEFAULT_ADMIN_EMAIL = "admin@medicare.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String DEFAULT_ADMIN_ROLES = "[\"ROLE_ADMIN\"]";

    private final Connection cnx;
    private String lastRegisterErrorMessage;

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
        if (adminResult != null) return adminResult;
        LoginResult userResult = loginFromUserTable(email, password);
        if (userResult != null) return userResult;
        return loginFromMedecinTable(email, password);
    }

    private LoginResult loginFromAdminTable(String email, String password) {
        Set<String> columns = getTableColumnsSafely("admin");
        if (columns.isEmpty() || !columns.contains("email") || !columns.contains("password")) return null;
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
        } catch (SQLException e) { System.out.println("Erreur login admin: " + e.getMessage()); }
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
        } catch (SQLException e) { System.out.println("Erreur login user: " + e.getMessage()); }
        return null;
    }

    private LoginResult loginFromMedecinTable(String email, String password) {
        Set<String> columns = getTableColumnsSafely("medecin");
        if (columns.isEmpty()) return null;

        // Path 1: medecin linked to user table via user_id (preferred)
        if (columns.contains("user_id")) {
            String query = "SELECT m.id AS medecin_id, u.* FROM medecin m JOIN user u ON m.user_id = u.id WHERE u.email = ?";
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
            } catch (SQLException e) { System.out.println("Erreur login medecin via user: " + e.getMessage()); }
        }

        // Path 2: standalone medecin row (has its own email + password, user_id is NULL)
        if (!columns.contains("email") || !columns.contains("password")) return null;
        String query = "SELECT * FROM medecin WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedPw = rs.getString("password");
                if (storedPw != null && !storedPw.isBlank() && passwordMatches(password, storedPw)) {
                    User user = new User();
                    user.setId(columns.contains("id") ? rs.getInt("id") : 0);
                    user.setNom(readFirstAvailable(rs, columns, "nom", "last_name", "lastname", "name"));
                    user.setPrenom(readFirstAvailable(rs, columns, "prenom", "first_name", "firstname"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(storedPw);
                    user.setNumero(readFirstAvailable(rs, columns, "numero", "phone", "telephone"));
                    user.setAdresse(readFirstAvailable(rs, columns, "adresse", "address"));
                    user.setPhoto(readFirstAvailable(rs, columns, "photo", "avatar", "image"));
                    user.setRoles("[\"ROLE_MEDECIN\"]");
                    user.setIsVerified(columns.contains("isverified") || columns.contains("is_verified")
                            ? rs.getBoolean(columns.contains("isVerified") ? "isVerified" : "is_verified") : true);
                    int medecinId = columns.contains("id") ? rs.getInt("id") : -1;
                    return new LoginResult(user, "medecin", medecinId);
                }
            }
        } catch (SQLException e) { System.out.println("Erreur login medecin standalone: " + e.getMessage()); }
        return null;
    }

    public boolean register(User user) {
        lastRegisterErrorMessage = null;
        if (emailExists(user.getEmail())) {
            lastRegisterErrorMessage = "Cet email est deja utilise.";
            return false;
        }
        return insertUser(user);
    }

    public String getLastRegisterErrorMessage() { return lastRegisterErrorMessage; }

    public boolean emailExists(String email) { return emailExists(email, null); }

    public boolean emailExists(String email, Integer excludeUserId) {
        String query = "SELECT id FROM user WHERE email = ?";
        if (excludeUserId != null) query += " AND id <> ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            if (excludeUserId != null) ps.setInt(2, excludeUserId);
            return ps.executeQuery().next();
        } catch (SQLException e) { System.out.println("Erreur emailExists: " + e.getMessage()); }
        return false;
    }

    public User getById(int userId) {
        String query = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) { System.out.println("Erreur getById: " + e.getMessage()); }
        return null;
    }

    public User findByEmail(String email) {
        String query = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) { System.out.println("Erreur findByEmail: " + e.getMessage()); }
        return null;
    }

    public LoginResult loginWithoutPassword(String email) {
        // Only login users from the user table via Google (or admin table if matching email)
        // Let's first check admin table
        Set<String> adminColumns = getTableColumnsSafely("admin");
        if (!adminColumns.isEmpty() && adminColumns.contains("email")) {
            String q = "SELECT * FROM admin WHERE email = ?";
            try (PreparedStatement ps = cnx.prepareStatement(q)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    User user = new User();
                    user.setId(adminColumns.contains("id") ? rs.getInt("id") : 0);
                    user.setNom(readFirstAvailable(rs, adminColumns, "nom", "last_name", "lastname", "name", "username"));
                    user.setPrenom(readFirstAvailable(rs, adminColumns, "prenom", "first_name", "firstname"));
                    user.setEmail(rs.getString("email"));
                    user.setRoles(DEFAULT_ADMIN_ROLES);
                    user.setIsVerified(true);
                    return new LoginResult(user, "admin", -1);
                }
            } catch (SQLException e) { System.out.println("Erreur admin loginWithoutPassword: " + e.getMessage()); }
        }

        // Check user table
        User user = findByEmail(email);
        if (user != null) {
            // Also check if they are a medecin
            Set<String> medecinColumns = getTableColumnsSafely("medecin");
            if (!medecinColumns.isEmpty() && medecinColumns.contains("user_id")) {
                String q = "SELECT id FROM medecin WHERE user_id = ?";
                try (PreparedStatement ps = cnx.prepareStatement(q)) {
                    ps.setInt(1, user.getId());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        if (user.getRoles() == null || !user.getRoles().contains("ROLE_MEDECIN")) {
                            user.setRoles("[\"ROLE_MEDECIN\"]");
                        }
                        return new LoginResult(user, "medecin", rs.getInt("id"));
                    }
                } catch (SQLException e) { System.out.println("Erreur medecin loginWithoutPassword: " + e.getMessage()); }
            }
            return new LoginResult(user, "user", -1);
        }

        return null;
    }

    public boolean updateProfile(User user, String newPassword) {
        if (user == null || user.getId() <= 0) return false;
        if (emailExists(user.getEmail(), user.getId())) return false;
        try {
            StringBuilder query = new StringBuilder(
                "UPDATE user SET nom = ?, prenom = ?, email = ?, numero = ?, adresse = ?, photo = ?"
            );
            boolean updatePassword = newPassword != null && !newPassword.isBlank();
            Set<String> userColumns = getTableColumns("user");
            if (userColumns.contains("privacy_level")) query.append(", privacy_level = ?");
            if (userColumns.contains("city")) query.append(", city = ?");
            if (userColumns.contains("latitude")) query.append(", latitude = ?");
            if (userColumns.contains("longitude")) query.append(", longitude = ?");
            if (userColumns.contains("gender")) query.append(", gender = ?");
            if (userColumns.contains("blood_type")) query.append(", blood_type = ?");
            if (userColumns.contains("allergies")) query.append(", allergies = ?");
            if (updatePassword) query.append(", password = ?");
            query.append(" WHERE id = ?");

            try (PreparedStatement ps = cnx.prepareStatement(query.toString())) {
                int index = 1;
                ps.setString(index++, user.getNom());
                ps.setString(index++, user.getPrenom());
                ps.setString(index++, user.getEmail());
                ps.setString(index++, user.getNumero());
                ps.setString(index++, user.getAdresse());
                ps.setString(index++, user.getPhoto());
                if (userColumns.contains("privacy_level")) ps.setString(index++, user.getPrivacyLevel() != null ? user.getPrivacyLevel() : "public");
                if (userColumns.contains("city")) ps.setString(index++, user.getCity());
                if (userColumns.contains("latitude")) {
                    if (user.getLatitude() == null) ps.setNull(index++, Types.DOUBLE);
                    else ps.setDouble(index++, user.getLatitude());
                }
                if (userColumns.contains("longitude")) {
                    if (user.getLongitude() == null) ps.setNull(index++, Types.DOUBLE);
                    else ps.setDouble(index++, user.getLongitude());
                }
                if (userColumns.contains("gender")) ps.setString(index++, user.getGender());
                if (userColumns.contains("blood_type")) ps.setString(index++, user.getBloodType());
                if (userColumns.contains("allergies")) ps.setString(index++, user.getAllergies());
                if (updatePassword) ps.setString(index++, hashPassword(newPassword));
                ps.setInt(index, user.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) { System.out.println("Erreur updateProfile: " + e.getMessage()); }
        return false;
    }

    // --- Profile completion helpers ---
    public void incrementSkipCount(int userId) {
        String query = "UPDATE user SET skip_count = skip_count + 1 WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("Erreur incrementSkipCount: " + e.getMessage()); }
    }

    public void updateProfileCompleted(int userId, boolean completed) {
        String query = "UPDATE user SET profile_completed = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setBoolean(1, completed);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("Erreur updateProfileCompleted: " + e.getMessage()); }
    }

    public boolean createDoctorRequest(int userId, String certificatePath, List<String> cinImagePaths) {
        if (userId <= 0 || certificatePath == null || certificatePath.isBlank() || cinImagePaths == null || cinImagePaths.size() != 2) return false;
        try {
            Set<String> columns = getTableColumns("demande_medecin");
            if (columns.isEmpty() || !columns.contains("user_id")) return false;
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("user_id", userId);
            String certificateColumn = findFirstMatching(columns, "certificat_pdf", "certificat", "certificate_pdf", "certificate", "document_pdf", "document", "preuve_pdf", "justificatif_pdf");
            if (certificateColumn != null) values.put(certificateColumn, certificatePath);
            String cinFrontColumn = findFirstMatching(columns, "cin_recto", "cin_face_1", "cin_image_1", "cin1", "photo_cin_recto", "image_cin_recto", "image1");
            if (cinFrontColumn != null) values.put(cinFrontColumn, cinImagePaths.get(0));
            String cinBackColumn = findFirstMatching(columns, "cin_verso", "cin_face_2", "cin_image_2", "cin2", "photo_cin_verso", "image_cin_verso", "image2");
            if (cinBackColumn != null) values.put(cinBackColumn, cinImagePaths.get(1));
            String statusColumn = findFirstMatching(columns, "statut", "status", "etat");
            if (statusColumn != null) values.put(statusColumn, "en_attente");
            String query = buildInsertQuery("demande_medecin", values.keySet());
            try (PreparedStatement ps = cnx.prepareStatement(query)) {
                int index = 1;
                for (Object value : values.values()) ps.setObject(index++, value);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) { System.out.println("Erreur createDoctorRequest: " + e.getMessage()); }
        return false;
    }

    @Override
    public void add(User user) { insertUser(user); }

    private boolean insertUser(User user) {
        try {
            Set<String> userColumns = getTableColumns("user");
            if (userColumns.isEmpty()) { lastRegisterErrorMessage = "Table user introuvable."; return false; }
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("nom", user.getNom());
            values.put("prenom", user.getPrenom());
            if (userColumns.contains("username")) values.put("username", buildUsername(user));
            values.put("email", user.getEmail());
            values.put("password", hashPassword(user.getPassword()));
            if (userColumns.contains("numero")) values.put("numero", user.getNumero());
            if (userColumns.contains("adresse")) values.put("adresse", user.getAdresse());
            if (userColumns.contains("photo")) values.put("photo", user.getPhoto());
            if (userColumns.contains("roles")) values.put("roles", normalizeRoles(user.getRoles()));
            if (userColumns.contains("is_verified")) values.put("is_verified", user.isVerified());
            if (userColumns.contains("wants_email_notifications")) values.put("wants_email_notifications", true);
            if (userColumns.contains("profile_completed")) values.put("profile_completed", false);
            if (userColumns.contains("privacy_level")) values.put("privacy_level", "public");

            String query = buildInsertQuery("user", values.keySet());
            try (PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                int index = 1;
                for (Object value : values.values()) ps.setObject(index++, value);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) user.setId(keys.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            lastRegisterErrorMessage = buildRegisterErrorMessage(e);
            System.out.println("Erreur add user: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void update(User user) {
        boolean updatePassword = user.getPassword() != null && !user.getPassword().isBlank();
        StringBuilder query = new StringBuilder(
            "UPDATE user SET nom = ?, prenom = ?, email = ?, numero = ?, adresse = ?, photo = ?, roles = ?, is_verified = ?"
        );
        if (updatePassword) query.append(", password = ?");
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
            if (updatePassword) ps.setString(index++, hashPassword(user.getPassword()));
            ps.setInt(index, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("Erreur update user: " + e.getMessage()); }
    }

    @Override
    public void delete(int id) { deleteUser(id); }


    public boolean updateLatLng(int userId, double latitude, double longitude) {
        Set<String> cols = getTableColumnsSafely("user");
        if (!cols.contains("latitude") || !cols.contains("longitude")) return false;
        String q = "UPDATE user SET latitude = ?, longitude = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setDouble(1, latitude);
            ps.setDouble(2, longitude);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Erreur updateLatLng: " + e.getMessage()); }
        return false;
    }

    public boolean updateLocation(int id, String city, String adresse, double latitude, double longitude) {
        Set<String> userCols = getTableColumnsSafely("user");
        Set<String> medecinCols = getTableColumnsSafely("medecin");

        // Truncate to fit DB columns
        String safeCity = (city != null && city.length() > 100) ? city.substring(0, 100) : city;
        String safeAddr = (adresse != null && adresse.length() > 255) ? adresse.substring(0, 255) : adresse;

        boolean updated = false;

        // 1. Primary Update: Try ID as User ID
        if (userCols.contains("latitude") && userCols.contains("longitude")
                && userCols.contains("city") && userCols.contains("adresse")) {
            String q = "UPDATE user SET latitude = ?, longitude = ?, city = ?, adresse = ? WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(q)) {
                ps.setDouble(1, latitude); ps.setDouble(2, longitude);
                ps.setString(3, safeCity);     ps.setString(4, safeAddr);
                ps.setInt(5, id);
                if (ps.executeUpdate() > 0) updated = true;
            } catch (SQLException e) { System.out.println("Erreur updateLocation user(id): " + e.getMessage()); }
        }

        // 2. Secondary Update: Try ID as Medecin ID (for medecin coordinates)
        if (medecinCols.contains("latitude") && medecinCols.contains("longitude")) {
            String qMed = "UPDATE medecin SET latitude = ?, longitude = ? WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(qMed)) {
                ps.setDouble(1, latitude); ps.setDouble(2, longitude);
                ps.setInt(3, id);
                if (ps.executeUpdate() > 0) updated = true;
            } catch (SQLException e) { System.out.println("Erreur updateLocation medecin(id): " + e.getMessage()); }
        }

        // 3. Update linked user row by medecin.id whenever medecin has user_id
        if (medecinCols.contains("user_id")
                && userCols.contains("latitude") && userCols.contains("longitude")
                && userCols.contains("city") && userCols.contains("adresse")) {
            String qLink = "UPDATE user u JOIN medecin m ON u.id = m.user_id "
                    + "SET u.latitude = ?, u.longitude = ?, u.city = ?, u.adresse = ? WHERE m.id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(qLink)) {
                ps.setDouble(1, latitude); ps.setDouble(2, longitude);
                ps.setString(3, safeCity);     ps.setString(4, safeAddr);
                ps.setInt(5, id);
                if (ps.executeUpdate() > 0) updated = true;
            } catch (SQLException e) { System.out.println("Erreur updateLocation user(via medecin.id): " + e.getMessage()); }
        }

        // 4. Tertiary Update: Try ID as user_id linked to a medecin (for medecin coordinates)
        if (medecinCols.contains("user_id") && medecinCols.contains("latitude") && medecinCols.contains("longitude")) {
             String qUserLink = "UPDATE medecin SET latitude = ?, longitude = ? WHERE user_id = ?";
             try (PreparedStatement ps = cnx.prepareStatement(qUserLink)) {
                 ps.setDouble(1, latitude); ps.setDouble(2, longitude);
                 ps.setInt(3, id);
                 if (ps.executeUpdate() > 0) updated = true;
             } catch (SQLException e) { System.out.println("Erreur updateLocation medecin(user_id): " + e.getMessage()); }
        }

        return updated;
    }

    public boolean updateNotifPreferences(int userId, boolean wantsEmail, String privacyLevel) {
        String q = "UPDATE user SET wants_email_notifications = ?, privacy_level = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setBoolean(1, wantsEmail);
            ps.setString(2, privacyLevel != null ? privacyLevel : "public");
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Erreur updateNotifPreferences: " + e.getMessage()); }
        return false;
    }

    public static class DoctorDistance {
        private final int userId;
        private final String fullName;
        private final String city;
        private final String adresse;
        private final double latitude;
        private final double longitude;
        private final String specialite;
        private final double distanceKm;

        public DoctorDistance(int userId, String fullName, String city, String adresse,
                              double latitude, double longitude, String specialite, double distanceKm) {
            this.userId = userId;
            this.fullName = fullName;
            this.city = city;
            this.adresse = adresse;
            this.latitude = latitude;
            this.longitude = longitude;
            this.specialite = specialite;
            this.distanceKm = distanceKm;
        }

        public int getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getCity() { return city; }
        public String getAdresse() { return adresse; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getSpecialite() { return specialite; }
        public double getDistanceKm() { return distanceKm; }
    }

    public List<DoctorDistance> getNearestDoctors(int patientUserId, int k) {
        User patient = getById(patientUserId);
        if (patient == null || patient.getLatitude() == null || patient.getLongitude() == null) {
            return new ArrayList<>();
        }
        List<DoctorDistance> all = getDoctorsWithCoordinates(patientUserId, patient.getLatitude(), patient.getLongitude());
        if (k <= 0 || all.size() <= k) return all;
        return new ArrayList<>(all.subList(0, k));
    }

    public List<DoctorDistance> getDoctorsWithCoordinates(int excludeUserId, double fromLat, double fromLng) {
        List<DoctorDistance> doctors = new ArrayList<>();
        String q = "SELECT u.id AS user_id, u.nom, u.prenom, u.city, u.adresse, u.latitude, u.longitude, m.specialite "
                + "FROM user u "
                + "LEFT JOIN medecin m ON m.user_id = u.id "
                + "WHERE u.id <> ? "
                + "AND u.roles LIKE ? "
                + "AND u.latitude IS NOT NULL "
                + "AND u.longitude IS NOT NULL";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, excludeUserId);
            ps.setString(2, "%ROLE_MEDECIN%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String fullName = (rs.getString("prenom") == null ? "" : rs.getString("prenom") + " ")
                        + (rs.getString("nom") == null ? "" : rs.getString("nom"));
                String city = rs.getString("city");
                String adresse = rs.getString("adresse");
                double lat = rs.getDouble("latitude");
                double lng = rs.getDouble("longitude");
                String specialite = rs.getString("specialite");
                double distance = haversineKm(fromLat, fromLng, lat, lng);
                doctors.add(new DoctorDistance(userId, fullName.trim(), city, adresse, lat, lng, specialite, distance));
            }
        } catch (SQLException e) {
            System.out.println("Erreur getDoctorsWithCoordinates: " + e.getMessage());
        }
        doctors.sort(Comparator.comparingDouble(DoctorDistance::getDistanceKm));
        return doctors;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    public boolean updateMedecinLanguages(int userId, String languages) {
        String q = "UPDATE medecin SET languages = ? WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setString(1, languages);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Erreur updateMedecinLanguages: " + e.getMessage()); }
        return false;
    }

    public boolean linkGoogleAccount(int userId, String googleId, String accessToken) {
        Set<String> cols = getTableColumnsSafely("user");
        if (!cols.contains("google_id")) return false;
        StringBuilder q = new StringBuilder("UPDATE user SET google_id = ?");
        if (cols.contains("google_access_token")) q.append(", google_access_token = ?");
        q.append(" WHERE id = ?");
        try (PreparedStatement ps = cnx.prepareStatement(q.toString())) {
            int i = 1;
            ps.setString(i++, googleId);
            if (cols.contains("google_access_token")) ps.setString(i++, accessToken);
            ps.setInt(i, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur linkGoogleAccount: " + e.getMessage());
        }
        return false;
    }

    public String getMedecinLanguages(int userId) {
        String q = "SELECT languages FROM medecin WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("languages");
        } catch (SQLException e) { System.out.println("Erreur getMedecinLanguages: " + e.getMessage()); }
        return null;
    }

    public boolean updateUserRole(int userId, String role) {
        String q = "UPDATE user SET roles = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Erreur updateUserRole: " + e.getMessage()); }
        return false;
    }

    public int createMedecinForUser(int userId) {
        // Return existing medecin id if already linked
        try (PreparedStatement ps = cnx.prepareStatement("SELECT id FROM medecin WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException ignored) {}

        User user = getById(userId);
        if (user == null) return -1;

        Set<String> cols = getTableColumnsSafely("medecin");
        if (cols.isEmpty()) return -1;

        Map<String, Object> values = new LinkedHashMap<>();
        if (cols.contains("user_id"))           values.put("user_id",           userId);
        if (cols.contains("nom"))               values.put("nom",               user.getNom());
        if (cols.contains("prenom"))            values.put("prenom",            user.getPrenom());
        if (cols.contains("email"))             values.put("email",             user.getEmail());
        if (cols.contains("specialite"))        values.put("specialite",        "Non spécifiée");
        if (cols.contains("cabinet"))           values.put("cabinet",           "Non spécifié");
        if (cols.contains("isVerified"))        values.put("isVerified",        false);
        if (cols.contains("profile_completed")) values.put("profile_completed", false);

        String query = buildInsertQuery("medecin", values.keySet());
        try (PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            for (Object v : values.values()) ps.setObject(idx++, v);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) { System.out.println("Erreur createMedecinForUser: " + e.getMessage()); }
        return -1;
    }

    public boolean toggleVerified(int userId, boolean verified) {
        String query = "UPDATE user SET is_verified = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setBoolean(1, verified); ps.setInt(2, userId); ps.executeUpdate(); return true;
        } catch (SQLException e) { System.out.println("Erreur toggleVerified: " + e.getMessage()); }
        return false;
    }

    public boolean updateBiometricId(int userId, String bioId) {
        String query = "UPDATE user SET biometric_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, bioId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Erreur updateBiometricId: " + e.getMessage()); }
        return false;
    }

    public LoginResult loginByBiometric(String biometricId) {
        if (biometricId == null || biometricId.isBlank()) return null;
        String query = "SELECT * FROM user WHERE biometric_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, biometricId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new LoginResult(mapUser(rs), "user", -1);
            }
        } catch (SQLException e) { System.out.println("Erreur loginByBiometric: " + e.getMessage()); }
        return null;
    }

    public boolean deleteUser(int userId) {
        try {
            cnx.createStatement().executeUpdate("DELETE FROM rendez_vous WHERE patient_id = " + userId);
            cnx.createStatement().executeUpdate("DELETE FROM rendez_vous WHERE medecin_id IN (SELECT id FROM medecin WHERE user_id = " + userId + ")");
            cnx.createStatement().executeUpdate("DELETE FROM disponibilite WHERE medecin_id IN (SELECT id FROM medecin WHERE user_id = " + userId + ")");
            cnx.createStatement().executeUpdate("DELETE FROM medecin WHERE user_id = " + userId);
            cnx.createStatement().executeUpdate("DELETE FROM demande_medecin WHERE user_id = " + userId);
            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM user WHERE id = ?")) {
                ps.setInt(1, userId); ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) { System.out.println("Erreur deleteUser: " + e.getMessage()); }
        return false;
    }

    @Override
    public List<User> getAll() {
        List<User> list = new ArrayList<>();
        String query = "SELECT u.*, m.rating_average FROM user u LEFT JOIN medecin m ON u.id = m.user_id ORDER BY u.id DESC";
        try (Statement st = cnx.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) { System.out.println("Erreur getAllUsers: " + e.getMessage()); }
        return list;
    }

    public List<User> getAllUsers() { return getAll(); }

    // ========== ADMIN STATS & MANAGEMENT ==========

    public long getTotalUsersCount() {
        return getCount("SELECT COUNT(*) FROM user");
    }

    public long getTotalMedecinsCount() {
        return getCount("SELECT COUNT(*) FROM medecin");
    }

    public long getTotalAppointmentsCount() {
        return getCount("SELECT COUNT(*) FROM rendez_vous");
    }

    public long getNewUsersCount(int days) {
        return getCount("SELECT COUNT(*) FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY)", days);
    }

    public long getNewMedecinsCount(int days) {
        return getCount("SELECT COUNT(*) FROM medecin WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY)", days);
    }

    public List<Map<String, Object>> getPendingDoctorRequests() {
        List<Map<String, Object>> requests = new ArrayList<>();
        String query = "SELECT d.*, u.nom, u.prenom, u.email FROM demande_medecin d JOIN user u ON d.user_id = u.id WHERE d.statut = 'en_attente'";
        try (Statement st = cnx.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                Map<String, Object> req = new HashMap<>();
                req.put("id", rs.getInt("id"));
                req.put("user_id", rs.getInt("user_id"));
                req.put("nom", rs.getString("nom"));
                req.put("prenom", rs.getString("prenom"));
                req.put("email", rs.getString("email"));
                req.put("created_at", rs.getTimestamp("created_at"));
                requests.add(req);
            }
        } catch (SQLException e) { System.out.println("Erreur getPendingDoctorRequests: " + e.getMessage()); }
        return requests;
    }

    public boolean approveDoctorRequest(int requestId, int userId) {
        try {
            cnx.setAutoCommit(false);
            // 1. Update request status
            try (PreparedStatement ps = cnx.prepareStatement("UPDATE demande_medecin SET statut = 'approuve' WHERE id = ?")) {
                ps.setInt(1, requestId); ps.executeUpdate();
            }
            // 2. Add ROLE_MEDECIN
            User user = getById(userId);
            if (user != null) {
                String roles = user.getRoles();
                if (!roles.contains("ROLE_MEDECIN")) {
                    roles = roles.replace("]", ", \"ROLE_MEDECIN\"]");
                    roles = roles.replaceFirst("\\[, ", "[");
                    if (!roles.startsWith("[")) roles = "[\"ROLE_MEDECIN\"]";
                    updateUserRole(userId, roles);
                }
            }
            // 3. Create entry in medecin table if not exists
            createMedecinForUser(userId);
            
            cnx.commit();
            return true;
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ignored) {}
            System.out.println("Erreur approveDoctorRequest: " + e.getMessage());
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ignored) {}
        }
        return false;
    }

    public boolean rejectDoctorRequest(int requestId) {
        String query = "UPDATE demande_medecin SET statut = 'rejete' WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Erreur rejectDoctorRequest: " + e.getMessage()); }
        return false;
    }

    private long getCount(String query, Object... params) {
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { System.out.println("Erreur getCount: " + e.getMessage()); }
        return 0;
    }

    public List<User> getUsersByRole(String roleSnippet) {
        List<User> list = new ArrayList<>();
        String query = "SELECT u.*, m.rating_average FROM user u LEFT JOIN medecin m ON u.id = m.user_id WHERE u.roles LIKE ? ORDER BY u.id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, "%" + roleSnippet + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) { System.out.println("Erreur getUsersByRole: " + e.getMessage()); }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        ResultSetMetaData meta = rs.getMetaData();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setNumero(rs.getString("numero"));
        user.setAdresse(rs.getString("adresse"));
        user.setPhoto(rs.getString("photo"));
        user.setRoles(rs.getString("roles"));
        user.setIsVerified(rs.getBoolean("is_verified"));

        try {
            if (hasColumn(meta, "rating_average")) {
                double rating = rs.getDouble("rating_average");
                if (!rs.wasNull()) {
                    user.setRatingAverage(rating);
                }
            }
        } catch (Exception ignored) {}

        // New columns — read safely
        if (hasColumn(meta, "city")) user.setCity(rs.getString("city"));
        if (hasColumn(meta, "gender")) user.setGender(rs.getString("gender"));
        if (hasColumn(meta, "blood_type")) user.setBloodType(rs.getString("blood_type"));
        if (hasColumn(meta, "allergies")) user.setAllergies(rs.getString("allergies"));
        if (hasColumn(meta, "profile_completed")) user.setProfileCompleted(rs.getBoolean("profile_completed"));
        if (hasColumn(meta, "skip_count")) user.setSkipCount(rs.getInt("skip_count"));
        if (hasColumn(meta, "privacy_level")) user.setPrivacyLevel(rs.getString("privacy_level"));
        if (hasColumn(meta, "google_id")) user.setGoogleId(rs.getString("google_id"));
        if (hasColumn(meta, "latitude")) { double lat = rs.getDouble("latitude"); if (!rs.wasNull()) user.setLatitude(lat); }
        if (hasColumn(meta, "longitude")) { double lng = rs.getDouble("longitude"); if (!rs.wasNull()) user.setLongitude(lng); }
        if (hasColumn(meta, "wants_email_notifications")) user.setWantsEmailNotifications(rs.getBoolean("wants_email_notifications"));
        if (hasColumn(meta, "username")) user.setUsername(rs.getString("username"));
        if (hasColumn(meta, "biometric_id")) user.setBiometricId(rs.getString("biometric_id"));
        return user;
    }

    // --- Admin account setup ---
    private void ensureDefaultAdminAccount() {
        if (cnx == null) return;
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
                boolean pwMatch = storedHash != null && BCrypt.checkpw(DEFAULT_ADMIN_PASSWORD, storedHash.replace("$2y$", "$2a$"));
                boolean rolesMatch = storedRoles != null && storedRoles.contains("ROLE_ADMIN");
                if (!pwMatch || !rolesMatch || !verified) {
                    try (PreparedStatement update = cnx.prepareStatement(
                        "UPDATE user SET nom = ?, prenom = ?, password = ?, roles = ?, is_verified = ? WHERE id = ?")) {
                        update.setString(1, "Admin"); update.setString(2, "Medicare");
                        update.setString(3, hashPassword(DEFAULT_ADMIN_PASSWORD));
                        update.setString(4, DEFAULT_ADMIN_ROLES); update.setBoolean(5, true); update.setInt(6, userId);
                        update.executeUpdate();
                    }
                }
                return;
            }
            try (PreparedStatement insert = cnx.prepareStatement(
                "INSERT INTO user (nom, prenom, username, email, password, numero, adresse, photo, roles, is_verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, "Admin"); insert.setString(2, "Medicare"); insert.setString(3, "admin"); insert.setString(4, DEFAULT_ADMIN_EMAIL);
                insert.setString(5, hashPassword(DEFAULT_ADMIN_PASSWORD)); insert.setString(6, ""); insert.setString(7, "");
                insert.setString(8, null); insert.setString(9, DEFAULT_ADMIN_ROLES); insert.setBoolean(10, true);
                insert.executeUpdate();
            }
        } catch (SQLException e) { System.out.println("Erreur ensureDefaultAdminAccount: " + e.getMessage()); }
    }

    private void ensureDefaultAdminInAdminTable() {
        Set<String> columns = getTableColumnsSafely("admin");
        if (columns.isEmpty() || !columns.contains("email") || !columns.contains("password")) return;
        try (PreparedStatement select = cnx.prepareStatement("SELECT * FROM admin WHERE email = ?")) {
            select.setString(1, DEFAULT_ADMIN_EMAIL);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                if (!passwordMatches(DEFAULT_ADMIN_PASSWORD, rs.getString("password"))) {
                    try (PreparedStatement update = cnx.prepareStatement("UPDATE admin SET password = ? WHERE email = ?")) {
                        update.setString(1, hashPassword(DEFAULT_ADMIN_PASSWORD)); update.setString(2, DEFAULT_ADMIN_EMAIL);
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
            if (columns.contains("nom")) first = appendColumnValue(query, values, params, first, "nom", "Admin");
            if (columns.contains("prenom")) first = appendColumnValue(query, values, params, first, "prenom", "Medicare");
            if (columns.contains("username")) first = appendColumnValue(query, values, params, first, "username", "admin");
            if (columns.contains("role")) first = appendColumnValue(query, values, params, first, "role", "ROLE_ADMIN");
            if (columns.contains("roles")) first = appendColumnValue(query, values, params, first, "roles", DEFAULT_ADMIN_ROLES);
            if (columns.contains("is_verified")) first = appendColumnValue(query, values, params, first, "is_verified", true);
            query.append(")"); values.append(")"); query.append(values);
            try (PreparedStatement insert = cnx.prepareStatement(query.toString())) {
                int index = 1;
                for (Object param : params) insert.setObject(index++, param);
                insert.executeUpdate();
            }
        } catch (SQLException e) { System.out.println("Erreur ensureDefaultAdminInAdminTable: " + e.getMessage()); }
    }

    // --- Utility methods ---
    private String hashPassword(String password) { return BCrypt.hashpw(password, BCrypt.gensalt(13)).replace("$2a$", "$2y$"); }
    private boolean passwordMatches(String plain, String stored) {
        if (stored == null || stored.isBlank()) return false;
        return BCrypt.checkpw(plain, stored.replace("$2y$", "$2a$"));
    }
    private String normalizeRoles(String roles) { return (roles == null || roles.isBlank()) ? "[\"ROLE_USER\"]" : roles; }
    private String buildUsername(User user) {
        String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        if (!email.isBlank() && email.contains("@")) return email.substring(0, email.indexOf('@'));
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
        String nom = user.getNom() == null ? "" : user.getNom().trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
        String username = (prenom + "." + nom).replaceAll("^\\.+|\\.+$", "");
        return username.isBlank() ? "user" : username;
    }
    private Set<String> getTableColumns(String tableName) throws SQLException {
        Set<String> columns = new LinkedHashSet<>();
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM " + tableName + " WHERE 1 = 0")) {
            ResultSetMetaData meta = ps.executeQuery().getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) columns.add(meta.getColumnName(i).toLowerCase());
        }
        return columns;
    }
    private Set<String> getTableColumnsSafely(String tableName) {
        try { return getTableColumns(tableName); } catch (SQLException e) { return new LinkedHashSet<>(); }
    }
    private String findFirstMatching(Set<String> columns, String... candidates) {
        for (String c : candidates) if (columns.contains(c.toLowerCase())) return c;
        return null;
    }
    private boolean hasColumn(ResultSetMetaData meta, String col) throws SQLException {
        for (int i = 1; i <= meta.getColumnCount(); i++) if (col.equalsIgnoreCase(meta.getColumnName(i))) return true;
        return false;
    }
    private String readFirstAvailable(ResultSet rs, Set<String> columns, String... candidates) throws SQLException {
        for (String c : candidates) if (columns.contains(c.toLowerCase())) return rs.getString(c);
        return null;
    }
    private boolean appendColumnValue(StringBuilder q, StringBuilder v, List<Object> p, boolean first, String col, Object val) {
        if (!first) { q.append(", "); v.append(", "); }
        q.append(col); v.append("?"); p.add(val); return false;
    }
    private String buildInsertQuery(String tableName, Set<String> columns) {
        StringBuilder q = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder v = new StringBuilder(" VALUES (");
        int i = 0;
        for (String col : columns) { if (i > 0) { q.append(", "); v.append(", "); } q.append(col); v.append("?"); i++; }
        q.append(")"); v.append(")");
        return q.append(v).toString();
    }
    private String buildRegisterErrorMessage(SQLException e) {
        String sqlState = e.getSQLState();
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if ("23000".equals(sqlState) || message.contains("duplicate") || message.contains("unique")) return "Cet email est deja utilise.";
        if (message.contains("unknown column")) return "Erreur base de donnees: colonne manquante dans la table user.";
        if (message.contains("cannot be null")) return "Erreur base de donnees: un champ obligatoire manque dans la table user.";
        return "Impossible de creer le compte. Verifiez la structure de la table user.";
    }

    /**
     * Returns the User for the given patient_id.
     * Since patient_id = user_id (no separate patient table), this just delegates to getById.
     */
    public User getUserByPatientId(int patientId) {
        return getById(patientId);
    }

    /**
     * Returns the User linked to a medecin row (via medecin.user_id FK).
     */
    public User getUserByMedecinId(int medecinId) {
        String q = "SELECT u.* FROM user u JOIN medecin m ON u.id = m.user_id WHERE m.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) { System.out.println("Erreur getUserByMedecinId: " + e.getMessage()); }
        return null;
    }
}
