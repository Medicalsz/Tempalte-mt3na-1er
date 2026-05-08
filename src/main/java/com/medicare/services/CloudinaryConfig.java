package com.medicare.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads Cloudinary credentials from src/main/resources/cloudinary.properties.
 * The properties file is gitignored — never commit secrets.
 */
public final class CloudinaryConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = CloudinaryConfig.class.getResourceAsStream("/cloudinary.properties")) {
            if (is == null) {
                throw new IllegalStateException("cloudinary.properties not found in resources");
            }
            PROPS.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load cloudinary.properties: " + e.getMessage(), e);
        }
    }

    private CloudinaryConfig() {}

    public static String cloudName()    { return require("cloudinary.cloud_name"); }
    public static String apiKey()       { return require("cloudinary.api_key"); }
    public static String apiSecret()    { return require("cloudinary.api_secret"); }
    public static String uploadFolder() { return PROPS.getProperty("cloudinary.upload_folder", "medicare/products"); }

    private static String require(String key) {
        String v = PROPS.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing Cloudinary property: " + key);
        }
        return v;
    }
}
