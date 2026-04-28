package com.medicare.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.medicare.models.Badge;
import com.medicare.utils.MyConnection;

public class BadgeService {

    public List<Badge> getBadgesForPartner(int partnerId) {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT b.id, b.name, b.description, b.icon_path " +
                     "FROM badges b " +
                     "JOIN partner_badges pb ON b.id = pb.badge_id " +
                     "WHERE pb.partner_id = ?";

        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partnerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                badges.add(new Badge(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("icon_path")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des badges : " + e.getMessage());
            e.printStackTrace();
        }
        return badges;
    }
}