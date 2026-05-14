package com.medicare.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class CloudinaryConfig {

    private static final Properties PROPS = new Properties();
    private static volatile boolean loaded;

    private CloudinaryConfig() {}

    private static void ensureLoaded() {
        if (loaded) return;
        synchronized (CloudinaryConfig.class) {
            if (loaded) return;
            try (InputStream is = CloudinaryConfig.class.getResourceAsStream("/cloudinary.properties")) {
                if (is != null) PROPS.load(is);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot load cloudinary.properties: " + e.getMessage(), e);
            }
            loaded = true;
        }
    }

    public static String cloudName() { return require("cloudinary.cloud_name"); }
    public static String apiKey() { return require("cloudinary.api_key"); }
    public static String apiSecret() { return require("cloudinary.api_secret"); }

    public static String uploadFolder() {
        ensureLoaded();
        String folder = PROPS.getProperty("cloudinary.upload_folder");
        return folder == null || folder.isBlank() ? "medicare/products" : folder;
    }

    private static String require(String key) {
        ensureLoaded();
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing Cloudinary property: " + key);
        }
        return value;
    }
}
