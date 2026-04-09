package com.medicare.services;

import com.medicare.models.*;
import com.medicare.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class RendezVousService {

    private final Connection cnx;

    public RendezVousService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ==================== PATIENT ID ====================

    public int getPatientIdByUserId(int userId) {
        String q = "SELECT id FROM patient WHERE user_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { System.out.println("Erreur getPatientId: " + e.getMessage()); }

        // Patient n'existe pas → on le crée automatiquement
        return createPatientForUser(userId);
    }

    private int createPatientForUser(int userId) {
        String q = "INSERT INTO patient (user_id) VALUES (?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(q, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int patientId = keys.getInt(1);
                System.out.println("Patient cree automatiquement, id=" + patientId);
                return patientId;
            }
        } catch (SQLException e) { System.out.println("Erreur createPatient: " + e.getMessage()); }
        return -1;
    }

    // ==================== SPECIALITES ====================

    public List<Specialite> getAllSpecialites() {
        List<Specialite> list = new ArrayList<>();
        String q = "SELECT DISTINCT s.* FROM specialite s " +
                   "JOIN medecin m ON m.specialite_ref_id = s.id " +
                   "ORDER BY s.nom";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(q);
            while (rs.next()) {
                Specialite s = new Specialite();
                s.setId(rs.getInt("id"));
                s.setNom(rs.getString("nom"));
                s.setSlug(rs.getString("slug"));
                s.setActive(rs.getBoolean("active"));
                list.add(s);
            }
            System.out.println("Specialites chargees (avec medecins): " + list.size());
        } catch (SQLException e) { System.out.println("Erreur specialites: " + e.getMessage()); }
        return list;
    }

    // ==================== MEDECINS PAR SPECIALITE ====================

    public List<Medecin> getMedecinsBySpecialite(int specialiteId) {
        List<Medecin> list = new ArrayList<>();
        String q = "SELECT m.*, u.nom, u.prenom, u.email FROM medecin m " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE m.specialite_ref_id = ? ORDER BY u.nom";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, specialiteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Medecin m = new Medecin();
                m.setId(rs.getInt("id"));
                m.setUserId(rs.getInt("user_id"));
                m.setSpecialite(rs.getString("specialite"));
                m.setCabinet(rs.getString("cabinet"));
                m.setBio(rs.getString("bio"));
                m.setNom(rs.getString("nom"));
                m.setPrenom(rs.getString("prenom"));
                m.setEmail(rs.getString("email"));
                list.add(m);
            }
        } catch (SQLException e) { System.out.println("Erreur medecins: " + e.getMessage()); }
        return list;
    }

    // ==================== DISPONIBILITES ====================

    public Disponibilite getDisponibilite(int medecinId, String jourSemaine) {
        String q = "SELECT * FROM disponibilite WHERE medecin_id = ? AND jour_semaine = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ps.setString(2, jourSemaine);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Disponibilite d = new Disponibilite();
                d.setId(rs.getInt("id"));
                d.setMedecinId(rs.getInt("medecin_id"));
                d.setJourSemaine(rs.getString("jour_semaine"));
                d.setFerme(rs.getBoolean("ferme"));
                Time t;
                t = rs.getTime("matin_debut");   if (t != null) d.setMatinDebut(t.toLocalTime());
                t = rs.getTime("matin_fin");     if (t != null) d.setMatinFin(t.toLocalTime());
                t = rs.getTime("pause_debut");   if (t != null) d.setPauseDebut(t.toLocalTime());
                t = rs.getTime("pause_fin");     if (t != null) d.setPauseFin(t.toLocalTime());
                t = rs.getTime("apres_midi_debut"); if (t != null) d.setApresMidiDebut(t.toLocalTime());
                t = rs.getTime("apres_midi_fin");   if (t != null) d.setApresMidiFin(t.toLocalTime());
                return d;
            }
        } catch (SQLException e) { System.out.println("Erreur dispo: " + e.getMessage()); }
        return null;
    }

    // ==================== CRENEAUX DISPONIBLES ====================

    public List<LocalTime> getCreneauxDisponibles(int medecinId, LocalDate date) {
        List<LocalTime> creneaux = new ArrayList<>();

        // Trouver le jour de la semaine en francais
        String jour = switch (date.getDayOfWeek()) {
            case MONDAY    -> "Lundi";
            case TUESDAY   -> "Mardi";
            case WEDNESDAY -> "Mercredi";
            case THURSDAY  -> "Jeudi";
            case FRIDAY    -> "Vendredi";
            case SATURDAY  -> "Samedi";
            case SUNDAY    -> "Dimanche";
        };

        Disponibilite dispo = getDisponibilite(medecinId, jour);
        if (dispo == null || dispo.isFerme()) return creneaux;

        // Generer les creneaux toutes les 30 min (matin + apres-midi, hors pause)
        if (dispo.getMatinDebut() != null && dispo.getMatinFin() != null) {
            LocalTime t = dispo.getMatinDebut();
            while (t.isBefore(dispo.getMatinFin())) {
                if (!isDuringPause(t, dispo)) creneaux.add(t);
                t = t.plusMinutes(30);
            }
        }
        if (dispo.getApresMidiDebut() != null && dispo.getApresMidiFin() != null) {
            LocalTime t = dispo.getApresMidiDebut();
            while (t.isBefore(dispo.getApresMidiFin())) {
                creneaux.add(t);
                t = t.plusMinutes(30);
            }
        }
        return creneaux;
    }

    private boolean isDuringPause(LocalTime t, Disponibilite d) {
        if (d.getPauseDebut() == null || d.getPauseFin() == null) return false;
        return !t.isBefore(d.getPauseDebut()) && t.isBefore(d.getPauseFin());
    }

    public List<LocalTime> getHeuresPrises(int medecinId, LocalDate date) {
        List<LocalTime> prises = new ArrayList<>();
        String q = "SELECT heure FROM rendez_vous WHERE medecin_id = ? AND date = ? AND statut != 'annule'";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ps.setDate(2, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                prises.add(rs.getTime("heure").toLocalTime());
            }
        } catch (SQLException e) { System.out.println("Erreur heures prises: " + e.getMessage()); }
        return prises;
    }

    // ==================== CRUD RENDEZ-VOUS ====================

    public boolean create(RendezVous rv) {
        String q = "INSERT INTO rendez_vous (medecin_id, patient_id, date, heure, statut) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, rv.getMedecinId());
            ps.setInt(2, rv.getPatientId());
            ps.setDate(3, Date.valueOf(rv.getDate()));
            ps.setTime(4, Time.valueOf(rv.getHeure()));
            ps.setString(5, rv.getStatut());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur create RV: " + e.getMessage()); }
        return false;
    }

    public List<RendezVous> getByPatient(int patientId) {
        List<RendezVous> list = new ArrayList<>();
        String q = "SELECT rv.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite " +
                   "FROM rendez_vous rv " +
                   "JOIN medecin m ON rv.medecin_id = m.id " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE rv.patient_id = ? AND rv.hidden_by_patient = 0 " +
                   "ORDER BY rv.date DESC, rv.heure DESC";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RendezVous rv = new RendezVous();
                rv.setId(rs.getInt("id"));
                rv.setMedecinId(rs.getInt("medecin_id"));
                rv.setPatientId(rs.getInt("patient_id"));
                rv.setDate(rs.getDate("date").toLocalDate());
                rv.setHeure(rs.getTime("heure").toLocalTime());
                rv.setStatut(rs.getString("statut"));
                rv.setMedecinNom(rs.getString("med_nom"));
                rv.setMedecinPrenom(rs.getString("med_prenom"));
                rv.setSpecialite(rs.getString("specialite"));
                list.add(rv);
            }
        } catch (SQLException e) { System.out.println("Erreur list RV: " + e.getMessage()); }
        return list;
    }

    public RendezVous getById(int id) {
        String q = "SELECT rv.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite, m.cabinet " +
                   "FROM rendez_vous rv " +
                   "JOIN medecin m ON rv.medecin_id = m.id " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE rv.id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                RendezVous rv = new RendezVous();
                rv.setId(rs.getInt("id"));
                rv.setMedecinId(rs.getInt("medecin_id"));
                rv.setPatientId(rs.getInt("patient_id"));
                rv.setDate(rs.getDate("date").toLocalDate());
                rv.setHeure(rs.getTime("heure").toLocalTime());
                rv.setStatut(rs.getString("statut"));
                rv.setMedecinNom(rs.getString("med_nom"));
                rv.setMedecinPrenom(rs.getString("med_prenom"));
                rv.setSpecialite(rs.getString("specialite"));
                return rv;
            }
        } catch (SQLException e) { System.out.println("Erreur getById RV: " + e.getMessage()); }
        return null;
    }

    public boolean update(RendezVous rv) {
        String q = "UPDATE rendez_vous SET medecin_id=?, date=?, heure=?, statut=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, rv.getMedecinId());
            ps.setDate(2, Date.valueOf(rv.getDate()));
            ps.setTime(3, Time.valueOf(rv.getHeure()));
            ps.setString(4, rv.getStatut());
            ps.setInt(5, rv.getId());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur update RV: " + e.getMessage()); }
        return false;
    }

    public boolean delete(int id) {
        String q = "DELETE FROM rendez_vous WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur delete RV: " + e.getMessage()); }
        return false;
    }

    public boolean cancel(int id) {
        String q = "UPDATE rendez_vous SET statut = 'annule' WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur cancel RV: " + e.getMessage()); }
        return false;
    }

    // ==================== MEDECIN ====================

    public int getMedecinIdByUserId(int userId) {
        String q = "SELECT id FROM medecin WHERE user_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { System.out.println("Erreur getMedecinId: " + e.getMessage()); }
        return -1;
    }

    public List<RendezVous> getByMedecin(int medecinId) {
        List<RendezVous> list = new ArrayList<>();
        String q = "SELECT rv.*, u.nom AS pat_nom, u.prenom AS pat_prenom " +
                   "FROM rendez_vous rv " +
                   "JOIN patient p ON rv.patient_id = p.id " +
                   "JOIN user u ON p.user_id = u.id " +
                   "WHERE rv.medecin_id = ? AND rv.hidden_by_medecin = 0 " +
                   "ORDER BY rv.date DESC, rv.heure DESC";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RendezVous rv = new RendezVous();
                rv.setId(rs.getInt("id"));
                rv.setMedecinId(rs.getInt("medecin_id"));
                rv.setPatientId(rs.getInt("patient_id"));
                rv.setDate(rs.getDate("date").toLocalDate());
                rv.setHeure(rs.getTime("heure").toLocalTime());
                rv.setStatut(rs.getString("statut"));
                rv.setMedecinNom(rs.getString("pat_nom"));
                rv.setMedecinPrenom(rs.getString("pat_prenom"));
                list.add(rv);
            }
        } catch (SQLException e) { System.out.println("Erreur getByMedecin: " + e.getMessage()); }
        return list;
    }

    public boolean accept(int id) {
        String q = "UPDATE rendez_vous SET statut = 'confirme' WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur accept: " + e.getMessage()); }
        return false;
    }

    public boolean refuse(int id) {
        String q = "UPDATE rendez_vous SET statut = 'annule' WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur refuse: " + e.getMessage()); }
        return false;
    }

    public boolean proposeReport(int id, LocalDate newDate, LocalTime newHeure) {
        String q = "UPDATE rendez_vous SET proposed_date = ?, proposed_heure = ?, " +
                   "report_pending_patient_response = 1 WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setDate(1, Date.valueOf(newDate));
            ps.setTime(2, Time.valueOf(newHeure));
            ps.setInt(3, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur report: " + e.getMessage()); }
        return false;
    }

    // ==================== ADMIN ====================

    public List<RendezVous> getAllRendezVous() {
        List<RendezVous> list = new ArrayList<>();
        String q = "SELECT rv.*, " +
                   "um.nom AS med_nom, um.prenom AS med_prenom, " +
                   "up.nom AS pat_nom, up.prenom AS pat_prenom, " +
                   "s.nom AS spec_nom " +
                   "FROM rendez_vous rv " +
                   "JOIN medecin m ON rv.medecin_id = m.id " +
                   "JOIN user um ON m.user_id = um.id " +
                   "JOIN patient p ON rv.patient_id = p.id " +
                   "JOIN user up ON p.user_id = up.id " +
                   "LEFT JOIN specialite s ON m.specialite_ref_id = s.id " +
                   "ORDER BY rv.date DESC, rv.heure DESC";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(q);
            while (rs.next()) {
                RendezVous rv = new RendezVous();
                rv.setId(rs.getInt("id"));
                rv.setMedecinId(rs.getInt("medecin_id"));
                rv.setPatientId(rs.getInt("patient_id"));
                rv.setDate(rs.getDate("date").toLocalDate());
                rv.setHeure(rs.getTime("heure").toLocalTime());
                rv.setStatut(rs.getString("statut"));
                rv.setMedecinNom(rs.getString("med_nom"));
                rv.setMedecinPrenom(rs.getString("med_prenom"));
                rv.setPatientNom(rs.getString("pat_nom"));
                rv.setPatientPrenom(rs.getString("pat_prenom"));
                rv.setSpecialite(rs.getString("spec_nom"));
                list.add(rv);
            }
        } catch (SQLException e) { System.out.println("Erreur getAllRdv: " + e.getMessage()); }
        return list;
    }
}
