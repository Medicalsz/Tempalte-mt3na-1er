package com.medicare.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.medicare.models.Commande;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Builds, signs and verifies QR codes used on the invoice PDF.
 * The QR encodes a payload signed with HMAC-SHA256, so it cannot be forged
 * without the secret key.
 *
 * Payload format (pipe-separated):
 *   MEDICARE|v1|<commandeNumber>|<userId>|<productId>|<qty>|<total>|<dateIso>|<sigBase64Url>
 */
public class QrCodeService {

    // TODO PI: in production, load from env var or external config, not hard-coded.
    private static final String SECRET_KEY = "MEDICARE-PI-2026-SECRET-DO-NOT-LEAK";
    private static final String PREFIX = "MEDICARE";
    private static final String VERSION = "v1";
    private static final String SEP = "|";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static class VerificationResult {
        public final boolean valid;
        public final String reason;
        public final String commandeNumber;
        public final int userId;
        public final int productId;
        public final int quantity;
        public final BigDecimal total;
        public final LocalDateTime date;

        private VerificationResult(boolean valid, String reason, String commandeNumber,
                                   int userId, int productId, int quantity,
                                   BigDecimal total, LocalDateTime date) {
            this.valid = valid;
            this.reason = reason;
            this.commandeNumber = commandeNumber;
            this.userId = userId;
            this.productId = productId;
            this.quantity = quantity;
            this.total = total;
            this.date = date;
        }

        static VerificationResult invalid(String reason) {
            return new VerificationResult(false, reason, null, 0, 0, 0, null, null);
        }

        static VerificationResult valid(String commandeNumber, int userId, int productId,
                                        int quantity, BigDecimal total, LocalDateTime date) {
            return new VerificationResult(true, "Signature authentique", commandeNumber,
                    userId, productId, quantity, total, date);
        }
    }

    /** Builds the signed payload string that will be encoded into the QR. */
    public String buildSignedPayload(Commande c) {
        String body = PREFIX + SEP + VERSION + SEP
                + safe(c.getCommandeNumber()) + SEP
                + c.getUserId() + SEP
                + c.getProductId() + SEP
                + c.getQuantity() + SEP
                + (c.getTotalPrice() != null ? c.getTotalPrice().toPlainString() : "0") + SEP
                + (c.getCommandeDate() != null ? c.getCommandeDate().format(ISO) : LocalDateTime.now().format(ISO));
        String sig = sign(body);
        return body + SEP + sig;
    }

    /** Returns PNG bytes of a QR encoding the given text. */
    public byte[] generatePng(String text, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("QR generation failed: " + e.getMessage(), e);
        }
    }

    /** Tries to decode a QR from a BufferedImage. Returns null if no QR found. */
    public String decode(BufferedImage img) {
        if (img == null) return null;
        try {
            BufferedImageLuminanceSource src = new BufferedImageLuminanceSource(img);
            BinaryBitmap bmp = new BinaryBitmap(new HybridBinarizer(src));
            Result r = new MultiFormatReader().decode(bmp);
            return r != null ? r.getText() : null;
        } catch (NotFoundException nf) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Verifies an arbitrary payload string. */
    public VerificationResult verify(String payload) {
        if (payload == null || payload.isBlank()) {
            return VerificationResult.invalid("QR vide ou illisible");
        }
        String[] parts = payload.split("\\" + SEP, -1);
        // Expected: MEDICARE|v1|num|userId|productId|qty|total|date|sig  → 9 parts
        if (parts.length != 9) {
            return VerificationResult.invalid("Format inconnu (champs: " + parts.length + ")");
        }
        if (!PREFIX.equals(parts[0])) {
            return VerificationResult.invalid("Prefixe invalide");
        }
        if (!VERSION.equals(parts[1])) {
            return VerificationResult.invalid("Version non supportee: " + parts[1]);
        }

        String body = String.join(SEP, parts[0], parts[1], parts[2], parts[3],
                parts[4], parts[5], parts[6], parts[7]);
        String givenSig = parts[8];
        String expectedSig = sign(body);
        if (!constantTimeEquals(givenSig, expectedSig)) {
            return VerificationResult.invalid("Signature invalide (QR falsifie)");
        }

        try {
            String num = parts[2];
            int userId = Integer.parseInt(parts[3]);
            int productId = Integer.parseInt(parts[4]);
            int qty = Integer.parseInt(parts[5]);
            BigDecimal total = new BigDecimal(parts[6]);
            LocalDateTime date = LocalDateTime.parse(parts[7], ISO);
            return VerificationResult.valid(num, userId, productId, qty, total, date);
        } catch (Exception e) {
            return VerificationResult.invalid("Donnees corrompues: " + e.getMessage());
        }
    }

    // ----- internals -----

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException("HMAC failed: " + e.getMessage(), e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int diff = 0;
        for (int i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
        return diff == 0;
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace(SEP, "_");
    }
}
