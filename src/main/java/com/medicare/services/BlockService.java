package com.medicare.services;

import com.medicare.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlockService {

    public record BlockedUser(
        int    id,
        String nom,
        String prenom,
        String email,
        String photo,
        String roles,
        String blockedAt
    ) {
        public String displayName() {
            String full = ((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim();
            return full.isEmpty() ? email : full;
        }

        public String roleLabel() {
            if (roles == null) return "Utilisateur";
            if (roles.contains("ROLE_MEDECIN")) return "Médecin";
            if (roles.contains("ROLE_ADMIN"))   return "Admin";
            return "Patient";
        }
    }

    private final Connection cnx;

    public BlockService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public boolean blockUser(int blockerId, int blockedId) {
        if (blockerId == blockedId) return false;
        String sql = "INSERT IGNORE INTO user_block (blocker_id, blocked_id) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur blockUser: " + e.getMessage());
        }
        return false;
    }

    public boolean unblockUser(int blockerId, int blockedId) {
        String sql = "DELETE FROM user_block WHERE blocker_id = ? AND blocked_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur unblockUser: " + e.getMessage());
        }
        return false;
    }

    public boolean isBlocked(int blockerId, int blockedId) {
        String sql = "SELECT 1 FROM user_block WHERE blocker_id = ? AND blocked_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Erreur isBlocked: " + e.getMessage());
        }
        return false;
    }

    public List<BlockedUser> getBlockList(int blockerId) {
        List<BlockedUser> list = new ArrayList<>();
        String sql = """
            SELECT u.id, u.nom, u.prenom, u.email, u.photo, u.roles,
                   DATE_FORMAT(ub.created_at, '%d/%m/%Y') AS blocked_at
            FROM user_block ub
            JOIN user u ON u.id = ub.blocked_id
            WHERE ub.blocker_id = ?
            ORDER BY ub.created_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new BlockedUser(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("photo"),
                    rs.getString("roles"),
                    rs.getString("blocked_at")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Erreur getBlockList: " + e.getMessage());
        }
        return list;
    }

    public int getBlockCount(int blockerId) {
        String sql = "SELECT COUNT(*) FROM user_block WHERE blocker_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Erreur getBlockCount: " + e.getMessage());
        }
        return 0;
    }
}
