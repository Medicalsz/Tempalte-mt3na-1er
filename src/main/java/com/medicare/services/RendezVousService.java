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
        // L'utilisateur IS le patient : on retourne directement son user_id
        return userId;
    }


    // ==================== SPECIALITES ====================

    public List<Specialite> getAllSpecialites() {
        List<Specialite> list = new ArrayList<>();
        String q = "SELECT * FROM specialite WHERE active = 1 ORDER BY nom";
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
        } catch (SQLException e) { System.out.println("Erreur specialites: " + e.getMessage()); }
        return list;
    }

    // ==================== MEDECINS PAR SPECIALITE ====================

    public List<Medecin> getMedecinsBySpecialite(int specialiteId) {
        List<Medecin> list = new ArrayList<>();
        // Match by specialite_ref_id OR by specialite text name (fallback for doctors without ref_id set)
        String q = "SELECT m.id, m.user_id, m.specialite, m.cabinet, m.bio, m.specialite_ref_id, " +
                   "m.rating_average, m.experience_years, m.consultation_duration, m.is_available_online, " +
                   "u.nom, u.prenom, u.email, u.photo " +
                   "FROM medecin m " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE m.specialite_ref_id = ? " +
                   "   OR m.specialite = (SELECT nom FROM specialite WHERE id = ?) " +
                   "ORDER BY u.nom";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, specialiteId);
            ps.setInt(2, specialiteId);
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
                m.setPhoto(rs.getString("photo"));
                m.setRatingAverage(rs.getDouble("rating_average"));
                m.setExperienceYears(rs.getInt("experience_years"));
                m.setConsultationDuration(rs.getInt("consultation_duration"));
                m.setAvailableOnline(rs.getBoolean("is_available_online"));
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
        String q = "INSERT INTO rendez_vous (medecin_id, patient_id, date, heure, statut, motif) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, rv.getMedecinId());
            ps.setInt(2, rv.getPatientId());
            ps.setDate(3, Date.valueOf(rv.getDate()));
            ps.setTime(4, Time.valueOf(rv.getHeure()));
            ps.setString(5, rv.getStatut());
            ps.setString(6, rv.getMotif());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Fallback: column might not exist yet (pre-migration DB)
            if (e.getMessage() != null && e.getMessage().contains("motif")) {
                try {
                    PreparedStatement ps2 = cnx.prepareStatement(
                        "INSERT INTO rendez_vous (medecin_id, patient_id, date, heure, statut) VALUES (?, ?, ?, ?, ?)");
                    ps2.setInt(1, rv.getMedecinId());
                    ps2.setInt(2, rv.getPatientId());
                    ps2.setDate(3, Date.valueOf(rv.getDate()));
                    ps2.setTime(4, Time.valueOf(rv.getHeure()));
                    ps2.setString(5, rv.getStatut());
                    ps2.executeUpdate();
                    return true;
                } catch (SQLException e2) { System.out.println("Erreur create RV fallback: " + e2.getMessage()); }
            } else {
                System.out.println("Erreur create RV: " + e.getMessage());
            }
        }
        return false;
    }

    public List<RendezVous> getByPatient(int patientId) {
        List<RendezVous> list = new ArrayList<>();
        // Try with hidden_by_patient filter (requires migration); fall back without it
        String q = "SELECT rv.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite " +
                   "FROM rendez_vous rv " +
                   "JOIN medecin m ON rv.medecin_id = m.id " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE rv.patient_id = ? AND rv.hidden_by_patient = 0 " +
                   "ORDER BY rv.date DESC, rv.heure DESC";
        String qFallback = "SELECT rv.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite " +
                   "FROM rendez_vous rv " +
                   "JOIN medecin m ON rv.medecin_id = m.id " +
                   "JOIN user u ON m.user_id = u.id " +
                   "WHERE rv.patient_id = ? " +
                   "ORDER BY rv.date DESC, rv.heure DESC";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, patientId);
            ResultSet rs;
            try {
                rs = ps.executeQuery();
            } catch (SQLException ex) {
                // hidden_by_patient column not yet in DB — use fallback
                ps = cnx.prepareStatement(qFallback);
                ps.setInt(1, patientId);
                rs = ps.executeQuery();
            }
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
                try { rv.setMotif(rs.getString("motif")); } catch (SQLException ignore) {}
                try { rv.setReportPending(rs.getBoolean("report_pending_patient_response")); } catch (SQLException ignore) {}
                try {
                    java.sql.Date pd = rs.getDate("proposed_date");
                    if (pd != null) rv.setProposedDate(pd.toLocalDate());
                } catch (SQLException ignore) {}
                try {
                    java.sql.Time ph = rs.getTime("proposed_heure");
                    if (ph != null) rv.setProposedHeure(ph.toLocalTime());
                } catch (SQLException ignore) {}
                list.add(rv);
            }
        } catch (SQLException e) { System.out.println("Erreur list RV: " + e.getMessage()); }
        return list;
    }

    public RendezVous getById(int id) {
        String q = "SELECT rv.*, " +
                   "um.nom AS med_nom, um.prenom AS med_prenom, m.specialite, m.cabinet, " +
                   "up.nom AS pat_nom, up.prenom AS pat_prenom " +
                   "FROM rendez_vous rv " +
                   "JOIN medecin m ON rv.medecin_id = m.id " +
                   "JOIN user um ON m.user_id = um.id " +
                   "JOIN user up ON rv.patient_id = up.id " +
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
                rv.setPatientNom(rs.getString("pat_nom"));
                rv.setPatientPrenom(rs.getString("pat_prenom"));
                rv.setSpecialite(rs.getString("specialite"));
                try { rv.setMotif(rs.getString("motif")); } catch (SQLException ignore) {}
                try { rv.setMotifAnnulation(rs.getString("motif_annulation")); } catch (SQLException ignore) {}
                try { rv.setReportPending(rs.getBoolean("report_pending_patient_response")); } catch (SQLException ignore) {}
                try {
                    java.sql.Date pd = rs.getDate("proposed_date");
                    if (pd != null) rv.setProposedDate(pd.toLocalDate());
                } catch (SQLException ignore) {}
                try {
                    java.sql.Time ph = rs.getTime("proposed_heure");
                    if (ph != null) rv.setProposedHeure(ph.toLocalTime());
                } catch (SQLException ignore) {}
                return rv;
            }
        } catch (SQLException e) { System.out.println("Erreur getById RV: " + e.getMessage()); }
        return null;
    }

    public boolean update(RendezVous rv) {
        String q = "UPDATE rendez_vous SET medecin_id=?, date=?, heure=?, statut=?, motif=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, rv.getMedecinId());
            ps.setDate(2, Date.valueOf(rv.getDate()));
            ps.setTime(3, Time.valueOf(rv.getHeure()));
            ps.setString(4, rv.getStatut());
            ps.setString(5, rv.getMotif());
            ps.setInt(6, rv.getId());
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
                   "JOIN user u ON rv.patient_id = u.id " +
                   "WHERE rv.medecin_id = ? AND rv.hidden_by_medecin = 0 " +
                   "ORDER BY rv.date DESC, rv.heure DESC";
        String qFallback = "SELECT rv.*, u.nom AS pat_nom, u.prenom AS pat_prenom " +
                   "FROM rendez_vous rv " +
                   "JOIN user u ON rv.patient_id = u.id " +
                   "WHERE rv.medecin_id = ? " +
                   "ORDER BY rv.date DESC, rv.heure DESC";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, medecinId);
            ResultSet rs;
            try {
                rs = ps.executeQuery();
            } catch (SQLException ex) {
                ps = cnx.prepareStatement(qFallback);
                ps.setInt(1, medecinId);
                rs = ps.executeQuery();
            }
            while (rs.next()) {
                RendezVous rv = new RendezVous();
                rv.setId(rs.getInt("id"));
                rv.setMedecinId(rs.getInt("medecin_id"));
                rv.setPatientId(rs.getInt("patient_id"));
                rv.setDate(rs.getDate("date").toLocalDate());
                rv.setHeure(rs.getTime("heure").toLocalTime());
                rv.setStatut(rs.getString("statut"));
                rv.setPatientNom(rs.getString("pat_nom"));
                rv.setPatientPrenom(rs.getString("pat_prenom"));
                try { rv.setReportPending(rs.getBoolean("report_pending_patient_response")); } catch (SQLException ignore) {}
                try {
                    java.sql.Date pd = rs.getDate("proposed_date");
                    if (pd != null) rv.setProposedDate(pd.toLocalDate());
                } catch (SQLException ignore) {}
                try {
                    java.sql.Time ph = rs.getTime("proposed_heure");
                    if (ph != null) rv.setProposedHeure(ph.toLocalTime());
                } catch (SQLException ignore) {}
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

    public boolean refuse(int id, String motifAnnulation) {
        String q = "UPDATE rendez_vous SET statut = 'annule', motif_annulation = ? WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setString(1, motifAnnulation);
            ps.setInt(2, id);
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
                   "JOIN user up ON rv.patient_id = up.id " +
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
                try { rv.setMotif(rs.getString("motif")); } catch (SQLException ignore) {}
                list.add(rv);
            }
        } catch (SQLException e) { System.out.println("Erreur getAllRdv: " + e.getMessage()); }
        return list;
    }

    // ==================== REPORT / RESCHEDULE ====================

    public boolean acceptReport(int id) {
        String q = "UPDATE rendez_vous SET date = proposed_date, heure = proposed_heure, " +
                   "proposed_date = NULL, proposed_heure = NULL, report_pending_patient_response = 0 WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur acceptReport: " + e.getMessage()); }
        return false;
    }

    public boolean refuseReport(int id) {
        String q = "UPDATE rendez_vous SET statut = 'annule', proposed_date = NULL, proposed_heure = NULL, " +
                   "report_pending_patient_response = 0 WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur refuseReport: " + e.getMessage()); }
        return false;
    }

    public boolean cancelReport(int id) {
        String q = "UPDATE rendez_vous SET proposed_date = NULL, proposed_heure = NULL, " +
                   "report_pending_patient_response = 0 WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur cancelReport: " + e.getMessage()); }
        return false;
    }

    // ==================== DUPLICATE CHECK ====================

    public boolean patientADejaRdvCeJour(int patientId, int medecinId, LocalDate date, int excludeRdvId) {
        String q = "SELECT COUNT(*) FROM rendez_vous WHERE patient_id = ? AND medecin_id = ? AND date = ? " +
                   "AND statut != 'annule' AND id != ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, patientId);
            ps.setInt(2, medecinId);
            ps.setDate(3, Date.valueOf(date));
            ps.setInt(4, excludeRdvId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { System.out.println("Erreur check rdv jour: " + e.getMessage()); }
        return false;
    }

    // ==================== RAPPEL EMAIL ====================

    public List<RendezVous> getRendezVousConfirmesParDate(LocalDate date) {
        List<RendezVous> list = new ArrayList<>();
        String q = "SELECT rv.*, " +
                   "um.nom AS med_nom, um.prenom AS med_prenom, " +
                   "up.nom AS pat_nom, up.prenom AS pat_prenom " +
                   "FROM rendez_vous rv " +
                   "JOIN medecin m ON rv.medecin_id = m.id " +
                   "JOIN user um ON m.user_id = um.id " +
                   "JOIN user up ON rv.patient_id = up.id " +
                   "WHERE rv.date = ? AND rv.statut = 'confirme' AND rv.rappel_envoye = 0";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setDate(1, Date.valueOf(date));
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
                rv.setPatientNom(rs.getString("pat_nom"));
                rv.setPatientPrenom(rs.getString("pat_prenom"));
                list.add(rv);
            }
        } catch (SQLException e) { System.out.println("Erreur getRendezVousConfirmesParDate: " + e.getMessage()); }
        return list;
    }

    public void markRappelEnvoye(int rdvId) {
        String q = "UPDATE rendez_vous SET rappel_envoye = 1 WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, rdvId);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("Erreur markRappelEnvoye: " + e.getMessage()); }
    }

    // ==================== ORDONNANCE ====================

    public boolean createOrdonnance(int rendezVousId, String contenu) {
        String q = "INSERT INTO ordonnance (rendez_vous_id, contenu) VALUES (?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, rendezVousId);
            ps.setString(2, contenu);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur createOrdonnance: " + e.getMessage()); }
        return false;
    }

    public Ordonnance getOrdonnanceByRdv(int rendezVousId) {
        String q = "SELECT o.*, rv.date AS rdv_date, " +
                   "um.nom AS med_nom, um.prenom AS med_prenom, m.specialite, m.cabinet, " +
                   "up.nom AS pat_nom, up.prenom AS pat_prenom " +
                   "FROM ordonnance o " +
                   "JOIN rendez_vous rv ON o.rendez_vous_id = rv.id " +
                   "JOIN medecin m ON rv.medecin_id = m.id " +
                   "JOIN user um ON m.user_id = um.id " +
                   "JOIN user up ON rv.patient_id = up.id " +
                   "WHERE o.rendez_vous_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, rendezVousId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Ordonnance ord = new Ordonnance();
                ord.setId(rs.getInt("id"));
                ord.setRendezVousId(rs.getInt("rendez_vous_id"));
                ord.setContenu(rs.getString("contenu"));
                ord.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                ord.setMedecinNom(rs.getString("med_nom"));
                ord.setMedecinPrenom(rs.getString("med_prenom"));
                ord.setSpecialite(rs.getString("specialite"));
                ord.setCabinet(rs.getString("cabinet"));
                ord.setPatientNom(rs.getString("pat_nom"));
                ord.setPatientPrenom(rs.getString("pat_prenom"));
                ord.setDateRdv(rs.getDate("rdv_date").toString());
                return ord;
            }
        } catch (SQLException e) { System.out.println("Erreur getOrdonnance: " + e.getMessage()); }
        return null;
    }

    public boolean hasOrdonnance(int rendezVousId) {
        String q = "SELECT COUNT(*) FROM ordonnance WHERE rendez_vous_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, rendezVousId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { System.out.println("Erreur hasOrdonnance: " + e.getMessage()); }
        return false;
    }

    // ==================== DISPONIBILITE MANAGEMENT ====================

    public void saveOrUpdateDisponibilite(Disponibilite d) {
        Disponibilite existing = getDisponibilite(d.getMedecinId(), d.getJourSemaine());
        if (existing != null) {
            String q = "UPDATE disponibilite SET ferme=?, matin_debut=?, matin_fin=?, pause_debut=?, pause_fin=?, apres_midi_debut=?, apres_midi_fin=? WHERE id=?";
            try {
                PreparedStatement ps = cnx.prepareStatement(q);
                ps.setBoolean(1, d.isFerme());
                setTimeOrNull(ps, 2, d.getMatinDebut());
                setTimeOrNull(ps, 3, d.getMatinFin());
                setTimeOrNull(ps, 4, d.getPauseDebut());
                setTimeOrNull(ps, 5, d.getPauseFin());
                setTimeOrNull(ps, 6, d.getApresMidiDebut());
                setTimeOrNull(ps, 7, d.getApresMidiFin());
                ps.setInt(8, existing.getId());
                ps.executeUpdate();
            } catch (SQLException e) { System.out.println("Erreur update dispo: " + e.getMessage()); }
        } else {
            String q = "INSERT INTO disponibilite (medecin_id, jour_semaine, ferme, matin_debut, matin_fin, pause_debut, pause_fin, apres_midi_debut, apres_midi_fin) VALUES (?,?,?,?,?,?,?,?,?)";
            try {
                PreparedStatement ps = cnx.prepareStatement(q);
                ps.setInt(1, d.getMedecinId());
                ps.setString(2, d.getJourSemaine());
                ps.setBoolean(3, d.isFerme());
                setTimeOrNull(ps, 4, d.getMatinDebut());
                setTimeOrNull(ps, 5, d.getMatinFin());
                setTimeOrNull(ps, 6, d.getPauseDebut());
                setTimeOrNull(ps, 7, d.getPauseFin());
                setTimeOrNull(ps, 8, d.getApresMidiDebut());
                setTimeOrNull(ps, 9, d.getApresMidiFin());
                ps.executeUpdate();
            } catch (SQLException e) { System.out.println("Erreur insert dispo: " + e.getMessage()); }
        }
    }

    public List<Disponibilite> getAllDisponibilites(int medecinId) {
        List<Disponibilite> list = new ArrayList<>();
        String[] jours = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
        for (String jour : jours) {
            Disponibilite d = getDisponibilite(medecinId, jour);
            if (d == null) {
                d = new Disponibilite();
                d.setMedecinId(medecinId);
                d.setJourSemaine(jour);
                d.setFerme(jour.equals("Samedi") || jour.equals("Dimanche"));
            }
            list.add(d);
        }
        return list;
    }

    private void setTimeOrNull(PreparedStatement ps, int index, LocalTime time) throws SQLException {
        if (time != null) ps.setTime(index, Time.valueOf(time));
        else ps.setNull(index, Types.TIME);
    }

    // ==================== TERMINE ====================

    public boolean terminer(int id) {
        String q = "UPDATE rendez_vous SET statut = 'termine' WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur terminer: " + e.getMessage()); }
        return false;
    }

    public boolean hideByPatient(int id) {
        String q = "UPDATE rendez_vous SET hidden_by_patient = 1 WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur hideByPatient: " + e.getMessage()); }
        return false;
    }

    public boolean hideByMedecin(int id) {
        String q = "UPDATE rendez_vous SET hidden_by_medecin = 1 WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(q);
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println("Erreur hideByMedecin: " + e.getMessage()); }
        return false;
    }
}
