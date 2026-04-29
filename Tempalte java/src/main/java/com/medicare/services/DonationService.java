package com.medicare.services;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.medicare.models.Don;
import com.medicare.models.Donation;
import com.medicare.utils.MyConnection;

public class DonationService {

    private final Connection cnx;
    private String lastError;

    public DonationService() {
        cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) {
            lastError = "La connexion à la base de données est nulle.";
            System.err.println("ERREUR : " + lastError);
        } else {
            // Diagnostic des tables
            System.out.println("--- Diagnostic de la base de données ---");
            checkTable("don");
            checkTable("cause");
            checkTable("objet_don");
            ensurePhotoColumnExists();
        }
    }

    private void ensurePhotoColumnExists() {
        if (!checkColumnExists("objet_don", "photo")) {
            try {
                Statement st = cnx.createStatement();
                st.execute("ALTER TABLE objet_don ADD COLUMN photo VARCHAR(255) DEFAULT NULL");
                System.out.println("Colonne 'photo' ajoutée à la table 'objet_don'.");
            } catch (SQLException e) {
                System.err.println("Erreur lors de l'ajout de la colonne 'photo' : " + e.getMessage());
            }
        }
    }

    private void checkTable(String tableName) {
        try {
            DatabaseMetaData md = cnx.getMetaData();
            ResultSet rs = md.getColumns(null, null, tableName, null);
            System.out.print("Table [" + tableName + "] : ");
            boolean first = true;
            while (rs.next()) {
                if (!first) System.out.print(", ");
                System.out.print(rs.getString("COLUMN_NAME"));
                first = false;
            }
            System.out.println();
        } catch (SQLException e) {
            System.err.println("Erreur diagnostic table " + tableName + " : " + e.getMessage());
        }
    }

    public String getLastError() { return lastError; }

    public boolean addMoneyDon(int userId, int causeId, double amount, String description, String cardType) {
        if (cnx == null) return false;
        lastError = null;
        
        try {
            // Désactiver les contraintes de clés étrangères pour cette session
            cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS=0");
            
            // S'assurer que le donateur existe quand même
            ensureDonateurExists(userId);

            String query = "INSERT INTO don (donateur_id, cause_id, type_don, montant, mode_paiement, date_don, statut_don, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setInt(2, causeId);
            ps.setString(3, "argent");
            ps.setDouble(4, amount);
            ps.setString(5, cardType != null ? cardType : "argent");
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.setString(7, "confirmé");
            ps.setString(8, description);

            int rows = ps.executeUpdate();
            
            // Réactiver les contraintes
            cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS=1");

            if (rows > 0) {
                return updateCauseAmount(causeId, amount);
            }
        } catch (SQLException e) {
            lastError = e.getMessage();
            try { cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS=1"); } catch (SQLException ignored) {}
            System.err.println("Erreur addMoneyDon : " + lastError);
            e.printStackTrace();
        }
        return false;
    }

    public boolean addMaterialDon(int userId, int causeId, List<MaterialItem> items, String description) {
        if (cnx == null) return false;
        lastError = null;

        try {
            // Désactiver les contraintes
            cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS=0");
            
            ensureDonateurExists(userId);

            String queryDon = "INSERT INTO don (donateur_id, cause_id, type_don, description, date_don, statut_don, mode_paiement, montant) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            cnx.setAutoCommit(false);
            
            PreparedStatement psDon = cnx.prepareStatement(queryDon, Statement.RETURN_GENERATED_KEYS);
            psDon.setInt(1, userId);
            psDon.setInt(2, causeId);
            psDon.setString(3, "matériel");
            psDon.setString(4, description);
            psDon.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            psDon.setString(6, "en attente");
            psDon.setString(7, "materiel");
            psDon.setDouble(8, 0.0);

            int rows = psDon.executeUpdate();
            if (rows > 0) {
                ResultSet rs = psDon.getGeneratedKeys();
                if (rs.next()) {
                    int donId = rs.getInt(1);
                    // On vérifie si la table objet_don a une colonne 'description'
                    boolean hasDescInObjet = checkColumnExists("objet_don", "description");
                    String queryObjet = hasDescInObjet 
                        ? "INSERT INTO objet_don (don_id, nom_objet, quantite, description, photo) VALUES (?, ?, ?, ?, ?)"
                        : "INSERT INTO objet_don (don_id, nom_objet, quantite, photo) VALUES (?, ?, ?, ?)";
                    
                    PreparedStatement psObj = cnx.prepareStatement(queryObjet);
                    for (MaterialItem item : items) {
                        psObj.setInt(1, donId);
                        psObj.setString(2, item.getName());
                        psObj.setInt(3, item.getQuantity());
                        if (hasDescInObjet) {
                            psObj.setString(4, ""); 
                            psObj.setString(5, item.getPhoto());
                        } else {
                            psObj.setString(4, item.getPhoto());
                        }
                        psObj.addBatch();
                    }
                    psObj.executeBatch();
                }
                cnx.commit();
                
                // Réactiver les contraintes
                try { cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS=1"); } catch (SQLException ignored) {}
                return true;
            } else {
                // Si l'insertion du don a échoué (rows == 0)
                try { cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS=1"); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            lastError = e.getMessage();
            try { cnx.rollback(); } catch (SQLException ignored) {}
            try { cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS=1"); } catch (SQLException ignored) {}
            System.err.println("Erreur addMaterialDon : " + lastError);
            e.printStackTrace();
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ignored) {}
        }
        return false;
    }

    private void ensureDonateurExists(int userId) {
        // Tentative d'insertion "douce"
        try {
            // On vérifie d'abord
            PreparedStatement psCheck = cnx.prepareStatement("SELECT id_donateur FROM donateur WHERE id_donateur = ?");
            psCheck.setInt(1, userId);
            if (psCheck.executeQuery().next()) return;

            // Sinon on insère avec l'ID utilisateur
            PreparedStatement psIns = cnx.prepareStatement("INSERT IGNORE INTO donateur (id_donateur) VALUES (?)");
            psIns.setInt(1, userId);
            psIns.executeUpdate();
        } catch (SQLException e) {
            // Si l'ID utilisateur n'est pas l'id_donateur, on essaie via user_id
            try {
                PreparedStatement psIns2 = cnx.prepareStatement("INSERT IGNORE INTO donateur (user_id) VALUES (?)");
                psIns2.setInt(1, userId);
                psIns2.executeUpdate();
            } catch (SQLException ignored) {}
        }
    }

    public boolean updateMaterialDon(int donId, List<MaterialItem> items, String description) {
        if (cnx == null) return false;
        lastError = null;
        try {
            cnx.setAutoCommit(false);
            
            // 1. Mettre à jour la description dans la table don
            String qUpdateDon = "UPDATE don SET description = ? WHERE id = ?";
            PreparedStatement psUpdateDon = cnx.prepareStatement(qUpdateDon);
            psUpdateDon.setString(1, description);
            psUpdateDon.setInt(2, donId);
            psUpdateDon.executeUpdate();

            // 2. Supprimer les anciens objets
            String qDeleteObj = "DELETE FROM objet_don WHERE don_id = ?";
            PreparedStatement psDeleteObj = cnx.prepareStatement(qDeleteObj);
            psDeleteObj.setInt(1, donId);
            psDeleteObj.executeUpdate();

            // 3. Insérer les nouveaux objets
            boolean hasDescInObjet = checkColumnExists("objet_don", "description");
            String queryObjet = hasDescInObjet 
                ? "INSERT INTO objet_don (don_id, nom_objet, quantite, description, photo) VALUES (?, ?, ?, ?, ?)"
                : "INSERT INTO objet_don (don_id, nom_objet, quantite, photo) VALUES (?, ?, ?, ?)";
            
            PreparedStatement psObj = cnx.prepareStatement(queryObjet);
            for (MaterialItem item : items) {
                psObj.setInt(1, donId);
                psObj.setString(2, item.getName());
                psObj.setInt(3, item.getQuantity());
                if (hasDescInObjet) {
                    psObj.setString(4, "");
                    psObj.setString(5, item.getPhoto());
                } else {
                    psObj.setString(4, item.getPhoto());
                }
                psObj.addBatch();
            }
            psObj.executeBatch();

            cnx.commit();
            return true;
        } catch (SQLException e) {
            lastError = e.getMessage();
            try { cnx.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ignored) {}
        }
        return false;
    }

    public List<MaterialItem> getMaterialItemsForDon(int donId) {
        List<MaterialItem> list = new ArrayList<>();
        String q = "SELECT nom_objet, quantite, photo FROM objet_don WHERE don_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new MaterialItem(rs.getString("nom_objet"), rs.getInt("quantite"), rs.getString("photo")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public String getDonDescription(int donId) {
        String q = "SELECT description FROM don WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("description");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public double getTotalMoneyDonatedByUser(int userId) {
        if (cnx == null) return 0.0;
        String query = "SELECT SUM(montant) FROM don WHERE donateur_id = ? AND type_don = 'argent'";
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getTotalMoneyDonatedByUser : " + e.getMessage());
        }
        return 0.0;
    }

    public Donation getCauseByDonId(int donId) {
        String q = "SELECT c.* FROM cause c JOIN don d ON d.cause_id = c.id WHERE d.id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Donation d = new Donation();
                d.setId(rs.getInt("id"));
                d.setNom(getValue(rs, "nom", "titre"));
                return d;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class MaterialItem {
        private String name;
        private int quantity;
        private String photo;
        public MaterialItem(String name, int quantity) { 
            this(name, quantity, null); 
        }
        public MaterialItem(String name, int quantity, String photo) { 
            this.name = name; 
            this.quantity = quantity; 
            this.photo = photo;
        }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public String getPhoto() { return photo; }
        public void setPhoto(String photo) { this.photo = photo; }
    }

    private boolean updateCauseAmount(int causeId, double amount) {
        if (cnx == null) return false;
        String colActuel = null;
        if (checkColumnExists("cause", "montant_actuel")) {
            colActuel = "montant_actuel";
        } else if (checkColumnExists("cause", "actuel")) {
            colActuel = "actuel";
        } else {
            // Tentative de trouver n'importe quelle colonne qui ressemble à "actuel"
            try {
                DatabaseMetaData md = cnx.getMetaData();
                ResultSet rs = md.getColumns(null, null, "cause", null);
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    if (col.toLowerCase().contains("actuel")) {
                        colActuel = col;
                        break;
                    }
                }
            } catch (SQLException ignored) {}
        }

        if (colActuel == null) {
            lastError = "Impossible de trouver la colonne du montant actuel dans la table 'cause'.";
            return false;
        }

        String query = "UPDATE cause SET " + colActuel + " = " + colActuel + " + ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setDouble(1, amount);
            ps.setInt(2, causeId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                lastError = "Aucune cause trouvée avec l'ID " + causeId;
                return false;
            }
            return true;
        } catch (SQLException e) {
            lastError = "Erreur updateCauseAmount (" + colActuel + ") : " + e.getMessage();
            System.err.println(lastError);
            return false;
        }
    }

    public List<Don> getAllDons() {
        List<Don> list = new ArrayList<>();
        if (cnx == null) return list;

        // Détection du nom de la colonne pour la cause
        String colCauseNom = checkColumnExists("cause", "nom") ? "nom" : "titre";
        
        // Query avec aliasing explicite
        String query = "SELECT d.id as don_id_fix, d.*, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email, c." + colCauseNom + " as cause_nom " +
                       "FROM don d " +
                       "LEFT JOIN user u ON d.donateur_id = u.id " +
                       "LEFT JOIN cause c ON d.cause_id = c.id " +
                       "ORDER BY d.date_don DESC";
        
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                Don don = new Don();
                int realDonId = rs.getInt("don_id_fix");
                don.setId(realDonId);
                don.setDonateurNom(rs.getString("user_nom"));
                don.setDonateurPrenom(rs.getString("user_prenom"));
                don.setDonateurEmail(rs.getString("user_email"));
                don.setCauseNom(rs.getString("cause_nom"));
                
                String typeDon = rs.getString("type_don");
                if (typeDon != null && typeDon.toLowerCase().contains("argent")) {
                    don.setType("argent");
                    don.setMontant(rs.getDouble("montant"));
                    don.setMode(rs.getString("mode_paiement"));
                } else {
                    don.setType("materiel");
                    // Remplissage des objets et photos
                    fillMaterialData(realDonId, don);
                }
                
                don.setAdresse(rs.getString("adresse"));
                
                // Détection dynamique des colonnes de géolocalisation
                setGeoLocation(rs, don);

                Timestamp ts = rs.getTimestamp("date_don");
                if (ts != null) don.setDate(ts.toLocalDateTime());
                don.setStatut(rs.getString("statut_don"));
                
                list.add(don);
            }
        } catch (SQLException e) {
            System.err.println("ERREUR SQL getAllDons : " + e.getMessage());
        }
        return list;
    }

    private void fillMaterialData(int donId, Don don) {
        StringBuilder sb = new StringBuilder();
        int totalQty = 0;
        String q = "SELECT nom_objet, quantite, photo FROM objet_don WHERE don_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, donId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (sb.length() > 0) sb.append("\n");
                    int qty = rs.getInt("quantite");
                    sb.append("x").append(qty).append(" ").append(rs.getString("nom_objet"));
                    totalQty += qty;
                    
                    String photo = rs.getString("photo");
                    if (photo != null && !photo.isEmpty()) {
                        don.getObjectPhotos().add(photo);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur fillMaterialData pour Don " + donId + " : " + e.getMessage());
        }
        don.setMateriels(sb.length() == 0 ? "Aucun objet" : sb.toString());
        don.setQuantite(totalQty);
    }

    private void setGeoLocation(ResultSet rs, Don don) throws SQLException {
        String[] latCols = {"latitude", "lattitude"};
        String[] lngCols = {"longitude", "lng"};

        for (String col : latCols) {
            if (hasColumn(rs, col)) {
                double val = rs.getDouble(col);
                if (!rs.wasNull()) {
                    don.setLatitude(val);
                    break;
                }
            }
        }
        for (String col : lngCols) {
            if (hasColumn(rs, col)) {
                double val = rs.getDouble(col);
                if (!rs.wasNull()) {
                    don.setLongitude(val);
                    break;
                }
            }
        }
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Don> getDonsByUserId(int userId) {
        List<Don> list = new ArrayList<>();
        // Query pour récupérer les dons d'un utilisateur spécifique avec le nom de la cause
        String query = "SELECT d.*, c." + (checkColumnExists("cause", "nom") ? "nom" : "titre") + " as cause_nom " +
                       "FROM don d " +
                       "LEFT JOIN cause c ON d.cause_id = c.id " +
                       "WHERE d.donateur_id = ? " +
                       "ORDER BY d.date_don DESC";
        
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Don don = new Don();
                int donId = rs.getInt("id");
                don.setId(donId);
                don.setCauseNom(rs.getString("cause_nom"));
                
                String typeDon = rs.getString("type_don");
                if (typeDon != null && typeDon.toLowerCase().contains("argent")) {
                    don.setType("argent");
                    don.setMontant(rs.getDouble("montant"));
                } else {
                    don.setType("materiel");
                    // Récupération des vrais objets depuis objet_don
                    don.setMateriels(getObjectsForDon(donId, don));
                    don.setQuantite(getTotalQuantityForDon(donId));
                }
                
                don.setAdresse(rs.getString("adresse"));
                Timestamp ts = rs.getTimestamp("date_don");
                if (ts != null) don.setDate(ts.toLocalDateTime());
                
                don.setStatut(rs.getString("statut_don"));
                list.add(don);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getDonsByUserId : " + e.getMessage());
        }
        return list;
    }

    private String getObjectsForDon(int donId, Don don) {
        StringBuilder sb = new StringBuilder();
        String q = "SELECT nom_objet, quantite, photo FROM objet_don WHERE don_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("x").append(rs.getInt("quantite"))
                  .append(" ").append(rs.getString("nom_objet"));
                
                String photo = rs.getString("photo");
                if (photo != null && !photo.isEmpty() && don != null) {
                    don.getObjectPhotos().add(photo);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getObjectsForDon : " + e.getMessage());
        }
        return sb.toString().isEmpty() ? "Aucun objet" : sb.toString();
    }

    private int getTotalQuantityForDon(int donId) {
        String q = "SELECT SUM(quantite) FROM objet_don WHERE don_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getTotalQuantityForDon : " + e.getMessage());
        }
        return 0;
    }

    public List<Donation> getAllDonations() {
        List<Donation> list = new ArrayList<>();
        
        // On récupère url_image de image_cause
        String query = "SELECT c.*, ic.url_image as img_url FROM cause c " +
                       "LEFT JOIN image_cause ic ON c.id = ic.cause_id"; 
        
        try {
            ResultSet rs = cnx.createStatement().executeQuery(query);
            while (rs.next()) {
                Donation d = new Donation();
                d.setId(rs.getInt("id"));
                d.setNom(getValue(rs, "nom", "titre"));
                d.setDescription(getValue(rs, "description", "contenu"));
                
                // Lecture de l'objectif montant
                double obj = 0;
                try { obj = rs.getDouble("objectif_montant"); } catch (SQLException e) {
                    try { obj = rs.getDouble("objectif"); } catch (SQLException e2) {}
                }
                d.setObjectifMontant(obj);

                // Lecture du montant actuel
                double actuel = 0;
                try { actuel = rs.getDouble("montant_actuel"); } catch (SQLException e) {
                    try { actuel = rs.getDouble("actuel"); } catch (SQLException e2) {}
                }
                d.setMontantActuel(actuel);

                // Lecture de l'URL de l'image (via l'alias img_url)
                try {
                    String img = rs.getString("img_url");
                    if (img == null || img.isEmpty()) {
                        // Tentative avec d'autres noms au cas où
                        img = getValue(rs, "image", "url", "image_name");
                    }
                    d.setImage(img);
                } catch (SQLException ignored) {}

                try {
                    d.setCause(rs.getString("cause"));
                } catch (SQLException ignored) {
                    d.setCause("Donation");
                }
                
                list.add(d);
            }
        } catch (SQLException e) {
            System.err.println("ERREUR SQL dans getAllDonations : " + e.getMessage());
            return getSimpleDonations();
        }
        return list;
    }

    public boolean deleteCause(int id) {
        try {
            // Supprimer l'image associée d'abord
            String q1 = "DELETE FROM image_cause WHERE cause_id = ?";
            PreparedStatement ps1 = cnx.prepareStatement(q1);
            ps1.setInt(1, id);
            ps1.executeUpdate();

            // Supprimer la cause
            String q2 = "DELETE FROM cause WHERE id = ?";
            PreparedStatement ps2 = cnx.prepareStatement(q2);
            ps2.setInt(1, id);
            int rows = ps2.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur suppression cause : " + e.getMessage());
            return false;
        }
    }

    public boolean createCause(Donation d) {
        String colNom = checkColumnExists("cause", "nom") ? "nom" : "titre";
        String colDesc = checkColumnExists("cause", "description") ? "description" : "contenu";
        String colObj = checkColumnExists("cause", "objectif_montant") ? "objectif_montant" : "objectif";
        String colImg = checkColumnExists("image_cause", "url_image") ? "url_image" : (checkColumnExists("image_cause", "image") ? "image" : "image_name");

        // Détection des dates et du statut
        boolean hasDateDebut = checkColumnExists("cause", "date_debut");
        boolean hasDateFin = checkColumnExists("cause", "date_fin");
        boolean hasStatut = checkColumnExists("cause", "statut");

        try {
            StringBuilder queryBuilder = new StringBuilder("INSERT INTO cause (")
                .append(colNom).append(", ")
                .append(colDesc).append(", ")
                .append(colObj);
            
            if (hasDateDebut) queryBuilder.append(", date_debut");
            if (hasDateFin) queryBuilder.append(", date_fin");
            if (hasStatut) queryBuilder.append(", statut");
            
            queryBuilder.append(") VALUES (?, ?, ?");
            if (hasDateDebut) queryBuilder.append(", ?");
            if (hasDateFin) queryBuilder.append(", ?");
            if (hasStatut) queryBuilder.append(", ?");
            queryBuilder.append(")");

            PreparedStatement ps1 = cnx.prepareStatement(queryBuilder.toString(), Statement.RETURN_GENERATED_KEYS);
            ps1.setString(1, d.getNom());
            ps1.setString(2, d.getDescription());
            ps1.setDouble(3, d.getObjectifMontant());
            
            int paramIndex = 4;
            if (hasDateDebut) ps1.setDate(paramIndex++, new java.sql.Date(System.currentTimeMillis()));
            if (hasDateFin) {
                // Par défaut, on met une date de fin dans 30 jours
                long thirtyDays = 30L * 24 * 60 * 60 * 1000;
                ps1.setDate(paramIndex++, new java.sql.Date(System.currentTimeMillis() + thirtyDays));
            }
            if (hasStatut) ps1.setString(paramIndex++, "active");

            ps1.executeUpdate();

            ResultSet keys = ps1.getGeneratedKeys();
            if (keys.next()) {
                int causeId = keys.getInt(1);
                
                if (d.getImage() != null && !d.getImage().isEmpty()) {
                    String q2 = "INSERT INTO image_cause (cause_id, " + colImg + ") VALUES (?, ?)";
                    PreparedStatement ps2 = cnx.prepareStatement(q2);
                    ps2.setInt(1, causeId);
                    ps2.setString(2, d.getImage());
                    ps2.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur création cause : " + e.getMessage());
        }
        return false;
    }

    public boolean updateDonStatut(int donId, String status) {
        String query = "UPDATE don SET statut_don = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setString(1, status);
            ps.setInt(2, donId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur updateDonStatut : " + e.getMessage());
            return false;
        }
    }

    public boolean updateDonAdresse(int donId, String adresse) {
        String query = "UPDATE don SET adresse = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setString(1, adresse);
            ps.setInt(2, donId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur updateDonAdresse : " + e.getMessage());
            return false;
        }
    }

    private boolean checkColumnExists(String tableName, String columnName) {
        try {
            DatabaseMetaData md = cnx.getMetaData();
            ResultSet rs = md.getColumns(null, null, tableName, columnName);
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }


    private List<Donation> getSimpleDonations() {
        List<Donation> list = new ArrayList<>();
        System.out.println("Mode repli : getSimpleDonations()");
        try {
            ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM cause");
            while (rs.next()) {
                Donation d = new Donation();
                d.setId(rs.getInt("id"));
                d.setNom(getValue(rs, "nom", "titre"));
                d.setDescription(getValue(rs, "description"));
                
                // On récupère aussi les montants ici au cas où
                try { d.setObjectifMontant(rs.getDouble("objectif_montant")); } catch (SQLException e) {
                    try { d.setObjectifMontant(rs.getDouble("objectif")); } catch (SQLException e2) {}
                }
                try { d.setMontantActuel(rs.getDouble("montant_actuel")); } catch (SQLException e) {
                    try { d.setMontantActuel(rs.getDouble("actuel")); } catch (SQLException e2) {}
                }

                list.add(d);
            }
        } catch (SQLException e) {
            System.err.println("Erreur mode repli : " + e.getMessage());
        }
        return list;
    }

    private String getValue(ResultSet rs, String... colNames) {
        for (String name : colNames) {
            try {
                String val = rs.getString(name);
                if (val != null) return val;
            } catch (SQLException ignored) {}
        }
        return "Non spécifié";
    }
}
