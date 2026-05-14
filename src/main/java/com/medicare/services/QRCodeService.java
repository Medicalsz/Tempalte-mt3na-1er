package com.medicare.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.medicare.models.Commande;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class QRCodeService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String SECRET = System.getenv().getOrDefault("MEDICARE_QR_SECRET", "medicare-dev-qr-secret");

    public static class VerificationResult {
        public final boolean valid;
        public final String reason;
        public final String commandeNumber;
        public final int userId;
        public final int productId;
        public final int quantity;
        public final BigDecimal total;
        public final LocalDateTime date;

        public VerificationResult(boolean valid, String reason, String commandeNumber, int userId, int productId,
                                  int quantity, BigDecimal total, LocalDateTime date) {
            this.valid = valid;
            this.reason = reason;
            this.commandeNumber = commandeNumber;
            this.userId = userId;
            this.productId = productId;
            this.quantity = quantity;
            this.total = total;
            this.date = date;
        }
    }

    public String buildSignedPayload(Commande c) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("commande", c.getCommandeNumber() != null ? c.getCommandeNumber() : ("CMD-" + c.getId()));
        values.put("user", String.valueOf(c.getUserId()));
        values.put("product", String.valueOf(c.getProductId()));
        values.put("qty", String.valueOf(c.getQuantity()));
        values.put("total", c.getTotalPrice() != null ? c.getTotalPrice().toPlainString() : "0");
        values.put("date", c.getCommandeDate() != null ? c.getCommandeDate().format(DF) : LocalDateTime.now().format(DF));
        String data = toQuery(values);
        String sig = sign(data);
        return "MEDICARE|" + data + "|sig=" + sig;
    }

    public byte[] generatePng(String content, int size) throws Exception {
        BufferedImage image = generateQRCodeImage(content, size, size);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    public BufferedImage generateQRCodeImage(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    public String decode(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    public VerificationResult verify(String payload) {
        String normalized = normalizePayload(payload);
        if (normalized == null || !normalized.startsWith("MEDICARE|")) {
            return invalid("Format de QR Code invalide");
        }
        String body = normalized.substring("MEDICARE|".length());
        int sigIndex = body.lastIndexOf("|sig=");
        if (sigIndex < 0) return invalid("Signature absente");

        String data = body.substring(0, sigIndex);
        String actualSig = body.substring(sigIndex + 5);
        String expectedSig = sign(data);
        if (!expectedSig.equals(actualSig)) {
            return invalid("Signature QR invalide");
        }

        Map<String, String> values = parseQuery(data);
        try {
            return new VerificationResult(
                    true,
                    "OK",
                    values.get("commande"),
                    Integer.parseInt(values.getOrDefault("user", "0")),
                    Integer.parseInt(values.getOrDefault("product", "0")),
                    Integer.parseInt(values.getOrDefault("qty", "0")),
                    new BigDecimal(values.getOrDefault("total", "0")),
                    LocalDateTime.parse(values.get("date"), DF)
            );
        } catch (Exception e) {
            return invalid("Contenu QR invalide: " + e.getMessage());
        }
    }

    private VerificationResult invalid(String reason) {
        return new VerificationResult(false, reason, null, 0, 0, 0, null, null);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("QR signing failed: " + e.getMessage(), e);
        }
    }

    private String toQuery(Map<String, String> values) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) sb.append('&');
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                values.put(part.substring(0, eq), URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return values;
    }

    private String normalizePayload(String payload) {
        if (payload == null) return null;
        String normalized = payload.trim().replace("\r", "").replace("\n", "");
        int markerIndex = normalized.indexOf("MEDICARE|");
        if (markerIndex >= 0) {
            normalized = normalized.substring(markerIndex);
        }
        return normalized;
    }
}
