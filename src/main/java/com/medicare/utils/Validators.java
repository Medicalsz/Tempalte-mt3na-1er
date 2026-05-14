package com.medicare.utils;

import javafx.scene.control.TextInputControl;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Validators {

    private Validators() {}

    public static final String ERROR_STYLE =
            "-fx-background-radius: 8; -fx-font-size: 13px; -fx-border-color: #dc2626; -fx-border-radius: 8; -fx-border-width: 1.3;";

    public static final String NORMAL_STYLE =
            "-fx-background-radius: 8; -fx-font-size: 13px;";

    public static void markInvalid(TextInputControl field) {
        field.setStyle(ERROR_STYLE);
    }

    public static void markValid(TextInputControl field) {
        field.setStyle(NORMAL_STYLE);
    }

    public static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static boolean isSafeName(String s) {
        return s != null && s.trim().matches("[A-Za-zÀ-ÿ0-9 .,'\\-]{2,150}");
    }

    public static boolean isSku(String s) {
        return s == null || s.trim().isEmpty() || s.trim().matches("[A-Za-z0-9_\\-]{2,80}");
    }

    public static boolean isPositiveDecimal(String s) {
        if (!notBlank(s)) return false;
        try {
            BigDecimal v = new BigDecimal(s.trim());
            return v.compareTo(BigDecimal.ZERO) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isStrictlyPositiveInt(String s) {
        if (!notBlank(s)) return false;
        try {
            return Integer.parseInt(s.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNonNegativeInt(String s) {
        if (!notBlank(s)) return false;
        try {
            return Integer.parseInt(s.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isStrictlyFuture(LocalDate d) {
        return d != null && d.isAfter(LocalDate.now());
    }

    public static BigDecimal parseDecimal(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
