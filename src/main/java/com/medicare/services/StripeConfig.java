package com.medicare.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class StripeConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = StripeConfig.class.getResourceAsStream("/stripe.properties")) {
            if (is == null) {
                throw new IllegalStateException("stripe.properties not found in resources");
            }
            PROPS.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load stripe.properties: " + e.getMessage(), e);
        }
    }

    private StripeConfig() {}

    public static String secretKey()      { return require("stripe.secret_key"); }
    public static String publishableKey() { return require("stripe.publishable_key"); }
    public static String currency()       { return PROPS.getProperty("stripe.currency", "eur"); }
    public static String successUrl()     { return PROPS.getProperty("stripe.success_url", "https://medicare.local/payment-success"); }
    public static String cancelUrl()      { return PROPS.getProperty("stripe.cancel_url", "https://medicare.local/payment-cancel"); }

    private static String require(String key) {
        String v = PROPS.getProperty(key);
        if (v == null || v.isBlank() || v.contains("REPLACE_ME")) {
            throw new IllegalStateException("Missing/placeholder Stripe property: " + key
                    + " — fill src/main/resources/stripe.properties with your test keys.");
        }
        return v;
    }
}
