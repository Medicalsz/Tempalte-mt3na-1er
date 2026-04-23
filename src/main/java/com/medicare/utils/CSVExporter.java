package com.medicare.utils;

import com.medicare.models.RendezVous;
import com.medicare.models.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilitaire d'export CSV (compatible Excel : séparateur ';' + BOM UTF-8).
 */
public class CSVExporter {

    private static final String SEP = ";";
    private static final String NL = "\r\n";

    /** Exporte une liste d'utilisateurs en CSV. */
    public static boolean exportUsers(List<User> users, File file) {
        String[] headers = {"ID", "Nom", "Prenom", "Email", "Telephone", "Adresse", "Role", "Verifie"};
        try (BufferedWriter w = openWriter(file)) {
            writeHeader(w, headers);
            for (User u : users) {
                writeRow(w,
                        String.valueOf(u.getId()),
                        u.getNom(),
                        u.getPrenom(),
                        u.getEmail(),
                        u.getNumero(),
                        u.getAdresse(),
                        u.getRoles(),
                        u.isVerified() ? "Oui" : "Non"
                );
            }
            return true;
        } catch (Exception e) {
            System.out.println("Erreur export users CSV: " + e.getMessage());
            return false;
        }
    }

    /** Exporte une liste de rendez-vous en CSV. */
    public static boolean exportRendezVous(List<RendezVous> rdvs, File file) {
        String[] headers = {"ID", "Patient", "Medecin", "Specialite", "Date", "Heure", "Statut", "Motif annulation"};
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        try (BufferedWriter w = openWriter(file)) {
            writeHeader(w, headers);
            for (RendezVous r : rdvs) {
                writeRow(w,
                        String.valueOf(r.getId()),
                        r.getPatientFullName(),
                        "Dr. " + r.getMedecinFullName(),
                        r.getSpecialite(),
                        r.getDate() != null ? r.getDate().format(df) : "",
                        r.getHeure() != null ? r.getHeure().format(tf) : "",
                        r.getStatut(),
                        r.getMotifAnnulation()
                );
            }
            return true;
        } catch (Exception e) {
            System.out.println("Erreur export rdv CSV: " + e.getMessage());
            return false;
        }
    }

    // ---------- Helpers ----------

    private static BufferedWriter openWriter(File file) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        // BOM UTF-8 pour Excel
        fos.write(0xEF);
        fos.write(0xBB);
        fos.write(0xBF);
        return new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
    }

    private static void writeHeader(BufferedWriter w, String[] headers) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) sb.append(SEP);
            sb.append(escape(headers[i]));
        }
        sb.append(NL);
        w.write(sb.toString());
    }

    private static void writeRow(BufferedWriter w, String... values) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(SEP);
            sb.append(escape(values[i]));
        }
        sb.append(NL);
        w.write(sb.toString());
    }

    private static String escape(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(SEP) || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}

