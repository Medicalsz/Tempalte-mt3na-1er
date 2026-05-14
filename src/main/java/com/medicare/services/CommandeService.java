package com.medicare.services;

import com.medicare.interfaces.ICommandeService;
import com.medicare.models.Commande;
import com.medicare.utils.MyConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandeService implements ICommandeService {

    private final Connection cnx;

    public CommandeService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(Commande c) {
        String q = "INSERT INTO commande (commande_number, product_id, user_id, quantity, total_price, status, " +
                "notes, commande_date, delivery_date, created_at, stripe_payment_intent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getCommandeNumber() != null ? c.getCommandeNumber() : generateCommandeNumber());
            ps.setInt(2, c.getProductId());
            if (c.getUserId() > 0) ps.setInt(3, c.getUserId()); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, c.getQuantity());
            ps.setBigDecimal(5, c.getTotalPrice() != null ? c.getTotalPrice() : BigDecimal.ZERO);
            ps.setString(6, c.getStatus() != null ? c.getStatus() : "en_attente");
            ps.setString(7, c.getNotes());
            ps.setTimestamp(8, c.getCommandeDate() != null ? Timestamp.valueOf(c.getCommandeDate()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(9, c.getDeliveryDate() != null ? Timestamp.valueOf(c.getDeliveryDate()) : null);
            ps.setTimestamp(10, c.getCreatedAt() != null ? Timestamp.valueOf(c.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(11, c.getStripePaymentIntentId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
            if (c.getCommandeNumber() == null) c.setCommandeNumber(getById(c.getId()).getCommandeNumber());
        } catch (SQLException e) {
            System.out.println("Erreur add commande: " + e.getMessage());
        }
    }

    @Override
    public void update(Commande c) {
        String q = "UPDATE commande SET commande_number=?, product_id=?, user_id=?, quantity=?, total_price=?, status=?, notes=?, commande_date=?, delivery_date=?, stripe_payment_intent_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setString(1, c.getCommandeNumber());
            ps.setInt(2, c.getProductId());
            if (c.getUserId() > 0) ps.setInt(3, c.getUserId()); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, c.getQuantity());
            ps.setBigDecimal(5, c.getTotalPrice());
            ps.setString(6, c.getStatus());
            ps.setString(7, c.getNotes());
            ps.setTimestamp(8, c.getCommandeDate() != null ? Timestamp.valueOf(c.getCommandeDate()) : null);
            ps.setTimestamp(9, c.getDeliveryDate() != null ? Timestamp.valueOf(c.getDeliveryDate()) : null);
            ps.setString(10, c.getStripePaymentIntentId());
            ps.setInt(11, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur update commande: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String q = "DELETE FROM commande WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur delete commande: " + e.getMessage());
        }
    }

    @Override
    public List<Commande> getAll() {
        return runListQuery("SELECT c.*, p.name AS product_name, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commande c LEFT JOIN produit p ON c.product_id = p.id " +
                "LEFT JOIN user u ON c.user_id = u.id ORDER BY c.id DESC");
    }

    @Override
    public Commande getById(int id) {
        String q = "SELECT c.*, p.name AS product_name, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commande c LEFT JOIN produit p ON c.product_id = p.id " +
                "LEFT JOIN user u ON c.user_id = u.id WHERE c.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getById commande: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Commande> getByStatus(String status) {
        return runPreparedListQuery("SELECT c.*, p.name AS product_name, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commande c LEFT JOIN produit p ON c.product_id = p.id " +
                "LEFT JOIN user u ON c.user_id = u.id WHERE c.status = ? ORDER BY c.id DESC", status);
    }

    @Override
    public List<Commande> getByProduct(int productId) {
        return runPreparedListQuery("SELECT c.*, p.name AS product_name, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commande c LEFT JOIN produit p ON c.product_id = p.id " +
                "LEFT JOIN user u ON c.user_id = u.id WHERE c.product_id = ? ORDER BY c.id DESC", productId);
    }

    @Override
    public List<Commande> getByUser(int userId) {
        return runPreparedListQuery("SELECT c.*, p.name AS product_name, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commande c LEFT JOIN produit p ON c.product_id = p.id " +
                "LEFT JOIN user u ON c.user_id = u.id WHERE c.user_id = ? ORDER BY c.id DESC", userId);
    }

    @Override
    public List<Commande> search(String keyword) {
        List<Commande> list = new ArrayList<>();
        String q = "SELECT c.*, p.name AS product_name, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commande c LEFT JOIN produit p ON c.product_id = p.id " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "WHERE c.commande_number LIKE ? OR p.name LIKE ? OR c.status LIKE ? OR u.nom LIKE ? OR u.prenom LIKE ? " +
                "ORDER BY c.id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            String like = "%" + (keyword == null ? "" : keyword) + "%";
            for (int i = 1; i <= 5; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.out.println("Erreur search commande: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void updateStatus(int id, String status) {
        String q = "UPDATE commande SET status = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateStatus commande: " + e.getMessage());
        }
    }

    private List<Commande> runListQuery(String q) {
        List<Commande> list = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getAll commande: " + e.getMessage());
        }
        return list;
    }

    private List<Commande> runPreparedListQuery(String q, int value) {
        List<Commande> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.out.println("Erreur query commande: " + e.getMessage());
        }
        return list;
    }

    private List<Commande> runPreparedListQuery(String q, String value) {
        List<Commande> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.out.println("Erreur query commande: " + e.getMessage());
        }
        return list;
    }

    private String generateCommandeNumber() {
        return "CMD-" + System.currentTimeMillis();
    }

    private Commande map(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setId(rs.getInt("id"));
        c.setCommandeNumber(rs.getString("commande_number"));
        c.setProductId(rs.getInt("product_id"));
        c.setQuantity(rs.getInt("quantity"));
        BigDecimal total = rs.getBigDecimal("total_price");
        c.setTotalPrice(total != null ? total : BigDecimal.ZERO);
        c.setStatus(rs.getString("status"));
        c.setNotes(rs.getString("notes"));
        Timestamp commandeDate = rs.getTimestamp("commande_date");
        if (commandeDate != null) c.setCommandeDate(commandeDate.toLocalDateTime());
        Timestamp deliveryDate = rs.getTimestamp("delivery_date");
        if (deliveryDate != null) c.setDeliveryDate(deliveryDate.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) c.setCreatedAt(createdAt.toLocalDateTime());
        c.setStripePaymentIntentId(getOptionalString(rs, "stripe_payment_intent_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setProductName(getOptionalString(rs, "product_name"));
        String nom = getOptionalString(rs, "user_nom");
        String prenom = getOptionalString(rs, "user_prenom");
        if (nom != null || prenom != null) {
            c.setUserFullName(((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim());
        }
        return c;
    }

    private String getOptionalString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }
}
