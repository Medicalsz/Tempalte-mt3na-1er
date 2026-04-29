package com.medicare.services;

import com.medicare.models.Evaluation;
import com.medicare.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluationService {

    private final Connection cnx;

    public EvaluationService() {
        this.cnx = MyConnection.getInstance().getCnx();
    }

    /** Crée une évaluation. Retourne l'id généré ou -1 en cas d'échec. */
    public int create(Evaluation e) {
        String q = "INSERT INTO evaluation (rendez_vous_id, patient_id, medecin_id, note, " +
                   "note_ponctualite, note_ecoute, note_clarte, commentaire, created_at) " +
                   "VALUES (?,?,?,?,?,?,?,?, NOW())";
        try (PreparedStatement ps = cnx.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getRendezVousId());
            ps.setInt(2, e.getPatientId());
            ps.setInt(3, e.getMedecinId());
            ps.setInt(4, e.getNote());
            ps.setInt(5, e.getNotePonctualite());
            ps.setInt(6, e.getNoteEcoute());
            ps.setInt(7, e.getNoteClarte());
            if (e.getCommentaire() == null || e.getCommentaire().isBlank()) ps.setNull(8, Types.VARCHAR);
            else ps.setString(8, e.getCommentaire().trim());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException ex) {
            System.out.println("Erreur création évaluation: " + ex.getMessage());
        }
        return -1;
    }

    /** Vérifie si un RDV a déjà été évalué. */
    public boolean existsForRdv(int rdvId) {
        String q = "SELECT 1 FROM evaluation WHERE rendez_vous_id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, rdvId);
            return ps.executeQuery().next();
        } catch (SQLException ex) { return false; }
    }

    /** Récupère l'évaluation associée à un RDV (ou null). */
    public Evaluation getByRdv(int rdvId) {
        String q = "SELECT * FROM evaluation WHERE rendez_vous_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, rdvId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException ex) {
            System.out.println("Erreur getByRdv: " + ex.getMessage());
        }
        return null;
    }

    /** Liste toutes les évaluations d'un médecin (avec infos patient). */
    public List<Evaluation> getByMedecin(int medecinId) {
        List<Evaluation> list = new ArrayList<>();
        String q = "SELECT e.*, u.nom AS p_nom, u.prenom AS p_prenom " +
                   "FROM evaluation e " +
                   "JOIN patient p ON p.id = e.patient_id " +
                   "JOIN user u ON u.id = p.user_id " +
                   "WHERE e.medecin_id = ? " +
                   "ORDER BY e.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Evaluation ev = mapRow(rs);
                ev.setPatientNom(rs.getString("p_nom"));
                ev.setPatientPrenom(rs.getString("p_prenom"));
                list.add(ev);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getByMedecin: " + ex.getMessage());
        }
        return list;
    }

    /** Statistiques (moyennes + total) pour un médecin. */
    public Evaluation.Stats getStats(int medecinId) {
        Evaluation.Stats s = new Evaluation.Stats();
        String q = "SELECT COUNT(*) AS total, AVG(note) AS m, AVG(note_ponctualite) AS mp, " +
                   "AVG(note_ecoute) AS me, AVG(note_clarte) AS mc " +
                   "FROM evaluation WHERE medecin_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(q)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                s.total = rs.getInt("total");
                s.moyenneGlobale = rs.getDouble("m");
                s.moyennePonctualite = rs.getDouble("mp");
                s.moyenneEcoute = rs.getDouble("me");
                s.moyenneClarte = rs.getDouble("mc");
            }
        } catch (SQLException ex) {
            System.out.println("Erreur stats évaluation: " + ex.getMessage());
        }
        return s;
    }

    /** Stats agrégées par médecin pour la vue admin. */
    public static class MedecinStats {
        public int    medecinId;
        public String nom;
        public String prenom;
        public String specialite;
        public int    nbEvaluations;
        public double moyenne;
    }

    /**
     * Liste TOUS les médecins (même sans évaluation) avec leur nombre d'avis et leur moyenne.
     * Triée par nb d'évaluations décroissant puis par moyenne décroissante.
     */
    public List<MedecinStats> getAllMedecinStats() {
        List<MedecinStats> list = new ArrayList<>();
        String q = "SELECT m.id AS medecin_id, u.nom, u.prenom, " +
                   "       COALESCE(s.nom, '-') AS specialite, " +
                   "       COALESCE(COUNT(e.id), 0) AS nb_eval, " +
                   "       COALESCE(AVG(e.note), 0) AS moyenne " +
                   "FROM medecin m " +
                   "JOIN user u ON u.id = m.user_id " +
                   "LEFT JOIN specialite s ON s.id = m.specialite_ref_id " +
                   "LEFT JOIN evaluation e ON e.medecin_id = m.id " +
                   "GROUP BY m.id, u.nom, u.prenom, s.nom " +
                   "ORDER BY nb_eval DESC, moyenne DESC";
        try (PreparedStatement ps = cnx.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MedecinStats ms = new MedecinStats();
                ms.medecinId     = rs.getInt("medecin_id");
                ms.nom           = rs.getString("nom");
                ms.prenom        = rs.getString("prenom");
                ms.specialite    = rs.getString("specialite");
                ms.nbEvaluations = rs.getInt("nb_eval");
                ms.moyenne       = rs.getDouble("moyenne");
                list.add(ms);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getAllMedecinStats: " + ex.getMessage());
        }
        return list;
    }

    /** Stats globales toutes évaluations confondues. */
    public Evaluation.Stats getGlobalStats() {
        Evaluation.Stats s = new Evaluation.Stats();
        String q = "SELECT COUNT(*) AS total, AVG(note) AS m, AVG(note_ponctualite) AS mp, " +
                   "AVG(note_ecoute) AS me, AVG(note_clarte) AS mc FROM evaluation";
        try (PreparedStatement ps = cnx.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                s.total = rs.getInt("total");
                s.moyenneGlobale = rs.getDouble("m");
                s.moyennePonctualite = rs.getDouble("mp");
                s.moyenneEcoute = rs.getDouble("me");
                s.moyenneClarte = rs.getDouble("mc");
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getGlobalStats: " + ex.getMessage());
        }
        return s;
    }

    private Evaluation mapRow(ResultSet rs) throws SQLException {
        Evaluation e = new Evaluation();
        e.setId(rs.getInt("id"));
        e.setRendezVousId(rs.getInt("rendez_vous_id"));
        e.setPatientId(rs.getInt("patient_id"));
        e.setMedecinId(rs.getInt("medecin_id"));
        e.setNote(rs.getInt("note"));
        e.setNotePonctualite(rs.getInt("note_ponctualite"));
        e.setNoteEcoute(rs.getInt("note_ecoute"));
        e.setNoteClarte(rs.getInt("note_clarte"));
        e.setCommentaire(rs.getString("commentaire"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) e.setCreatedAt(t.toLocalDateTime());
        return e;
    }
}
