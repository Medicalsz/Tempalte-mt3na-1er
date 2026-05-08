package com.medicare.controllers;

import com.medicare.models.Commande;
import com.medicare.models.Produit;
import com.medicare.services.CommandeService;
import com.medicare.services.StripeConfig;
import com.medicare.services.StripeService;
import com.medicare.services.StripeService.CheckoutSession;
import com.medicare.services.StripeService.PaymentStatus;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Hosts a JavaFX WebView pointing at Stripe Checkout.
 * Listens for navigations to the configured success/cancel URLs to
 * finalize the commande payment (mark paid + persist payment intent).
 */
public class StripeCheckoutController {

    @FXML private WebView webView;
    @FXML private VBox loaderPane;
    @FXML private Label amountLabel;
    @FXML private Label hintLbl;

    private final StripeService stripeService = new StripeService();
    private final CommandeService commandeService = new CommandeService();

    private Commande commande;
    private Stage owner;
    private Consumer<Boolean> onClosed;   // callback(true=paid, false=cancelled/failed)

    public void start(Stage stage, Commande commande, Produit produit, Consumer<Boolean> onClosed) {
        this.owner = stage;
        this.commande = commande;
        this.onClosed = onClosed;
        amountLabel.setText("· " + commande.getTotalPrice() + " " + StripeConfig.currency().toUpperCase());

        // Listen for redirections to our success/cancel URLs
        webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> handleLocation(newUrl));
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, o, s) -> {
            if (s == Worker.State.SUCCEEDED) loaderPane.setVisible(false);
        });

        // Create the Stripe Checkout Session in a background thread
        new Thread(() -> {
            try {
                CheckoutSession session = stripeService.createCheckoutSession(commande, produit);
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
            Map<String, String> q = parseQuery(url);
            String sessionId = q.get("session_id");
            finalizePayment(sessionId);
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
                PaymentStatus status = stripeService.retrieve(sessionId);
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
                map.put(part.substring(0, eq), java.net.URLDecoder.decode(part.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return map;
    }
}
