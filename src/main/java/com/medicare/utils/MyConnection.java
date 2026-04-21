package com.medicare.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private Connection cnx;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/medicare",
                    "root",
                    ""
            );
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
        return cnx;
    }
}