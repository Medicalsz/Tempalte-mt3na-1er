package com.medicare.services;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.medicare.models.Commande;
import com.medicare.models.Produit;
import com.medicare.models.User;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Generates an invoice PDF for a Commande.
 * The PDF embeds a signed QR code that can be verified via QrVerifyController.
 */
public class FacturePdfService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color BRAND = new Color(124, 58, 237);   // #7c3aed
    private static final Color BRAND_LIGHT = new Color(237, 233, 254);
    private static final Color GREY = new Color(107, 114, 128);

    private final QrCodeService qrService = new QrCodeService();

    /**
     * Build the invoice and write it to {@code outputFile}.
     * @param commande the commande to invoice (must be hydrated with productName / userFullName when possible)
     * @param produit  the product (for unit price); may be null
     * @param user     the patient (for nom/email); may be null
     * @param outputFile destination file
     */
    public void generate(Commande commande, Produit produit, User user, File outputFile) {
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();

            addHeader(doc, commande);
            doc.add(new Paragraph(" "));
            addClientBlock(doc, commande, user);
            doc.add(new Paragraph(" "));
            addProductTable(doc, commande, produit);
            doc.add(new Paragraph(" "));
            addTotalBlock(doc, commande);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));
            addQrSection(doc, commande);
            addFooter(doc);

            doc.close();
        } catch (Exception e) {
            if (doc.isOpen()) doc.close();
            throw new RuntimeException("Echec generation PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document doc, Commande c) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2.5f, 1.5f});

        // Left: company
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        Paragraph brand = new Paragraph("MEDICARE",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, brandColor()));
        Paragraph subtitle = new Paragraph("Plateforme medicale",
                FontFactory.getFont(FontFactory.HELVETICA, 11, GREY));
        Paragraph addr = new Paragraph("Esprit, Tunisie  -  contact@medicare.tn",
                FontFactory.getFont(FontFactory.HELVETICA, 10, GREY));
        left.addElement(brand);
        left.addElement(subtitle);
        left.addElement(addr);

        // Right: invoice meta
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph title = new Paragraph("FACTURE",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, brandColor()));
        title.setAlignment(Element.ALIGN_RIGHT);
        Paragraph num = new Paragraph(
                "N° " + (c.getCommandeNumber() != null ? c.getCommandeNumber() : ("CMD-" + c.getId())),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK));
        num.setAlignment(Element.ALIGN_RIGHT);
        Paragraph date = new Paragraph(
                "Date : " + (c.getCommandeDate() != null ? c.getCommandeDate().format(DF) : "-"),
                FontFactory.getFont(FontFactory.HELVETICA, 10, GREY));
        date.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);
        right.addElement(num);
        right.addElement(date);

        header.addCell(left);
        header.addCell(right);
        doc.add(header);

        // separator
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell line = new PdfPCell();
        line.setFixedHeight(2f);
        line.setBackgroundColor(brandColor());
        line.setBorder(Rectangle.NO_BORDER);
        sep.addCell(line);
        doc.add(sep);
    }

    private void addClientBlock(Document doc, Commande c, User user) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f});

        PdfPCell left = boxCell("Client");
        String fullName = (user != null)
                ? (safe(user.getPrenom()) + " " + safe(user.getNom())).trim()
                : safe(c.getUserFullName());
        if (fullName.isBlank()) fullName = "-";
        String email = (user != null && user.getEmail() != null) ? user.getEmail() : "-";
        left.addElement(textLine("Nom : " + fullName, false));
        left.addElement(textLine("Email : " + email, false));
        left.addElement(textLine("ID : " + c.getUserId(), false));

        PdfPCell right = boxCell("Commande");
        right.addElement(textLine("Statut : " + (c.getStatus() != null ? c.getStatus() : "-"), false));
        right.addElement(textLine("Livraison : "
                + (c.getDeliveryDate() != null ? c.getDeliveryDate().format(DF) : "-"), false));
        right.addElement(textLine("Cree le : "
                + (c.getCreatedAt() != null ? c.getCreatedAt().format(DF) : "-"), false));

        t.addCell(left);
        t.addCell(right);
        doc.add(t);
    }

    private void addProductTable(Document doc, Commande c, Produit p) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{4f, 1.2f, 1.5f, 1.5f});

        // header row
        t.addCell(headerCell("Produit"));
        t.addCell(headerCell("Qte"));
        t.addCell(headerCell("Prix unit."));
        t.addCell(headerCell("Total"));

        String pname = (p != null && p.getName() != null) ? p.getName()
                : (c.getProductName() != null ? c.getProductName() : "Produit #" + c.getProductId());
        BigDecimal unit;
        if (p != null && p.getPrice() != null) {
            unit = p.getPrice();
        } else if (c.getQuantity() > 0 && c.getTotalPrice() != null) {
            unit = c.getTotalPrice().divide(BigDecimal.valueOf(c.getQuantity()), 3, java.math.RoundingMode.HALF_UP);
        } else {
            unit = BigDecimal.ZERO;
        }
        BigDecimal total = c.getTotalPrice() != null ? c.getTotalPrice() : BigDecimal.ZERO;

        t.addCell(bodyCell(pname, Element.ALIGN_LEFT));
        t.addCell(bodyCell(String.valueOf(c.getQuantity()), Element.ALIGN_CENTER));
        t.addCell(bodyCell(unit.toPlainString() + " DT", Element.ALIGN_RIGHT));
        t.addCell(bodyCell(total.toPlainString() + " DT", Element.ALIGN_RIGHT));

        if (c.getNotes() != null && !c.getNotes().isBlank()) {
            PdfPCell notes = bodyCell("Notes : " + c.getNotes(), Element.ALIGN_LEFT);
            notes.setColspan(4);
            notes.setBackgroundColor(new Color(249, 250, 251));
            t.addCell(notes);
        }
        doc.add(t);
    }

    private void addTotalBlock(Document doc, Commande c) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(50);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setWidths(new float[]{1.4f, 1f});

        BigDecimal total = c.getTotalPrice() != null ? c.getTotalPrice() : BigDecimal.ZERO;
        PdfPCell lblCell = new PdfPCell(new Phrase("TOTAL TTC",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.WHITE)));
        lblCell.setBackgroundColor(brandColor());
        lblCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        lblCell.setPadding(8);

        PdfPCell valCell = new PdfPCell(new Phrase(total.toPlainString() + " DT",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.WHITE)));
        valCell.setBackgroundColor(brandColor());
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valCell.setPadding(8);

        t.addCell(lblCell);
        t.addCell(valCell);
        doc.add(t);
    }

    private void addQrSection(Document doc, Commande c) throws Exception {
        String payload = qrService.buildSignedPayload(c);
        byte[] png = qrService.generatePng(payload, 220);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 2f});

        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Image qrImg = Image.getInstance(png);
        qrImg.scaleToFit(140, 140);
        qrCell.addElement(qrImg);

        PdfPCell info = new PdfPCell();
        info.setBorder(Rectangle.NO_BORDER);
        info.setVerticalAlignment(Element.ALIGN_MIDDLE);
        info.addElement(new Paragraph("Authentification",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, brandColor())));
        info.addElement(new Paragraph(
                "Ce QR code est signe cryptographiquement (HMAC-SHA256). " +
                "Scannez-le depuis l'application Medicare (menu Commandes > Verifier QR) " +
                "pour valider l'authenticite de cette facture.",
                FontFactory.getFont(FontFactory.HELVETICA, 10, GREY)));

        t.addCell(qrCell);
        t.addCell(info);
        doc.add(t);
    }

    private void addFooter(Document doc) throws Exception {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("Merci de votre confiance — Medicare © 2026",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, GREY)));
        doc.add(p);
    }

    // ---- helpers ----

    private Color brandColor() { return BRAND; }

    private PdfPCell headerCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
        c.setBackgroundColor(brandColor());
        c.setPadding(8);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private PdfPCell bodyCell(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK)));
        c.setPadding(7);
        c.setHorizontalAlignment(align);
        return c;
    }

    private PdfPCell boxCell(String title) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(BRAND_LIGHT);
        c.setBorderColor(BRAND);
        c.setBorderWidth(0.5f);
        c.setPadding(10);
        Paragraph h = new Paragraph(title,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, brandColor()));
        c.addElement(h);
        return c;
    }

    private Paragraph textLine(String s, boolean bold) {
        Font f = FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 10, Color.BLACK);
        return new Paragraph(s, f);
    }

    private String safe(String s) { return s == null ? "" : s; }
}
