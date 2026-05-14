package com.medicare.controllers;

import com.medicare.models.Commande;
import com.medicare.models.Produit;
import com.medicare.services.CommandeService;
import com.medicare.services.StripeConfig;
import com.medicare.services.StripeService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class StripeCheckoutController {

    @FXML private WebView webView;
    @FXML private VBox loaderPane;
    @FXML private Label amountLabel;
    @FXML private Label hintLbl;

    private final CommandeService commandeService = new CommandeService();
    private Commande commande;
    private Stage owner;
    private Consumer<Boolean> onClosed;

    public void start(Stage stage, Commande commande, Produit produit, Consumer<Boolean> onClosed) {
        this.owner = stage;
        this.commande = commande;
        this.onClosed = onClosed;
        amountLabel.setText("· " + commande.getTotalPrice() + " " + StripeConfig.currency().toUpperCase());

        webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> handleLocation(newUrl));
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, o, s) -> {
            if (s == Worker.State.SUCCEEDED) loaderPane.setVisible(false);
        });

        new Thread(() -> {
            try {
                StripeService stripeService = new StripeService();
                StripeService.CheckoutSession session = stripeService.createCheckoutSession(commande, produit);
                Platform.runLater(() -> webView.getEngine().load(session.url));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loaderPane.setVisible(false);
                    hintLbl.setText("Erreur Stripe : " + e.getMessage());
                    hintLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
                });
            }
        }, "stripe-checkout-create").start();
    }

    private void handleLocation(String url) {
        if (url == null) return;
        if (url.startsWith(StripeConfig.successUrl())) {
            finalizePayment(parseQuery(url).get("session_id"));
        } else if (url.startsWith(StripeConfig.cancelUrl())) {
            close(false);
        }
    }

    private void finalizePayment(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            close(false);
            return;
        }
        loaderPane.setVisible(true);
        new Thread(() -> {
            try {
                StripeService stripeService = new StripeService();
                StripeService.PaymentStatus status = stripeService.retrieve(sessionId);
                if (status.paid) {
                    commande.setStripePaymentIntentId(status.paymentIntentId);
                    commande.setStatus("confirmee");
                    commandeService.update(commande);
                    Platform.runLater(() -> close(true));
                } else {
                    Platform.runLater(() -> close(false));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> close(false));
            }
        }, "stripe-checkout-verify").start();
    }

    @FXML
    private void onCancel() {
        close(false);
    }

    private void close(boolean paid) {
        if (onClosed != null) onClosed.accept(paid);
        if (owner != null) owner.close();
    }

    private Map<String, String> parseQuery(String url) {
        Map<String, String> map = new HashMap<>();
        int idx = url.indexOf('?');
        if (idx < 0) return map;
        String q = url.substring(idx + 1);
        for (String part : q.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                map.put(part.substring(0, eq), URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }
}
