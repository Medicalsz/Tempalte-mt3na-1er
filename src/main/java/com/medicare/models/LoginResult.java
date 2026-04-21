package com.medicare.models;

public class LoginResult {
    private final User user;
    private final String accountType;
    private final int medecinId;

    public LoginResult(User user, String accountType, int medecinId) {
        this.user = user;
        this.accountType = accountType;
        this.medecinId = medecinId;
    }

    public User getUser() {
        return user;
    }

    public String getAccountType() {
        return accountType;
    }

    public int getMedecinId() {
        return medecinId;
    }

    public boolean isAdmin() {
        return "admin".equals(accountType);
    }

    public boolean isUser() {
        return "user".equals(accountType);
    }

    public boolean isMedecin() {
        return "medecin".equals(accountType);
    }
}
