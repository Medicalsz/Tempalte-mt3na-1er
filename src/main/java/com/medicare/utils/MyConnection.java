package com.medicare.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private Connection cnx;

<<<<<<< HEAD
    // Default values for local development
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/medicare";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private MyConnection() {
        // The constructor is kept private for the singleton pattern.
=======
    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/medicare",
                    "root",
                    "2003"
            );
            System.out.println("Connexion OK !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() {
<<<<<<< HEAD
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
=======
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
        return cnx;
    }
}