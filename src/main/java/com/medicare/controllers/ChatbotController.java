package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.Commande;
import com.medicare.models.Produit;
import com.medicare.models.User;
import com.medicare.services.CommandeService;
import com.medicare.services.MedicalChatbotService;
import com.medicare.services.MedicalChatbotService.Prediction;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;

public class ChatbotController {

    @FXML private VBox messagesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;
    @FXML private Button sendBtn;
    @FXML private Label modelInfoLbl;

    private final MedicalChatbotService chatbot = new MedicalChatbotService();
    private final CommandeService commandeService = new CommandeService();
    private static final DecimalFormat PCT = new DecimalFormat("#0.0%");

    @FXML
    private void initialize() {
        if (chatbot.isReady()) {
            modelInfoLbl.setText(chatbot.getClassCount() + " intentions  -  accuracy "
                    + PCT.format(chatbot.getAccuracy()));
        } else {
            modelInfoLbl.setText("Modele indisponible");
        }
        // Greeting from the bot
        addBotMessage("Bonjour ! Je suis l'assistant sante Medicare. Decris tes symptomes ou pose-moi une question, " +
                      "je peux te conseiller des produits du catalogue.", null);
        Platform.runLater(() -> inputField.requestFocus());
    }

    @FXML
    private void onSend() {
        String text = inputField.getText();
        if (text == null || text.isBlank()) return;
        addUserMessage(text.trim());
        inputField.clear();
        sendBtn.setDisable(true);

        // Show "thinking" indicator
        HBox typing = buildTypingIndicator();
        messagesBox.getChildren().add(typing);
        scrollToBottom();

        // Predict in a short pause for natural feel
        PauseTransition pause = new PauseTransition(Duration.millis(450));
        pause.setOnFinished(e -> {
            messagesBox.getChildren().remove(typing);
            try {
                Prediction p = chatbot.predict(text);
                addBotMessage(p.response, p);
            } catch (Exception ex) {
                addBotMessage("Erreur interne : " + ex.getMessage(), null);
            } finally {
                sendBtn.setDisable(false);
                Platform.runLater(() -> inputField.requestFocus());
            }
        });
        pause.play();
    }

    // ============== UI helpers ==============

    private void addUserMessage(String text) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(450);
        bubble.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; "
                + "-fx-padding: 10 14; -fx-background-radius: 16 16 4 16; -fx-font-size: 13px;");
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text, Prediction p) {
        VBox content = new VBox(8);
        content.setMaxWidth(520);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        String urgency = p != null ? p.urgency : "low";
        String bgColor = "#ffffff";
        String borderColor = "#e5e7eb";
        if ("critical".equalsIgnoreCase(urgency)) {
            bgColor = "#fee2e2";
            borderColor = "#dc2626";
        } else if ("high".equalsIgnoreCase(urgency)) {
            bgColor = "#fef3c7";
            borderColor = "#f59e0b";
        }
        bubble.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: #1f2937; "
                + "-fx-padding: 11 14; -fx-background-radius: 16 16 16 4; -fx-font-size: 13px; "
                + "-fx-border-color: " + borderColor + "; -fx-border-radius: 16 16 16 4; -fx-border-width: 1;");
        content.getChildren().add(bubble);

        // Confidence + intent label (small, discreet)
        if (p != null && p.confidence > 0) {
            Label tag = new Label(p.intent.replace('_', ' ') + "  -  " + PCT.format(p.confidence));
            tag.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af; -fx-padding: 0 4;");
            content.getChildren().add(tag);
        }

        // Critical warning banner
        if (p != null && "critical".equalsIgnoreCase(urgency)) {
            HBox warn = new HBox(8);
            warn.setAlignment(Pos.CENTER_LEFT);
            warn.setPadding(new Insets(8, 12, 8, 12));
            warn.setStyle("-fx-background-color: #dc2626; -fx-background-radius: 8;");
            FontIcon ic = new FontIcon(FontAwesomeSolid.PHONE);
            ic.setIconSize(15);
            ic.setIconColor(Color.WHITE);
            Label l = new Label("URGENCE - Appelle le 190 (SAMU) immediatement");
            l.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
            warn.getChildren().addAll(ic, l);
            content.getChildren().add(warn);
        }

        // Recommended products
        if (p != null && p.recommendedProducts != null && !p.recommendedProducts.isEmpty()) {
            Label header = new Label("Produits recommandes :");
            header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6b7280; -fx-padding: 4 0 0 4;");
            content.getChildren().add(header);
            for (Produit prod : p.recommendedProducts) {
                content.getChildren().add(buildProductCard(prod));
            }
        }

        HBox row = new HBox(content);
        row.setAlignment(Pos.CENTER_LEFT);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private HBox buildProductCard(Produit p) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 10, 8, 10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #ddd6fe; -fx-border-radius: 10; -fx-border-width: 1;");
        card.setMaxWidth(Double.MAX_VALUE);

        // Image
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(48, 48);
        imgBox.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 6;");
        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(48); iv.setFitHeight(48); iv.setPreserveRatio(true);
            try { iv.setImage(new Image(p.getImageUrl(), 48, 48, true, true, true)); } catch (Exception ignored) {}
            imgBox.getChildren().add(iv);
        } else {
            FontIcon ic = new FontIcon(FontAwesomeSolid.CAPSULES);
            ic.setIconSize(22);
            ic.setIconColor(Color.web("#7c3aed"));
            imgBox.getChildren().add(ic);
        }

        VBox info = new VBox(2);
        Label name = new Label(p.getName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        Label sub = new Label((p.getType() != null ? p.getType() : "")
                + (p.getDosage() != null && !p.getDosage().isBlank() ? "  -  " + p.getDosage() : ""));
        sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");
        Label price = new Label((p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO) + " DT");
        price.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        info.getChildren().addAll(name, sub, price);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button order = new Button("Commander");
        FontIcon ic = new FontIcon(FontAwesomeSolid.SHOPPING_CART);
        ic.setIconSize(11);
        ic.setIconColor(Color.WHITE);
        order.setGraphic(ic);
        order.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 11px; "
                + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12;");
        order.setOnAction(e -> quickOrder(p));

        card.getChildren().addAll(imgBox, info, sp, order);
        return card;
    }

    private HBox buildTypingIndicator() {
        Label l = new Label("...");
        l.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 16px; -fx-background-color: #f3f4f6; "
                + "-fx-padding: 8 16; -fx-background-radius: 16;");
        HBox row = new HBox(l);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
            // run twice to ensure layout is computed
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        });
    }

    // ============== Quick order from chat ==============

    private void quickOrder(Produit product) {
        User cu = DashboardPatientController.getCurrentUser();
        if (cu == null) {
            addBotMessage("Tu dois etre connecte pour commander.", null);
            return;
        }
        if (product.getQuantity() <= 0) {
            addBotMessage("Desole, " + product.getName() + " est en rupture de stock.", null);
            return;
        }
        // Create a one-quantity order in en_attente_paiement and immediately open Stripe
        Commande c = new Commande();
        c.setProductId(product.getId());
        c.setUserId(cu.getId());
        c.setQuantity(1);
        c.setTotalPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
        c.setStatus("en_attente_paiement");
        c.setNotes("Commande depuis l'assistant sante");
        c.setCommandeDate(LocalDateTime.now());
        c.setCreatedAt(LocalDateTime.now());

        commandeService.add(c);
        openStripe(c, product);
    }

    private void openStripe(Commande c, Produit product) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("stripe-checkout-view.fxml"));
            Parent root = loader.load();
            StripeCheckoutController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(messagesBox.getScene().getWindow());
            stage.setTitle("Paiement Medicare");
            stage.setScene(new Scene(root, 820, 760));
            ctrl.start(stage, c, product, paid -> {
                if (paid) {
                    addBotMessage("Paiement reussi ! Ta commande de " + product.getName() + " est confirmee.", null);
                } else {
                    addBotMessage("Paiement non finalise. Tu peux retenter depuis 'Mes commandes'.", null);
                }
            });
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            addBotMessage("Erreur ouverture paiement : " + ex.getMessage(), null);
        }
    }
}
