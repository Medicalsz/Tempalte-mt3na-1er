package com.medicare.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class StripeConfig {

    private static final Properties PROPS = new Properties();
    private static volatile boolean loaded;

    private StripeConfig() {}

    private static void ensureLoaded() {
        if (loaded) return;
        synchronized (StripeConfig.class) {
            if (loaded) return;
            try (InputStream is = StripeConfig.class.getResourceAsStream("/stripe.properties")) {
                if (is != null) PROPS.load(is);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot load stripe.properties: " + e.getMessage(), e);
            }
            loaded = true;
        }
    }

    public static String secretKey() { return require("stripe.secret_key"); }
    public static String publishableKey() { return require("stripe.publishable_key"); }

    public static boolean isConfigured() {
        ensureLoaded();
        return isPresent("stripe.secret_key") && isPresent("stripe.publishable_key");
    }

    public static String currency() {
        ensureLoaded();
        return PROPS.getProperty("stripe.currency", "eur");
    }

    public static String successUrl() {
        ensureLoaded();
        return PROPS.getProperty("stripe.success_url", "https://medicare.local/payment-success");
    }

    public static String cancelUrl() {
        ensureLoaded();
        return PROPS.getProperty("stripe.cancel_url", "https://medicare.local/payment-cancel");
    }

    private static String require(String key) {
        ensureLoaded();
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank() || value.contains("REPLACE_ME")) {
            throw new IllegalStateException("Missing Stripe property: " + key + " in src/main/resources/stripe.properties");
        }
        return value;
    }

    private static boolean isPresent(String key) {
        String value = PROPS.getProperty(key);
        return value != null && !value.isBlank() && !value.contains("REPLACE_ME");
    }
}
