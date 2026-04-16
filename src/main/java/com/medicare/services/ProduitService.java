package com.medicare.services;

import com.medicare.interfaces.IProduitService;
import com.medicare.models.Produit;
import com.medicare.utils.MyConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements IProduitService {

    private final Connection cnx;

    public ProduitService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(Produit p) {
        String q = "INSERT INTO produit (name, description, sku, price, quantity, type, dosage, " +
                   "expiry_date, is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getSku());
            ps.setBigDecimal(4, p.getPrice());
            ps.setInt(5, p.getQuantity());
            ps.setString(6, p.getType());
            ps.setString(7, p.getDosage());
            ps.setTimestamp(8, p.getExpiryDate() != null ? Timestamp.valueOf(p.getExpiryDate()) : null);
            ps.setBoolean(9, p.isActive());
            ps.setTimestamp(10, p.getCreatedAt() != null
                    ? Timestamp.valueOf(p.getCreatedAt())
                    : Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
            System.out.println("Produit ajoute: " + p.getName());
        } catch (SQLException e) {
            System.out.println("Erreur add produit: " + e.getMessage());
        }
    }

    @Override
    public void update(Produit p) {
        String q = "UPDATE produit SET name=?, description=?, sku=?, price=?, quantity=?, " +
                   "type=?, dosage=?, expiry_date=?, is_active=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getSku());
            ps.setBigDecimal(4, p.getPrice());
            ps.setInt(5, p.getQuantity());
            ps.setString(6, p.getType());
            ps.setString(7, p.getDosage());
            ps.setTimestamp(8, p.getExpiryDate() != null ? Timestamp.valueOf(p.getExpiryDate()) : null);
            ps.setBoolean(9, p.isActive());
            ps.setInt(10, p.getId());
            ps.executeUpdate();
            System.out.println("Produit mis a jour: id=" + p.getId());
        } catch (SQLException e) {
            System.out.println("Erreur update produit: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String q = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Produit supprime: id=" + id);
        } catch (SQLException e) {
            System.out.println("Erreur delete produit: " + e.getMessage());
        }
    }

    @Override
    public List<Produit> getAll() {
        List<Produit> list = new ArrayList<>();
        String q = "SELECT * FROM produit ORDER BY id DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getAll produit: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Produit getById(int id) {
        String q = "SELECT * FROM produit WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.out.println("Erreur getById produit: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Produit> search(String keyword) {
        List<Produit> list = new ArrayList<>();
        String q = "SELECT * FROM produit WHERE name LIKE ? OR sku LIKE ? OR type LIKE ? ORDER BY id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            String like = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur search produit: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Produit> getActive() {
        List<Produit> list = new ArrayList<>();
        String q = "SELECT * FROM produit WHERE is_active = 1 ORDER BY name";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getActive produit: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void toggleActive(int id, boolean active) {
        String q = "UPDATE produit SET is_active = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur toggleActive produit: " + e.getMessage());
        }
    }

    private Produit map(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setSku(rs.getString("sku"));
        BigDecimal price = rs.getBigDecimal("price");
        p.setPrice(price != null ? price : BigDecimal.ZERO);
        p.setQuantity(rs.getInt("quantity"));
        p.setType(rs.getString("type"));
        p.setDosage(rs.getString("dosage"));
        Timestamp exp = rs.getTimestamp("expiry_date");
        if (exp != null) p.setExpiryDate(exp.toLocalDateTime());
        p.setActive(rs.getBoolean("is_active"));
        Timestamp cr = rs.getTimestamp("created_at");
        if (cr != null) p.setCreatedAt(cr.toLocalDateTime());
        return p;
    }
}
