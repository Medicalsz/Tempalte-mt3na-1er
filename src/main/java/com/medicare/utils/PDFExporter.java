package com.medicare.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.medicare.models.RendezVous;
import com.medicare.models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilitaire d'export PDF de listes (tableaux) via iText.
 */
public class PDFExporter {

    private static final BaseColor VIOLET = new BaseColor(124, 58, 237);
    private static final BaseColor GRIS = new BaseColor(100, 116, 139);
    private static final BaseColor NOIR = new BaseColor(30, 41, 59);
    private static final BaseColor LIGNE_ALT = new BaseColor(249, 250, 251);

    /** Exporte une liste d'utilisateurs en PDF. */
    public static boolean exportUsers(List<User> users, File file) {
        try {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addTitle(doc, "LISTE DES UTILISATEURS", users.size() + " utilisateur(s)");

            PdfPTable table = new PdfPTable(new float[]{0.5f, 1.3f, 1.3f, 2.2f, 1.2f, 1.2f, 0.8f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            addHeaderCells(table, "ID", "Nom", "Prenom", "Email", "Telephone", "Role", "Verifie");

            int i = 0;
            for (User u : users) {
                BaseColor bg = (i++ % 2 == 0) ? BaseColor.WHITE : LIGNE_ALT;
                addBodyCell(table, String.valueOf(u.getId()), bg);
                addBodyCell(table, safe(u.getNom()), bg);
                addBodyCell(table, safe(u.getPrenom()), bg);
                addBodyCell(table, safe(u.getEmail()), bg);
                addBodyCell(table, safe(u.getNumero()), bg);
                addBodyCell(table, safe(u.getRoles()), bg);
                addBodyCell(table, u.isVerified() ? "Oui" : "Non", bg);
            }

            doc.add(table);
            addFooter(doc);
            doc.close();
            return true;
        } catch (Exception e) {
            System.out.println("Erreur export users PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /** Exporte une liste de rendez-vous en PDF. */
    public static boolean exportRendezVous(List<RendezVous> rdvs, File file) {
        try {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addTitle(doc, "LISTE DES RENDEZ-VOUS", rdvs.size() + " rendez-vous");

            PdfPTable table = new PdfPTable(new float[]{0.5f, 1.6f, 1.6f, 1.4f, 1f, 0.8f, 1f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            addHeaderCells(table, "ID", "Patient", "Medecin", "Specialite", "Date", "Heure", "Statut");

            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

            int i = 0;
            for (RendezVous r : rdvs) {
                BaseColor bg = (i++ % 2 == 0) ? BaseColor.WHITE : LIGNE_ALT;
                addBodyCell(table, String.valueOf(r.getId()), bg);
                addBodyCell(table, safe(r.getPatientFullName()), bg);
                addBodyCell(table, "Dr. " + safe(r.getMedecinFullName()), bg);
                addBodyCell(table, safe(r.getSpecialite()), bg);
                addBodyCell(table, r.getDate() != null ? r.getDate().format(df) : "", bg);
                addBodyCell(table, r.getHeure() != null ? r.getHeure().format(tf) : "", bg);
                addStatusCell(table, safe(r.getStatut()));
            }

            doc.add(table);
            addFooter(doc);
            doc.close();
            return true;
        } catch (Exception e) {
            System.out.println("Erreur export rdv PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ---------- Helpers ----------

    private static void addTitle(Document doc, String title, String subtitle) throws DocumentException {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, VIOLET);
        Font subFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, GRIS);

        Paragraph p = new Paragraph(title, titleFont);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);

        Paragraph sub = new Paragraph(subtitle + "  -  Genere le " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(5);
        doc.add(sub);
    }

    private static void addFooter(Document doc) throws DocumentException {
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, GRIS);
        Paragraph footer = new Paragraph("Medicare - Document genere automatiquement", smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        doc.add(footer);
    }

    private static void addHeaderCells(PdfPTable table, String... headers) {
        Font f = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, f));
            c.setBackgroundColor(VIOLET);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(7);
            c.setBorder(Rectangle.NO_BORDER);
            table.addCell(c);
        }
    }

    private static void addBodyCell(PdfPTable table, String text, BaseColor bg) {
        Font f = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, NOIR);
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setPadding(6);
        c.setBorderColor(new BaseColor(229, 231, 235));
        c.setBorderWidth(0.3f);
        table.addCell(c);
    }

    private static void addStatusCell(PdfPTable table, String statut) {
        BaseColor color;
        switch (statut) {
            case "confirme":  color = new BaseColor(22, 163, 74);  break;
            case "annule":    color = new BaseColor(220, 38, 38);  break;
            case "en_attente":color = new BaseColor(245, 158, 11); break;
            case "termine":   color = new BaseColor(59, 130, 246); break;
            default:          color = GRIS;
        }
        Font f = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        PdfPCell c = new PdfPCell(new Phrase(statut, f));
        c.setBackgroundColor(color);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(6);
        c.setBorder(Rectangle.NO_BORDER);
        table.addCell(c);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}

