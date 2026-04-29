package com.medicare.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/medicare";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static volatile MyConnection instance;
    private Connection cnx;

    private MyConnection() {
        connect();
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            synchronized (MyConnection.class) {
                if (instance == null) {
                    instance = new MyConnection();
                }
            }
        }
        return instance;
    }

    public synchronized Connection getConnection() {
        try {
            if (cnx == null || cnx.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            System.out.println("Erreur verification connexion MySQL: " + e.getMessage());
            connect();
        }
        return cnx;
    }

    public synchronized Connection getCnx() {
        return getConnection();
    }

    public synchronized boolean isConnected() {
        try {
            return getConnection() != null && !cnx.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private void connect() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion MySQL OK");
        } catch (SQLException e) {
            cnx = null;
            System.out.println("Erreur connexion MySQL: " + e.getMessage());
        }
    }
}
