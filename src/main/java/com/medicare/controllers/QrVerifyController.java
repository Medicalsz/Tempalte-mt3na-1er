package com.medicare.controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.medicare.models.Commande;
import com.medicare.services.CommandeService;
import com.medicare.services.QRCodeService;
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

    private final QRCodeService qrService = new QRCodeService();
    private final CommandeService commandeService = new CommandeService();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Webcam webcam;
    private Timeline scannerLoop;
    private volatile boolean scanning = false;

    @FXML
    private void initialize() {
        webcamView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Window window = newScene.getWindow();
                if (window != null) {
                    window.setOnHidden(event -> stopWebcam());
                }
            }
        });
    }

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
                showResult(false, "Impossible d'ouvrir la webcam", "Une autre application l'utilise peut-etre.");
                return;
            }
            webcamHint.setVisible(false);
            scanning = true;
            btnStart.setDisable(true);
            btnStop.setDisable(false);

            scannerLoop = new Timeline(new KeyFrame(Duration.millis(100), event -> tickWebcam()));
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

        Image fxImage = SwingFXUtils.toFXImage(frame, null);
        webcamView.setImage(fxImage);

        String payload = qrService.decode(frame);
        if (payload != null) {
            scannerLoop.stop();
            scanning = false;
            handlePayload(payload);
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
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        } catch (Exception ignored) {
        }
        webcam = null;
        Platform.runLater(() -> {
            btnStart.setDisable(false);
            btnStop.setDisable(true);
            webcamHint.setVisible(true);
        });
    }

    @FXML
    private void onPickImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image de QR");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        Window window = uploadedView.getScene() != null ? uploadedView.getScene().getWindow() : null;
        File file = chooser.showOpenDialog(window);
        if (file == null) return;

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                showResult(false, "Image illisible", "Le fichier choisi n'est pas une image valide.");
                return;
            }
            uploadedView.setImage(SwingFXUtils.toFXImage(image, null));
            String payload = qrService.decode(image);
            if (payload == null) {
                showResult(false, "Aucun QR detecte", "L'image ne contient pas de QR code lisible.");
                return;
            }
            handlePayload(payload);
        } catch (Exception ex) {
            showResult(false, "Erreur lecture image", ex.getMessage());
        }
    }

    private void handlePayload(String payload) {
        QRCodeService.VerificationResult result = qrService.verify(payload);
        if (!result.valid) {
            showResult(false, "QR invalide", result.reason);
            return;
        }

        StringBuilder details = new StringBuilder();
        details.append("Signature : OK\n");
        details.append("No commande : ").append(result.commandeNumber).append("\n");
        details.append("Patient (ID) : ").append(result.userId).append("\n");
        details.append("Produit (ID) : ").append(result.productId).append("\n");
        details.append("Quantite : ").append(result.quantity).append("\n");
        details.append("Total : ").append(result.total != null ? result.total.toPlainString() + " DT" : "-").append("\n");
        details.append("Date : ").append(result.date != null ? result.date.format(DF) : "-").append("\n");

        Commande dbMatch = findCommande(result.commandeNumber);
        if (dbMatch == null) {
            details.append("\nAttention : aucune commande trouvee en base avec ce numero.");
            showResult(false, "QR signe mais commande introuvable", details.toString());
            return;
        }

        details.append("\nVerifie en base : statut = ").append(dbMatch.getStatus());
        showResult(true, "Facture authentique", details.toString());
    }

    private Commande findCommande(String commandeNumber) {
        if (commandeNumber == null) return null;
        return commandeService.search(commandeNumber).stream()
                .filter(c -> commandeNumber.equals(c.getCommandeNumber()))
                .findFirst()
                .orElse(null);
    }

    private void showResult(boolean ok, String title, String details) {
        Platform.runLater(() -> {
            String border = ok ? "#16a34a" : "#dc2626";
            String bg = ok ? "#dcfce7" : "#fee2e2";
            String fg = ok ? "#15803d" : "#991b1b";
            resultBox.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; -fx-padding: 16; -fx-border-color: " + border + "; -fx-border-radius: 10; -fx-border-width: 1.5;");
            resultTitle.setText((ok ? "OK - " : "X - ") + title);
            resultTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + fg + ";");
            resultDetails.setText(details);
            resultDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: #1f2937; -fx-line-spacing: 2;");
        });
    }
}
