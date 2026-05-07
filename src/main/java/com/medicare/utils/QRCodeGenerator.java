package com.medicare.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire de génération de QR codes (ZXing).
 */
public class QRCodeGenerator {

    /**
     * Génère un QR code au format PNG (byte[]).
     * @param content contenu texte à encoder
     * @param size    taille en pixels (carré)
     */
    public static byte[] generatePNG(String content, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Génère une empreinte courte (hash) pour authentification de l'ordonnance.
     */
    public static String buildOrdonnanceHash(int ordId, int rdvId, String patient, String medecin) {
        int h = (ordId * 31 + rdvId) * 31 + (patient + medecin).hashCode();
        return Integer.toHexString(h & 0x7FFFFFFF).toUpperCase();
    }
}

