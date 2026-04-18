package com.medicare.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.medicare.interfaces.Crud;
import com.medicare.models.Partner;
import com.medicare.utils.MyConnection;

public class PartnerService implements Crud<Partner> {

    private Connection cnx;

    public PartnerService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public void add(Partner partner) {
        String query = "INSERT INTO partner (name, type_partenaire, email, telephone, adresse, statut, date_partenariat, image_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, partner.getName());
            pst.setString(2, partner.getTypePartenaire());
            pst.setString(3, partner.getEmail());
            pst.setString(4, partner.getTelephone());
            pst.setString(5, partner.getAdresse());
            pst.setString(6, partner.getStatut());
            pst.setDate(7, java.sql.Date.valueOf(partner.getDatePartenariat()));
            pst.setString(8, partner.getImageName());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Partner partner) {
        String query = "UPDATE partner SET name = ?, type_partenaire = ?, email = ?, telephone = ?, adresse = ?, statut = ?, date_partenariat = ?, image_name = ? WHERE id = ?";
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, partner.getName());
            pst.setString(2, partner.getTypePartenaire());
            pst.setString(3, partner.getEmail());
            pst.setString(4, partner.getTelephone());
            pst.setString(5, partner.getAdresse());
            pst.setString(6, partner.getStatut());
            pst.setDate(7, java.sql.Date.valueOf(partner.getDatePartenariat()));
            pst.setString(8, partner.getImageName());
            pst.setInt(9, partner.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM partner WHERE id = ?";
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Partner> getAll() {
        List<Partner> partners = new ArrayList<>();
        String query = "SELECT * FROM partner";
        try (Connection conn = MyConnection.getInstance().getCnx();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Partner p = new Partner();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setTypePartenaire(rs.getString("type_partenaire"));
                p.setEmail(rs.getString("email"));
                p.setTelephone(rs.getString("telephone"));
                p.setAdresse(rs.getString("adresse"));
                p.setStatut(rs.getString("statut"));
                if (rs.getDate("date_partenariat") != null) {
                    p.setDatePartenariat(rs.getDate("date_partenariat").toLocalDate());
                }
                p.setImageName(rs.getString("image_name"));
                partners.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return partners;
    }

    public List<Partner> searchByName(String name) {
        List<Partner> partners = new ArrayList<>();
        String query = "SELECT * FROM partner WHERE name LIKE ?";
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, "%" + name + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Partner p = new Partner();
                    p.setId(rs.getInt("id"));
                    p.setName(rs.getString("name"));
                    p.setTypePartenaire(rs.getString("type_partenaire"));
                    p.setEmail(rs.getString("email"));
                    p.setTelephone(rs.getString("telephone"));
                    p.setAdresse(rs.getString("adresse"));
                    p.setStatut(rs.getString("statut"));
                    if (rs.getDate("date_partenariat") != null) {
                        p.setDatePartenariat(rs.getDate("date_partenariat").toLocalDate());
                    }
                    p.setImageName(rs.getString("image_name"));
                    partners.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return partners;
    }
}