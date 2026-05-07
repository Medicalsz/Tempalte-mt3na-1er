package com.medicare.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private Connection cnx;

    private static final String URL = "jdbc:mysql://localhost:3306/medicare";
    private static final String USER = "root";
    private static final String PASS = "";

    private MyConnection() {
        connect();
    }

    private void connect() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Connexion OK !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
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
                System.out.println("Reconnexion a la base de donnees...");
                connect();
            }
        } catch (SQLException e) {
            System.out.println("Erreur verification connexion: " + e.getMessage());
            connect();
        }
        return cnx;
    }
}