package com.medicare.services;

import com.medicare.models.Don;
import com.medicare.models.Donation;
import com.medicare.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DonationService {

    private final Connection cnx;
    private String lastError;

    public DonationService() {
        cnx = MyConnection.getInstance().getCnx();
        ensureTables();
    }

    // ── Table bootstrap ──────────────────────────────────────────────────────

    private void ensureTables() {
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS donation (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "nom VARCHAR(255) NOT NULL," +
                "description TEXT," +
                "cause VARCHAR(255)," +
                "image TEXT," +
                "objectif_montant DOUBLE DEFAULT 0," +
                "montant_actuel DOUBLE DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS don (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT," +
                "donateur_nom VARCHAR(100)," +
                "donateur_prenom VARCHAR(100)," +
                "donateur_email VARCHAR(255)," +
                "cause_nom VARCHAR(255)," +
                "montant DOUBLE," +
                "materiels TEXT," +
                "quantite INT," +
                "adresse TEXT," +
                "date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "mode VARCHAR(50)," +
                "statut VARCHAR(50) DEFAULT 'en_attente'," +
                "type VARCHAR(20)," +
                "latitude DOUBLE," +
                "longitude DOUBLE)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS don_materiel_items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "don_id INT NOT NULL," +
                "nom VARCHAR(255)," +
                "quantite INT," +
                "photo TEXT)");
        } catch (SQLException e) {
            System.out.println("DonationService ensureTables: " + e.getMessage());
        }
    }

    // ── Cause management ─────────────────────────────────────────────────────

    public List<Donation> getAllDonations() {
        List<Donation> list = new ArrayList<>();
        String q = "SELECT d.*, COALESCE((SELECT SUM(dn.montant) FROM don dn " +
                   "WHERE dn.cause_nom=d.nom AND dn.type='argent'),0) AS total " +
                   "FROM donation d ORDER BY d.id DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                Donation d = new Donation();
                d.setId(rs.getInt("id"));
                d.setNom(rs.getString("nom"));
                d.setDescription(rs.getString("description"));
                d.setCause(rs.getString("cause"));
                d.setImage(rs.getString("image"));
                d.setObjectifMontant(rs.getDouble("objectif_montant"));
                d.setMontantActuel(rs.getDouble("total"));
                list.add(d);
            }
        } catch (SQLException e) {
            System.out.println("getAllDonations: " + e.getMessage());
        }
        return list;
    }

    public boolean createCause(String nom, String description, String cause, String image, double objectif) {
        String q = "INSERT INTO donation (nom, description, cause, image, objectif_montant) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setString(1, nom);
            ps.setString(2, description);
            ps.setString(3, cause);
            ps.setString(4, image);
            ps.setDouble(5, objectif);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("createCause: " + e.getMessage());
        }
        return false;
    }

    public boolean createCause(Donation donation) {
        if (donation == null) {
            lastError = "Cause de donation invalide";
            return false;
        }
        return createCause(
                donation.getNom(),
                donation.getDescription(),
                donation.getCause(),
                donation.getImage(),
                donation.getObjectifMontant()
        );
    }

    public boolean deleteCause(int id) {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM donation WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("deleteCause: " + e.getMessage());
        }
        return false;
    }

    // ── Money donation ────────────────────────────────────────────────────────

    public boolean addMoneyDon(Don don) {
        String q = "INSERT INTO don (user_id,donateur_nom,donateur_prenom,donateur_email," +
                   "cause_nom,montant,mode,statut,type,date) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, don.getUserId());
            ps.setString(2, don.getDonateurNom());
            ps.setString(3, don.getDonateurPrenom());
            ps.setString(4, don.getDonateurEmail());
            ps.setString(5, don.getCauseNom());
            ps.setDouble(6, don.getMontant() != null ? don.getMontant() : 0);
            ps.setString(7, don.getMode());
            ps.setString(8, "en_attente");
            ps.setString(9, "argent");
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) don.setId(keys.getInt(1));
            return true;
        } catch (SQLException e) {
            System.out.println("addMoneyDon: " + e.getMessage());
        }
        return false;
    }

    public boolean addMoneyDon(int userId, int donationId, double amount, String description, String mode) {
        Donation cause = getCauseById(donationId);
        Don don = buildDonForUser(userId, cause);
        don.setMontant(amount);
        don.setMateriels(description);
        don.setMode(mode);
        return addMoneyDon(don);
    }

    // ── Material donation ─────────────────────────────────────────────────────

    public boolean addMaterialDon(Don don, List<MaterialItem> items) {
        String qDon = "INSERT INTO don (user_id,donateur_nom,donateur_prenom,donateur_email," +
                      "cause_nom,materiels,quantite,statut,type,date) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            cnx.setAutoCommit(false);
            int donId;
            try (PreparedStatement ps = cnx.prepareStatement(qDon, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, don.getUserId());
                ps.setString(2, don.getDonateurNom());
                ps.setString(3, don.getDonateurPrenom());
                ps.setString(4, don.getDonateurEmail());
                ps.setString(5, don.getCauseNom());
                ps.setString(6, don.getMateriels());
                ps.setInt(7, don.getQuantite() != null ? don.getQuantite() : 0);
                ps.setString(8, "en_attente");
                ps.setString(9, "materiel");
                ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) { cnx.rollback(); cnx.setAutoCommit(true); return false; }
                donId = keys.getInt(1);
                don.setId(donId);
            }
            try (PreparedStatement ps = cnx.prepareStatement(
                    "INSERT INTO don_materiel_items (don_id,nom,quantite,photo) VALUES (?,?,?,?)")) {
                for (MaterialItem item : items) {
                    ps.setInt(1, donId);
                    ps.setString(2, item.getNom());
                    ps.setInt(3, item.getQuantite());
                    ps.setString(4, item.getPhoto());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            cnx.commit();
            return true;
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ignored) {}
            System.out.println("addMaterialDon: " + e.getMessage());
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ignored) {}
        }
        return false;
    }

    public boolean addMaterialDon(int userId, int donationId, List<MaterialItem> items, String description) {
        Donation cause = getCauseById(donationId);
        Don don = buildDonForUser(userId, cause);
        don.setMateriels(description);
        don.setQuantite(items == null ? 0 : items.stream().mapToInt(MaterialItem::getQuantite).sum());
        return addMaterialDon(don, items == null ? List.of() : items);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<Don> getAllDons() {
        List<Don> list = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM don ORDER BY date DESC")) {
            while (rs.next()) {
                Don don = mapDon(rs);
                if ("materiel".equals(don.getType())) {
                    List<MaterialItem> items = getMaterialItemsForDon(don.getId());
                    List<String> photos = new ArrayList<>();
                    for (MaterialItem item : items) if (item.getPhoto() != null) photos.add(item.getPhoto());
                    don.setObjectPhotos(photos);
                }
                list.add(don);
            }
        } catch (SQLException e) {
            System.out.println("getAllDons: " + e.getMessage());
        }
        return list;
    }

    public List<Don> getDonsByUserId(int userId) {
        List<Don> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM don WHERE user_id=? ORDER BY date DESC")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Don don = mapDon(rs);
                if ("materiel".equals(don.getType())) {
                    List<MaterialItem> items = getMaterialItemsForDon(don.getId());
                    List<String> photos = new ArrayList<>();
                    for (MaterialItem item : items) if (item.getPhoto() != null) photos.add(item.getPhoto());
                    don.setObjectPhotos(photos);
                }
                list.add(don);
            }
        } catch (SQLException e) {
            System.out.println("getDonsByUserId: " + e.getMessage());
        }
        return list;
    }

    public List<Don> getDonsByType(String type) {
        List<Don> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM don WHERE type=? ORDER BY date DESC")) {
            ps.setString(1, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Don don = mapDon(rs);
                if ("materiel".equals(type)) {
                    List<MaterialItem> items = getMaterialItemsForDon(don.getId());
                    List<String> photos = new ArrayList<>();
                    for (MaterialItem item : items) if (item.getPhoto() != null) photos.add(item.getPhoto());
                    don.setObjectPhotos(photos);
                }
                list.add(don);
            }
        } catch (SQLException e) {
            System.out.println("getDonsByType: " + e.getMessage());
        }
        return list;
    }

    public List<MaterialItem> getMaterialItemsForDon(int donId) {
        List<MaterialItem> items = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM don_materiel_items WHERE don_id=?")) {
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                items.add(new MaterialItem(rs.getString("nom"), rs.getInt("quantite"), rs.getString("photo")));
        } catch (SQLException e) {
            System.out.println("getMaterialItemsForDon: " + e.getMessage());
        }
        return items;
    }

    public Donation getCauseById(int id) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT d.*, COALESCE((SELECT SUM(dn.montant) FROM don dn " +
                "WHERE dn.cause_nom=d.nom AND dn.type='argent'),0) AS total " +
                "FROM donation d WHERE d.id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Donation d = new Donation();
                d.setId(rs.getInt("id"));
                d.setNom(rs.getString("nom"));
                d.setDescription(rs.getString("description"));
                d.setCause(rs.getString("cause"));
                d.setImage(rs.getString("image"));
                d.setObjectifMontant(rs.getDouble("objectif_montant"));
                d.setMontantActuel(rs.getDouble("total"));
                return d;
            }
        } catch (SQLException e) {
            lastError = e.getMessage();
            System.out.println("getCauseById: " + e.getMessage());
        }
        return null;
    }

    public Donation getCauseByDonId(int donId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT d.*, COALESCE((SELECT SUM(dn2.montant) FROM don dn2 " +
                "WHERE dn2.cause_nom=d.nom AND dn2.type='argent'),0) AS total " +
                "FROM donation d JOIN don dn ON dn.cause_nom=d.nom WHERE dn.id=?")) {
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Donation d = new Donation();
                d.setId(rs.getInt("id"));
                d.setNom(rs.getString("nom"));
                d.setDescription(rs.getString("description"));
                d.setCause(rs.getString("cause"));
                d.setImage(rs.getString("image"));
                d.setObjectifMontant(rs.getDouble("objectif_montant"));
                d.setMontantActuel(rs.getDouble("total"));
                return d;
            }
        } catch (SQLException e) {
            lastError = e.getMessage();
            System.out.println("getCauseByDonId: " + e.getMessage());
        }
        return null;
    }

    public String getDonDescription(int donId) {
        try (PreparedStatement ps = cnx.prepareStatement("SELECT materiels FROM don WHERE id=?")) {
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("materiels");
        } catch (SQLException e) {
            lastError = e.getMessage();
            System.out.println("getDonDescription: " + e.getMessage());
        }
        return "";
    }

    public double getTotalMoneyDonatedByUser(int userId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT COALESCE(SUM(montant),0) FROM don WHERE user_id=? AND type='argent'")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.out.println("getTotalMoneyDonatedByUser: " + e.getMessage());
        }
        return 0;
    }

    // ── Updates ───────────────────────────────────────────────────────────────

    public boolean updateDonStatut(int id, String statut) {
        try (PreparedStatement ps = cnx.prepareStatement("UPDATE don SET statut=? WHERE id=?")) {
            ps.setString(1, normalizeStatut(statut));
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("updateDonStatut: " + e.getMessage());
        }
        return false;
    }

    public boolean updateDonAdresse(int id, String adresse) {
        try (PreparedStatement ps = cnx.prepareStatement("UPDATE don SET adresse=? WHERE id=?")) {
            ps.setString(1, adresse);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("updateDonAdresse: " + e.getMessage());
        }
        return false;
    }

    public boolean updateMaterialDon(int donId, String materiels, int quantite, List<MaterialItem> items) {
        try {
            cnx.setAutoCommit(false);
            try (PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE don SET materiels=?,quantite=? WHERE id=?")) {
                ps.setString(1, materiels); ps.setInt(2, quantite); ps.setInt(3, donId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = cnx.prepareStatement(
                    "DELETE FROM don_materiel_items WHERE don_id=?")) {
                ps.setInt(1, donId); ps.executeUpdate();
            }
            try (PreparedStatement ps = cnx.prepareStatement(
                    "INSERT INTO don_materiel_items (don_id,nom,quantite,photo) VALUES (?,?,?,?)")) {
                for (MaterialItem item : items) {
                    ps.setInt(1, donId); ps.setString(2, item.getNom());
                    ps.setInt(3, item.getQuantite()); ps.setString(4, item.getPhoto());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            cnx.commit();
            return true;
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ignored) {}
            System.out.println("updateMaterialDon: " + e.getMessage());
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ignored) {}
        }
        return false;
    }

    public boolean updateMaterialDon(int donId, List<MaterialItem> items, String description) {
        int quantity = items == null ? 0 : items.stream().mapToInt(MaterialItem::getQuantite).sum();
        return updateMaterialDon(donId, description, quantity, items == null ? List.of() : items);
    }

    public String getLastError() {
        return lastError;
    }

    private String normalizeStatut(String statut) {
        if (statut == null) return null;
        String normalized = statut.trim().toLowerCase();
        if ("confirmé".equals(normalized) || "confirmÃ©".equals(normalized)) return "confirme";
        return normalized;
    }

    private Don buildDonForUser(int userId, Donation cause) {
        Don don = new Don();
        don.setUserId(userId);
        don.setCauseNom(cause != null ? cause.getNom() : "");

        try (PreparedStatement ps = cnx.prepareStatement("SELECT nom, prenom, email FROM user WHERE id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                don.setDonateurNom(rs.getString("nom"));
                don.setDonateurPrenom(rs.getString("prenom"));
                don.setDonateurEmail(rs.getString("email"));
            }
        } catch (SQLException e) {
            lastError = e.getMessage();
            System.out.println("buildDonForUser: " + e.getMessage());
        }
        return don;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private Don mapDon(ResultSet rs) throws SQLException {
        Don don = new Don();
        don.setId(rs.getInt("id"));
        try { don.setUserId(rs.getInt("user_id")); } catch (SQLException ignored) {}
        don.setDonateurNom(rs.getString("donateur_nom"));
        don.setDonateurPrenom(rs.getString("donateur_prenom"));
        don.setDonateurEmail(rs.getString("donateur_email"));
        don.setCauseNom(rs.getString("cause_nom"));
        don.setType(rs.getString("type"));
        don.setStatut(rs.getString("statut"));
        don.setMode(rs.getString("mode"));
        try { don.setAdresse(rs.getString("adresse")); } catch (SQLException ignored) {}
        try { double m = rs.getDouble("montant"); if (!rs.wasNull()) don.setMontant(m); } catch (SQLException ignored) {}
        try { int q = rs.getInt("quantite"); if (!rs.wasNull()) don.setQuantite(q); } catch (SQLException ignored) {}
        try { don.setMateriels(rs.getString("materiels")); } catch (SQLException ignored) {}
        try { double lat = rs.getDouble("latitude"); if (!rs.wasNull()) don.setLatitude(lat); } catch (SQLException ignored) {}
        try { double lng = rs.getDouble("longitude"); if (!rs.wasNull()) don.setLongitude(lng); } catch (SQLException ignored) {}
        try { Timestamp ts = rs.getTimestamp("date"); if (ts != null) don.setDate(ts.toLocalDateTime()); } catch (SQLException ignored) {}
        return don;
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    public static class MaterialItem {
        private String nom;
        private int quantite;
        private String photo;

        public MaterialItem(String nom, int quantite, String photo) {
            this.nom = nom; this.quantite = quantite; this.photo = photo;
        }

        public String getNom() { return nom; }
        public String getName() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public int getQuantite() { return quantite; }
        public int getQuantity() { return quantite; }
        public void setQuantite(int quantite) { this.quantite = quantite; }
        public String getPhoto() { return photo; }
        public void setPhoto(String photo) { this.photo = photo; }
    }
}
