package com.medicare.services;

import com.medicare.models.Commande;
import com.medicare.models.Produit;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Wrapper around Stripe Checkout. We create a hosted Checkout Session,
 * open its URL in a JavaFX WebView, and verify payment status when the
 * session redirects to the success URL.
 */
public class StripeService {

    public StripeService() {
        // Initialize SDK API key once.
        if (Stripe.apiKey == null || Stripe.apiKey.isBlank()) {
            Stripe.apiKey = StripeConfig.secretKey();
        }
    }

    public static class CheckoutSession {
        public final String id;
        public final String url;
        CheckoutSession(String id, String url) { this.id = id; this.url = url; }
    }

    /** Build a Checkout Session for a single-product commande. */
    public CheckoutSession createCheckoutSession(Commande commande, Produit produit) throws StripeException {
        String productName = produit != null && produit.getName() != null
                ? produit.getName() : ("Produit #" + commande.getProductId());
        BigDecimal unitPrice = produit != null && produit.getPrice() != null
                ? produit.getPrice()
                : commande.getTotalPrice().divide(BigDecimal.valueOf(commande.getQuantity()), 2, RoundingMode.HALF_UP);

        long unitAmountCents = unitPrice.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        String currency = StripeConfig.currency();
        String successUrl = StripeConfig.successUrl()
                + "?session_id={CHECKOUT_SESSION_ID}&commande_id=" + commande.getId();
        String cancelUrl = StripeConfig.cancelUrl()
                + "?commande_id=" + commande.getId();

        LineItem.PriceData.ProductData productData = LineItem.PriceData.ProductData.builder()
                .setName(productName)
                .setDescription(produit != null && produit.getDescription() != null
                        ? produit.getDescription() : "Commande Medicare")
                .build();

        LineItem.PriceData priceData = LineItem.PriceData.builder()
                .setCurrency(currency)
                .setUnitAmount(unitAmountCents)
                .setProductData(productData)
                .build();

        LineItem item = LineItem.builder()
                .setQuantity((long) commande.getQuantity())
                .setPriceData(priceData)
                .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(commande.getCommandeNumber() != null
                        ? commande.getCommandeNumber() : ("CMD-" + commande.getId()))
                .addLineItem(item)
                .putMetadata("commande_id", String.valueOf(commande.getId()))
                .putMetadata("product_id", String.valueOf(commande.getProductId()))
                .putMetadata("user_id", String.valueOf(commande.getUserId()))
                .build();

        Session session = Session.create(params);
        return new CheckoutSession(session.getId(), session.getUrl());
    }

    /** Result of inspecting a session after the Checkout flow returns. */
    public static class PaymentStatus {
        public final boolean paid;
        public final String paymentIntentId;
        public final String rawStatus;

        PaymentStatus(boolean paid, String paymentIntentId, String rawStatus) {
            this.paid = paid;
            this.paymentIntentId = paymentIntentId;
            this.rawStatus = rawStatus;
        }
    }

    /** Retrieve session and report whether payment succeeded. */
    public PaymentStatus retrieve(String sessionId) throws StripeException {
        Session s = Session.retrieve(sessionId);
        boolean paid = "paid".equalsIgnoreCase(s.getPaymentStatus());
        return new PaymentStatus(paid, s.getPaymentIntent(), s.getPaymentStatus());
    }
}
