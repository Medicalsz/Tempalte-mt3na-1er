package com.medicare.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private Connection cnx;

    // Default values for local development
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/medicare";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private MyConnection() {
        // The constructor is kept private for the singleton pattern.
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                // Read from environment variables, or use defaults if not found
                String dbUrl = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : DEFAULT_URL;
                String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : DEFAULT_USER;
                String dbPass = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : DEFAULT_PASSWORD;

                cnx = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                System.out.println("Nouvelle connexion DB établie !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur de connexion DB: " + e.getMessage());
        }
        return cnx;
    }
}