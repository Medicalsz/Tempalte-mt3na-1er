package com.medicare.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.medicare.models.Ordonnance;

import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class OrdonnancePDF {

    public static String generate(Ordonnance ord, String outputPath) {
        try {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 40);
            PdfWriter.getInstance(doc, new FileOutputStream(outputPath));
            doc.open();

            // Couleurs
            BaseColor bleu = new BaseColor(26, 115, 232);
            BaseColor gris = new BaseColor(100, 116, 139);
            BaseColor noir = new BaseColor(30, 41, 59);

            // Fonts
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, bleu);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, noir);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, gris);
            Font contentFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, noir);
            Font smallFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, gris);

            // En-tête
            Paragraph titre = new Paragraph("ORDONNANCE MEDICALE", titleFont);
            titre.setAlignment(Element.ALIGN_CENTER);
            titre.setSpacingAfter(5);
            doc.add(titre);

            // Ligne séparatrice
            LineSeparator line = new LineSeparator();
            line.setLineColor(bleu);
            line.setLineWidth(2);
            doc.add(new Chunk(line));
            doc.add(Chunk.NEWLINE);

            // Info médecin (gauche)
            PdfPTable infoTable = new PdfPTable(new float[]{3f, 2f, 1.2f});
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10);
            infoTable.setSpacingAfter(15);

            // Colonne médecin
            PdfPCell medecinCell = new PdfPCell();
            medecinCell.setBorder(Rectangle.NO_BORDER);
            medecinCell.addElement(new Paragraph("Dr. " + ord.getMedecinFullName(), headerFont));
            medecinCell.addElement(new Paragraph(ord.getSpecialite(), normalFont));
            if (ord.getCabinet() != null && !ord.getCabinet().isEmpty()) {
                medecinCell.addElement(new Paragraph("Cabinet: " + ord.getCabinet(), normalFont));
            }
            infoTable.addCell(medecinCell);

            // Colonne date
            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph datePar = new Paragraph("Date: " + ord.getDateRdv(), headerFont);
            datePar.setAlignment(Element.ALIGN_RIGHT);
            dateCell.addElement(datePar);
            Paragraph creePar = new Paragraph("Cree le: " +
                    ord.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), smallFont);
            creePar.setAlignment(Element.ALIGN_RIGHT);
            dateCell.addElement(creePar);
            infoTable.addCell(dateCell);

            // Colonne QR Code (vérification d'authenticité)
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try {
                String hash = com.medicare.utils.QRCodeGenerator.buildOrdonnanceHash(
                        ord.getId(), ord.getRendezVousId(),
                        ord.getPatientFullName(), ord.getMedecinFullName());
                String qrContent = "MEDICARE-ORDONNANCE\n" +
                        "ID: " + ord.getId() + "\n" +
                        "RDV: " + ord.getRendezVousId() + "\n" +
                        "Patient: " + ord.getPatientFullName() + "\n" +
                        "Medecin: Dr. " + ord.getMedecinFullName() + "\n" +
                        "Date: " + ord.getDateRdv() + "\n" +
                        "Code: " + hash;
                byte[] qrBytes = com.medicare.utils.QRCodeGenerator.generatePNG(qrContent, 220);
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.scaleAbsolute(75, 75);
                qrImage.setAlignment(Image.ALIGN_RIGHT);
                qrCell.addElement(qrImage);
                Paragraph codeLbl = new Paragraph("Code: " + hash, smallFont);
                codeLbl.setAlignment(Element.ALIGN_RIGHT);
                qrCell.addElement(codeLbl);
            } catch (Exception qrEx) {
                System.out.println("Erreur QR: " + qrEx.getMessage());
            }
            infoTable.addCell(qrCell);

            doc.add(infoTable);

            // Patient
            PdfPTable patientTable = new PdfPTable(1);
            patientTable.setWidthPercentage(100);
            patientTable.setSpacingAfter(20);

            PdfPCell patientCell = new PdfPCell();
            patientCell.setBackgroundColor(new BaseColor(240, 244, 255));
            patientCell.setBorderColor(bleu);
            patientCell.setBorderWidth(1);
            patientCell.setPadding(12);
            patientCell.addElement(new Paragraph("Patient: " + ord.getPatientFullName(), headerFont));
            patientTable.addCell(patientCell);

            doc.add(patientTable);

            // Contenu ordonnance
            Paragraph contenuTitle = new Paragraph("Prescription:", headerFont);
            contenuTitle.setSpacingAfter(10);
            doc.add(contenuTitle);

            LineSeparator thinLine = new LineSeparator();
            thinLine.setLineColor(new BaseColor(226, 232, 240));
            thinLine.setLineWidth(0.5f);
            doc.add(new Chunk(thinLine));
            doc.add(Chunk.NEWLINE);

            Paragraph contenu = new Paragraph(ord.getContenu(), contentFont);
            contenu.setLeading(20);
            doc.add(contenu);

            // Signature
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);

            Paragraph signature = new Paragraph("Signature du medecin", normalFont);
            signature.setAlignment(Element.ALIGN_RIGHT);
            doc.add(signature);

            Paragraph sigName = new Paragraph("Dr. " + ord.getMedecinFullName(), headerFont);
            sigName.setAlignment(Element.ALIGN_RIGHT);
            doc.add(sigName);

            // Mention QR code (pied de page)
            doc.add(Chunk.NEWLINE);
            LineSeparator footerLine = new LineSeparator();
            footerLine.setLineColor(new BaseColor(226, 232, 240));
            footerLine.setLineWidth(0.5f);
            doc.add(new Chunk(footerLine));
            Paragraph qrNote = new Paragraph(
                    "Ce document est authentifie par un QR code. Scannez-le pour verifier l'origine de l'ordonnance.",
                    smallFont);
            qrNote.setAlignment(Element.ALIGN_CENTER);
            qrNote.setSpacingBefore(6);
            doc.add(qrNote);

            doc.close();
            return outputPath;
        } catch (Exception e) {
            System.out.println("Erreur PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

