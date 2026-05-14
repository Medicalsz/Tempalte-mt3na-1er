package com.medicare.services;

import com.medicare.interfaces.Crud;
import com.medicare.models.Medecin;
import com.medicare.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedecinService implements Crud<Medecin> {

    private final Connection cnx;

    public MedecinService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ==================== CRUD ====================

    @Override
    public void add(Medecin m) { create(m); }

    public boolean create(Medecin m) {
        String q = "INSERT INTO medecin (user_id, nom, prenom, email, specialite, cabinet, bio, isVerified) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getUserId());
            ps.setString(2, m.getNom());
            ps.setString(3, m.getPrenom());
            ps.setString(4, m.getEmail());
            ps.setString(5, m.getSpecialite() != null ? m.getSpecialite() : "Non spécifiée");
            ps.setString(6, m.getCabinet() != null ? m.getCabinet() : "Non spécifié");
            ps.setString(7, m.getBio());
            ps.setBoolean(8, m.isVerified());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) m.setId(keys.getInt(1));
            return true;
        } catch (SQLException e) { System.out.println("Erreur MedecinService.create: " + e.getMessage()); }
        return false;
    }

    @Override
    public List<Medecin> getAll() {
        List<Medecin> list = new ArrayList<>();
        String q = "SELECT * FROM medecin ORDER BY id DESC";
        try (Statement st = cnx.createStatement()) {
            ResultSet rs = st.executeQuery(q);
            while (rs.next()) list.add(mapMedecin(rs));
        } catch (SQLException e) { System.out.println("Erreur MedecinService.getAll: " + e.getMessage()); }
        return list;
    }

    public Medecin getById(int id) {
        String q = "SELECT * FROM medecin WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapMedecin(rs);
        } catch (SQLException e) { System.out.println("Erreur MedecinService.getById: " + e.getMessage()); }
        return null;
    }

    public Medecin getByUserId(int userId) {
        String q = "SELECT * FROM medecin WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapMedecin(rs);
        } catch (SQLException e) { System.out.println("Erreur MedecinService.getByUserId: " + e.getMessage()); }
        return null;
    }

    @Override
    public void update(Medecin m) { updateMedecin(m); }

    public boolean updateMedecin(Medecin m) {
        String q = "UPDATE medecin SET nom=?, prenom=?, email=?, numero=?, adresse=?, specialite=?, cabinet=?, bio=?, " +
                   "ville=?, prixConsultation=?, experience_years=?, consultation_duration=?, " +
                   "is_available_online=?, rating_average=?, rating_count=?, " +
                   "latitude=?, longitude=?, languages=?, gender=?, photo=?, isVerified=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setString(1, m.getNom());
            ps.setString(2, m.getPrenom());
            ps.setString(3, m.getEmail());
            ps.setString(4, m.getNumero());
            ps.setString(5, m.getAdresse());
            ps.setString(6, m.getSpecialite());
            ps.setString(7, m.getCabinet());
            ps.setString(8, m.getBio());
            ps.setString(9, m.getVille());
            ps.setDouble(10, m.getPrixConsultation());
            if (m.getExperienceYears() != null) ps.setInt(11, m.getExperienceYears());
            else ps.setNull(11, Types.INTEGER);
            ps.setInt(12, m.getConsultationDuration());
            ps.setBoolean(13, m.isAvailableOnline());
            ps.setDouble(14, m.getRatingAverage());
            ps.setInt(15, m.getRatingCount());
            if (m.getLatitude() != null) ps.setDouble(16, m.getLatitude());
            else ps.setNull(16, Types.DOUBLE);
            if (m.getLongitude() != null) ps.setDouble(17, m.getLongitude());
            else ps.setNull(17, Types.DOUBLE);
            ps.setString(18, m.getLanguages());
            ps.setString(19, m.getGender());
            ps.setString(20, m.getPhoto());
            ps.setBoolean(21, m.isVerified());
            ps.setInt(22, m.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Erreur MedecinService.update: " + e.getMessage()); }
        return false;
    }

    @Override
    public void delete(int medecinId) { deleteMedecin(medecinId); }

    public boolean deleteMedecin(int medecinId) {
        try {
            cnx.setAutoCommit(false);
            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM rendez_vous WHERE medecin_id = ?")) {
                ps.setInt(1, medecinId); ps.executeUpdate();
            }
            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM disponibilite WHERE medecin_id = ?")) {
                ps.setInt(1, medecinId); ps.executeUpdate();
            }
            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM medecin WHERE id = ?")) {
                ps.setInt(1, medecinId); ps.executeUpdate();
            }
            cnx.commit();
            return true;
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ignored) {}
            System.out.println("Erreur MedecinService.delete: " + e.getMessage());
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ignored) {}
        }
        return false;
    }

    // ==================== GEOLOCATION ====================

    public record DoctorNearby(int userId, int medecinId, String fullName, String city,
                               String adresse, double latitude, double longitude,
                               String specialite, double distanceKm) {}

    public List<DoctorNearby> getNearbyDoctors(int excludeUserId, double fromLat, double fromLng) {
        List<DoctorNearby> list = new ArrayList<>();
        String q = "SELECT m.id, m.user_id, m.nom, m.prenom, m.ville, m.adresse, m.latitude, m.longitude, m.specialite " +
                   "FROM medecin m " +
                   "WHERE m.user_id <> ? AND m.latitude IS NOT NULL AND m.longitude IS NOT NULL";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, excludeUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double lat = rs.getDouble("latitude");
                double lng = rs.getDouble("longitude");
                String fullName = (rs.getString("prenom") == null ? "" : rs.getString("prenom") + " ")
                        + (rs.getString("nom") == null ? "" : rs.getString("nom"));
                list.add(new DoctorNearby(
                        rs.getInt("user_id"), rs.getInt("id"),
                        fullName.trim(), rs.getString("ville"), rs.getString("adresse"),
                        lat, lng, rs.getString("specialite"),
                        haversineKm(fromLat, fromLng, lat, lng)));
            }
        } catch (SQLException e) { System.out.println("Erreur getNearbyDoctors: " + e.getMessage()); }
        list.sort(java.util.Comparator.comparingDouble(DoctorNearby::distanceKm));
        return list;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ==================== MAPPER ====================

    private Medecin mapMedecin(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Medecin m = new Medecin();
        m.setId(rs.getInt("id"));
        m.setUserId(col(meta, "user_id") ? rs.getInt("user_id") : 0);
        m.setNom(col(meta, "nom") ? rs.getString("nom") : null);
        m.setPrenom(col(meta, "prenom") ? rs.getString("prenom") : null);
        m.setEmail(col(meta, "email") ? rs.getString("email") : null);
        m.setNumero(col(meta, "numero") ? rs.getString("numero") : null);
        m.setAdresse(col(meta, "adresse") ? rs.getString("adresse") : null);
        m.setVille(col(meta, "ville") ? rs.getString("ville") : null);
        m.setSpecialite(col(meta, "specialite") ? rs.getString("specialite") : null);
        m.setCabinet(col(meta, "cabinet") ? rs.getString("cabinet") : null);
        m.setBio(col(meta, "bio") ? rs.getString("bio") : null);
        m.setPhoto(col(meta, "photo") ? rs.getString("photo") : null);
        m.setGender(col(meta, "gender") ? rs.getString("gender") : null);
        m.setLanguages(col(meta, "languages") ? rs.getString("languages") : null);
        if (col(meta, "prixConsultation")) m.setPrixConsultation(rs.getDouble("prixConsultation"));
        if (col(meta, "experience_years")) {
            int exp = rs.getInt("experience_years");
            if (!rs.wasNull()) m.setExperienceYears(exp);
        }
        if (col(meta, "consultation_duration")) m.setConsultationDuration(rs.getInt("consultation_duration"));
        if (col(meta, "is_available_online")) m.setAvailableOnline(rs.getBoolean("is_available_online"));
        if (col(meta, "rating_average")) { double r = rs.getDouble("rating_average"); if (!rs.wasNull()) m.setRatingAverage(r); }
        if (col(meta, "rating_count")) m.setRatingCount(rs.getInt("rating_count"));
        if (col(meta, "isVerified")) m.setIsVerified(rs.getBoolean("isVerified"));
        if (col(meta, "latitude")) { double lat = rs.getDouble("latitude"); if (!rs.wasNull()) m.setLatitude(lat); }
        if (col(meta, "longitude")) { double lng = rs.getDouble("longitude"); if (!rs.wasNull()) m.setLongitude(lng); }
        if (col(meta, "disponibilite")) m.setDisponibilite(rs.getString("disponibilite"));
        if (col(meta, "privacy_level")) m.setPrivacyLevel(rs.getString("privacy_level"));
        if (col(meta, "profile_completed")) m.setProfileCompleted(rs.getBoolean("profile_completed"));
        return m;
    }

    private boolean col(ResultSetMetaData meta, String name) throws SQLException {
        for (int i = 1; i <= meta.getColumnCount(); i++)
            if (name.equalsIgnoreCase(meta.getColumnName(i))) return true;
        return false;
    }
}
