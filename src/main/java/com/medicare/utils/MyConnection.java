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
        // The connection will be established lazily and safely in getCnx()
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
                // Forcer l'utilisation de la base de données de développement locale
                System.out.println("Connexion à la base de données : " + DEFAULT_URL);
                cnx = DriverManager.getConnection(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
                System.out.println("Nouvelle connexion DB établie !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur de connexion DB: " + e.getMessage());
        }

        return cnx;
    }
}