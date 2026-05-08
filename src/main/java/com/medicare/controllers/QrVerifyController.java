package com.medicare.controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.medicare.models.Commande;
import com.medicare.services.CommandeService;
import com.medicare.services.QrCodeService;
import com.medicare.services.QrCodeService.VerificationResult;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.format.DateTimeFormatter;

public class QrVerifyController {

    @FXML private TabPane tabs;
    @FXML private StackPane webcamPane;
    @FXML private ImageView webcamView;
    @FXML private Label webcamHint;
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private ImageView uploadedView;
    @FXML private VBox resultBox;
    @FXML private Label resultTitle;
    @FXML private Label resultDetails;

    private final QrCodeService qrService = new QrCodeService();
    private final CommandeService commandeService = new CommandeService();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Webcam webcam;
    private Timeline scannerLoop;
    private volatile boolean scanning = false;

    @FXML
    private void initialize() {
        // when window closes, ensure webcam is released
        webcamView.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                Window w = newS.getWindow();
                if (w != null) {
                    w.setOnHidden(e -> stopWebcam());
                }
            }
        });
    }

    // ==================== WEBCAM ====================

    @FXML
    private void onStartWebcam() {
        if (scanning) return;
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                showResult(false, "Aucune webcam detectee", "Branche une webcam et reessaie.");
                return;
            }
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            if (!webcam.open()) {
                showResult(false, "Impossible d'ouvrir la webcam",
                        "Une autre application l'utilise peut-etre.");
                return;
            }
            webcamHint.setVisible(false);
            scanning = true;
            btnStart.setDisable(true);
            btnStop.setDisable(false);

            scannerLoop = new Timeline(new KeyFrame(Duration.millis(100), e -> tickWebcam()));
            scannerLoop.setCycleCount(Animation.INDEFINITE);
            scannerLoop.play();
        } catch (Exception ex) {
            showResult(false, "Erreur webcam", ex.getMessage());
            stopWebcam();
        }
    }

    @FXML
    private void onStopWebcam() {
        stopWebcam();
    }

    private void tickWebcam() {
        if (!scanning || webcam == null || !webcam.isOpen()) return;
        BufferedImage frame = webcam.getImage();
        if (frame == null) return;

        // display
        Image fxImg = SwingFXUtils.toFXImage(frame, null);
        webcamView.setImage(fxImg);

        // try decode
        String payload = qrService.decode(frame);
        if (payload != null) {
            // pause to avoid double-processing
            scannerLoop.stop();
            scanning = false;
            handlePayload(payload);
            // close webcam after success
            stopWebcam();
        }
    }

    private void stopWebcam() {
        scanning = false;
        if (scannerLoop != null) {
            scannerLoop.stop();
            scannerLoop = null;
        }
        try {
            if (webcam != null && webcam.isOpen()) webcam.close();
        } catch (Exception ignored) {}
        webcam = null;
        Platform.runLater(() -> {
            btnStart.setDisable(false);
            btnStop.setDisable(true);
            webcamHint.setVisible(true);
        });
    }

    // ==================== UPLOAD ====================

    @FXML
    private void onPickImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image de QR");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        Window w = uploadedView.getScene() != null ? uploadedView.getScene().getWindow() : null;
        File f = fc.showOpenDialog(w);
        if (f == null) return;

        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) {
                showResult(false, "Image illisible", "Le fichier choisi n'est pas une image valide.");
                return;
            }
            uploadedView.setImage(SwingFXUtils.toFXImage(img, null));
            String payload = qrService.decode(img);
            if (payload == null) {
                showResult(false, "Aucun QR detecte",
                        "L'image ne contient pas de QR code lisible. Reessaie avec une image plus nette.");
                return;
            }
            handlePayload(payload);
        } catch (Exception ex) {
            showResult(false, "Erreur lecture image", ex.getMessage());
        }
    }

    // ==================== RESULT ====================

    private void handlePayload(String payload) {
        VerificationResult res = qrService.verify(payload);
        if (!res.valid) {
            showResult(false, "QR INVALIDE", res.reason);
            return;
        }

        // cross-check with DB: does this commande still exist and does the data match?
        StringBuilder sb = new StringBuilder();
        sb.append("Signature : OK (HMAC-SHA256)\n");
        sb.append("N° commande : ").append(res.commandeNumber).append("\n");
        sb.append("Patient (ID) : ").append(res.userId).append("\n");
        sb.append("Produit (ID) : ").append(res.productId).append("\n");
        sb.append("Quantite : ").append(res.quantity).append("\n");
        sb.append("Total : ").append(res.total != null ? res.total.toPlainString() + " DT" : "-").append("\n");
        sb.append("Date : ").append(res.date != null ? res.date.format(DF) : "-").append("\n");

        Commande dbMatch = findCommande(res.commandeNumber);
        if (dbMatch == null) {
            sb.append("\nAttention : aucune commande trouvee en base avec ce numero.");
            showResult(false, "QR signe mais commande introuvable", sb.toString());
            return;
        }
        sb.append("\nVerifie en base : statut = ").append(dbMatch.getStatus());
        showResult(true, "FACTURE AUTHENTIQUE", sb.toString());
    }

    private Commande findCommande(String num) {
        if (num == null) return null;
        return commandeService.search(num).stream()
                .filter(c -> num.equals(c.getCommandeNumber()))
                .findFirst().orElse(null);
    }

    private void showResult(boolean ok, String title, String details) {
        Platform.runLater(() -> {
            String border = ok ? "#16a34a" : "#dc2626";
            String bg = ok ? "#dcfce7" : "#fee2e2";
            String fg = ok ? "#15803d" : "#991b1b";
            resultBox.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; "
                    + "-fx-padding: 16; -fx-border-color: " + border + "; -fx-border-radius: 10; -fx-border-width: 1.5;");
            resultTitle.setText((ok ? "✓  " : "✗  ") + title);
            resultTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + fg + ";");
            resultDetails.setText(details);
            resultDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: #1f2937; -fx-line-spacing: 2;");
        });
    }
}
