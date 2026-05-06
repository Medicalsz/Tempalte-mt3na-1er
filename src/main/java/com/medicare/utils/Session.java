package com.medicare.utils;

import com.medicare.models.User;

/**
 * Simple session holder — stores the currently logged-in user.
 * Used by controllers to access user state across views.
 */
public class Session {

    private static User currentUser;
    private static String accountType; // "user", "medecin", "admin"
    private static int medecinId = -1;

    private Session() {} // prevent instantiation

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static String getAccountType() {
        return accountType;
    }

    public static void setAccountType(String type) {
        accountType = type;
    }

    public static int getMedecinId() {
        return medecinId;
    }

    public static void setMedecinId(int id) {
        medecinId = id;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return "admin".equals(accountType);
    }

    public static boolean isMedecin() {
        return "medecin".equals(accountType);
    }

    public static boolean isUser() {
        return "user".equals(accountType);
    }

    public static void clear() {
        currentUser = null;
        accountType = null;
        medecinId = -1;
    }
}
