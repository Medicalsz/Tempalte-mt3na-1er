package com.medicare.utils;

import java.util.prefs.Preferences;

public final class AuthPreferenceUtil {
    private static final Preferences AUTH_PREFS = Preferences.userRoot().node("com/medicare/auth");
    private static final String BIOMETRIC_KEY_PREFIX = "biometric_enabled_";
    private static final String BIOMETRIC_TOKEN_PREFIX = "biometric_token_";

    private AuthPreferenceUtil() {
    }

    public static boolean isBiometricEnabled(String email) {
        if (email == null || email.isBlank()) return false;
        return AUTH_PREFS.getBoolean(BIOMETRIC_KEY_PREFIX + email.trim().toLowerCase(), false);
    }

    public static void setBiometricEnabled(String email, boolean enabled) {
        if (email == null || email.isBlank()) return;
        AUTH_PREFS.putBoolean(BIOMETRIC_KEY_PREFIX + email.trim().toLowerCase(), enabled);
    }

    public static String getBiometricToken(String email) {
        if (email == null || email.isBlank()) return null;
        return AUTH_PREFS.get(BIOMETRIC_TOKEN_PREFIX + email.trim().toLowerCase(), null);
    }

    public static void setBiometricToken(String email, String token) {
        if (email == null || email.isBlank()) return;
        if (token == null) AUTH_PREFS.remove(BIOMETRIC_TOKEN_PREFIX + email.trim().toLowerCase());
        else AUTH_PREFS.put(BIOMETRIC_TOKEN_PREFIX + email.trim().toLowerCase(), token);
    }
}
