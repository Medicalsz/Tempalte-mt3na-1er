package com.medicare.services;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class QrCodeService {

    // Classe interne pour le résultat de vérification
    public static class VerificationResult {
        private final boolean valid;
        private final String reason;

        public VerificationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
    }

    /**
     * Génère une image de QR Code
     */
    public BufferedImage generateQRCodeImage(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Décode un QR Code depuis une image
     */
    public String decode(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Vérifie la validité d'un QR Code Medicare
     */
    public VerificationResult verify(String qrData) {
        if (qrData == null || !qrData.startsWith("MEDICARE")) {
            return new VerificationResult(false, "Format de QR Code invalide");
        }
        // Logique de vérification simplifiée
        return new VerificationResult(true, "QR Code valide");
    }
}
