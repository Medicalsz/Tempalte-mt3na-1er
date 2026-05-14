package com.medicare.services;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.medicare.models.Commande;
import com.medicare.models.Produit;
import com.medicare.models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class FacturePdfService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final BaseColor BRAND = new BaseColor(124, 58, 237);
    private static final BaseColor BRAND_LIGHT = new BaseColor(237, 233, 254);
    private static final BaseColor GREY = new BaseColor(107, 114, 128);

    private final QRCodeService qrService = new QRCodeService();

    public void generate(Commande commande, Produit produit, User user, File outputFile) {
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
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
            addQrSection(doc, commande);
            addFooter(doc);
        } catch (Exception e) {
            throw new RuntimeException("Echec generation PDF: " + e.getMessage(), e);
        } finally {
            if (doc.isOpen()) doc.close();
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void addHeader(Document doc, Commande c) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2.5f, 1.5f});

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("MEDICARE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24f, BRAND)));
        left.addElement(new Paragraph("Plateforme medicale", FontFactory.getFont(FontFactory.HELVETICA, 11f, GREY)));
        left.addElement(new Paragraph("Esprit, Tunisie  -  contact@medicare.tn", FontFactory.getFont(FontFactory.HELVETICA, 10f, GREY)));

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph title = new Paragraph("FACTURE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, BRAND));
        title.setAlignment(Element.ALIGN_RIGHT);
        Paragraph num = new Paragraph("N° " + (c.getCommandeNumber() != null ? c.getCommandeNumber() : ("CMD-" + c.getId())),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor.BLACK));
        num.setAlignment(Element.ALIGN_RIGHT);
        Paragraph date = new Paragraph("Date : " + (c.getCommandeDate() != null ? c.getCommandeDate().format(DF) : "-"),
                FontFactory.getFont(FontFactory.HELVETICA, 10f, GREY));
        date.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);
        right.addElement(num);
        right.addElement(date);

        header.addCell(left);
        header.addCell(right);
        doc.add(header);

        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell line = new PdfPCell();
        line.setFixedHeight(2f);
        line.setBackgroundColor(BRAND);
        line.setBorder(Rectangle.NO_BORDER);
        sep.addCell(line);
        doc.add(sep);
    }

    private void addClientBlock(Document doc, Commande c, User user) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f});

        PdfPCell left = boxCell("Client");
        String fullName = user != null ? (safe(user.getPrenom()) + " " + safe(user.getNom())).trim() : safe(c.getUserFullName());
        if (fullName.isBlank()) fullName = "-";
        String email = user != null && user.getEmail() != null ? user.getEmail() : "-";
        left.addElement(textLine("Nom : " + fullName, false));
        left.addElement(textLine("Email : " + email, false));
        left.addElement(textLine("ID : " + c.getUserId(), false));

        PdfPCell right = boxCell("Commande");
        right.addElement(textLine("Statut : " + (c.getStatus() != null ? c.getStatus() : "-"), false));
        right.addElement(textLine("Livraison : " + (c.getDeliveryDate() != null ? c.getDeliveryDate().format(DF) : "-"), false));
        right.addElement(textLine("Cree le : " + (c.getCreatedAt() != null ? c.getCreatedAt().format(DF) : "-"), false));

        t.addCell(left);
        t.addCell(right);
        doc.add(t);
    }

    private void addProductTable(Document doc, Commande c, Produit p) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{4f, 1.2f, 1.5f, 1.5f});

        t.addCell(headerCell("Produit"));
        t.addCell(headerCell("Qte"));
        t.addCell(headerCell("Prix unit."));
        t.addCell(headerCell("Total"));

        String pname = p != null && p.getName() != null ? p.getName() : (c.getProductName() != null ? c.getProductName() : "Produit #" + c.getProductId());
        BigDecimal unit = p != null && p.getPrice() != null
                ? p.getPrice()
                : (c.getQuantity() > 0 && c.getTotalPrice() != null
                    ? c.getTotalPrice().divide(BigDecimal.valueOf(c.getQuantity()), 3, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        BigDecimal total = c.getTotalPrice() != null ? c.getTotalPrice() : BigDecimal.ZERO;

        t.addCell(bodyCell(pname, Element.ALIGN_LEFT));
        t.addCell(bodyCell(String.valueOf(c.getQuantity()), Element.ALIGN_CENTER));
        t.addCell(bodyCell(unit.toPlainString() + " DT", Element.ALIGN_RIGHT));
        t.addCell(bodyCell(total.toPlainString() + " DT", Element.ALIGN_RIGHT));

        if (c.getNotes() != null && !c.getNotes().isBlank()) {
            PdfPCell notes = bodyCell("Notes : " + c.getNotes(), Element.ALIGN_LEFT);
            notes.setColspan(4);
            notes.setBackgroundColor(new BaseColor(249, 250, 251));
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

        PdfPCell lbl = new PdfPCell(new Phrase("TOTAL TTC", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, BaseColor.WHITE)));
        lbl.setBackgroundColor(BRAND);
        lbl.setHorizontalAlignment(Element.ALIGN_LEFT);
        lbl.setPadding(8);

        PdfPCell val = new PdfPCell(new Phrase(total.toPlainString() + " DT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, BaseColor.WHITE)));
        val.setBackgroundColor(BRAND);
        val.setHorizontalAlignment(Element.ALIGN_RIGHT);
        val.setPadding(8);

        t.addCell(lbl);
        t.addCell(val);
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
        info.addElement(new Paragraph("Authentification", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BRAND)));
        info.addElement(new Paragraph(
                "Ce QR code est signe cryptographiquement. Scannez-le depuis Medicare pour verifier l'authenticite de la facture.",
                FontFactory.getFont(FontFactory.HELVETICA, 10f, GREY)));

        t.addCell(qrCell);
        t.addCell(info);
        doc.add(t);
    }

    private void addFooter(Document doc) throws Exception {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("Merci de votre confiance - Medicare", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9f, GREY)));
        doc.add(p);
    }

    private PdfPCell headerCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, BaseColor.WHITE)));
        c.setBackgroundColor(BRAND);
        c.setPadding(8);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private PdfPCell bodyCell(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 11f, BaseColor.BLACK)));
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
        c.addElement(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, BRAND)));
        return c;
    }

    private Paragraph textLine(String s, boolean bold) {
        Font f = FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 10f, BaseColor.BLACK);
        return new Paragraph(s, f);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
