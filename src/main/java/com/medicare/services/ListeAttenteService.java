package com.medicare.services;

import com.medicare.models.ListeAttente;
import com.medicare.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ListeAttenteService {

    private final Connection cnx;

    public ListeAttenteService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // Inscrire un patient en liste d'attente
    public boolean inscrire(int patientId, int medecinId, LocalDate date, LocalTime heureSouhaitee) {
        // Calculer la prochaine position
        int position = getNextPosition(medecinId, date);
        String q = "INSERT INTO liste_attente (patient_id, medecin_id, date, heure_souhaitee, position) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, patientId);
            ps.setInt(2, medecinId);
            ps.setDate(3, Date.valueOf(date));
            ps.setTime(4, heureSouhaitee != null ? Time.valueOf(heureSouhaitee) : null);
            ps.setInt(5, position);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur inscrire attente: " + e.getMessage()); }
        return false;
    }

    // Vérifier si un patient est déjà en attente pour ce médecin ce jour
    public boolean estDejaEnAttente(int patientId, int medecinId, LocalDate date) {
        String q = "SELECT COUNT(*) FROM liste_attente WHERE patient_id = ? AND medecin_id = ? AND date = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, patientId);
            ps.setInt(2, medecinId);
            ps.setDate(3, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { System.out.println("Erreur estDejaEnAttente: " + e.getMessage()); }
        return false;
    }

    // Récupérer toute la liste d'attente d'un médecin (dates futures)
    public List<ListeAttente> getByMedecin(int medecinId) {
        List<ListeAttente> list = new ArrayList<>();
        String q = "SELECT la.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite " +
                   "FROM liste_attente la " +
                   "JOIN medecin m ON la.medecin_id = m.id " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE la.medecin_id = ? AND la.date >= CURDATE() AND la.notifie = 0 " +
                   "ORDER BY la.date ASC, la.position ASC";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapListeAttente(rs));
        } catch (SQLException e) { System.out.println("Erreur getByMedecin attente: " + e.getMessage()); }
        return list;
    }

    // Compter le nombre total en attente pour un médecin
    public int countByMedecin(int medecinId) {
        String q = "SELECT COUNT(*) FROM liste_attente WHERE medecin_id = ? AND date >= CURDATE() AND notifie = 0";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println("Erreur countByMedecin: " + e.getMessage()); }
        return 0;
    }

    // Récupérer la liste d'attente d'un patient
    public List<ListeAttente> getByPatient(int patientId) {
        List<ListeAttente> list = new ArrayList<>();
        String q = "SELECT la.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite " +
                   "FROM liste_attente la " +
                   "JOIN medecin m ON la.medecin_id = m.id " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE la.patient_id = ? AND la.date >= CURDATE() " +
                   "ORDER BY la.date ASC";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapListeAttente(rs));
            }
        } catch (SQLException e) { System.out.println("Erreur getByPatient attente: " + e.getMessage()); }
        return list;
    }

    // Premier patient en attente pour ce médecin ce jour
    public ListeAttente getPremierEnAttente(int medecinId, LocalDate date) {
        String q = "SELECT la.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite " +
                   "FROM liste_attente la " +
                   "JOIN medecin m ON la.medecin_id = m.id " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE la.medecin_id = ? AND la.date = ? AND la.notifie = 0 " +
                   "ORDER BY la.position ASC LIMIT 1";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ps.setDate(2, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapListeAttente(rs);
        } catch (SQLException e) { System.out.println("Erreur getPremierEnAttente: " + e.getMessage()); }
        return null;
    }

    // Marquer comme notifié
    public void marquerNotifie(int id) {
        String q = "UPDATE liste_attente SET notifie = 1 WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("Erreur marquerNotifie: " + e.getMessage()); }
    }

    // Supprimer une inscription (patient se retire)
    public boolean supprimer(int id) {
        String q = "DELETE FROM liste_attente WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur supprimer attente: " + e.getMessage()); }
        return false;
    }

    // Nettoyer les inscriptions expirées (dates passées) et notifier les patients
    public List<ListeAttente> getExpireesNonNotifiees() {
        List<ListeAttente> list = new ArrayList<>();
        String q = "SELECT la.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite " +
                   "FROM liste_attente la " +
                   "JOIN medecin m ON la.medecin_id = m.id " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE la.date < CURDATE() AND la.notifie = 0";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(q);
            while (rs.next()) {
                list.add(mapListeAttente(rs));
            }
        } catch (SQLException e) { System.out.println("Erreur getExpirees: " + e.getMessage()); }
        return list;
    }

    public void supprimerExpireesNotifiees() {
        String q = "DELETE FROM liste_attente WHERE date < CURDATE() AND notifie = 1";
        try {
            cnx.createStatement().executeUpdate(q);
        } catch (SQLException e) { System.out.println("Erreur supprimerExpirees: " + e.getMessage()); }
    }

    private int getNextPosition(int medecinId, LocalDate date) {
        String q = "SELECT COUNT(*) FROM liste_attente WHERE medecin_id = ? AND date = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ps.setDate(2, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) + 1;
        } catch (SQLException e) { System.out.println("Erreur getNextPosition: " + e.getMessage()); }
        return 1;
    }

    private ListeAttente mapListeAttente(ResultSet rs) throws SQLException {
        ListeAttente la = new ListeAttente();
        la.setId(rs.getInt("id"));
        la.setPatientId(rs.getInt("patient_id"));
        la.setMedecinId(rs.getInt("medecin_id"));
        la.setDate(rs.getDate("date").toLocalDate());
        Time t = rs.getTime("heure_souhaitee");
        if (t != null) la.setHeureSouhaitee(t.toLocalTime());
        la.setPosition(rs.getInt("position"));
        la.setNotifie(rs.getBoolean("notifie"));
        la.setMedecinNom(rs.getString("med_nom"));
        la.setMedecinPrenom(rs.getString("med_prenom"));
        la.setSpecialite(rs.getString("specialite"));
        return la;
    }
}

