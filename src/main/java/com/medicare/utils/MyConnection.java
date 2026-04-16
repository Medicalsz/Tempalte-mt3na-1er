package com.medicare.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private Connection cnx;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/medicare",
                    "root",
                    "2003"
            );
            System.out.println("Connexion OK !");
            applyMigrations();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void applyMigrations() {
        try {
            addColumnIfMissing("rendez_vous", "motif_annulation", "TEXT DEFAULT NULL");
            addColumnIfMissing("rendez_vous", "hidden_by_patient", "TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing("rendez_vous", "hidden_by_medecin", "TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing("rendez_vous", "proposed_date", "DATE DEFAULT NULL");
            addColumnIfMissing("rendez_vous", "proposed_heure", "TIME DEFAULT NULL");
            addColumnIfMissing("rendez_vous", "report_pending_patient_response", "TINYINT(1) NOT NULL DEFAULT 0");

            // Table ordonnance
            cnx.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS ordonnance (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "rendez_vous_id INT NOT NULL, " +
                "contenu TEXT NOT NULL, " +
                "date_creation DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous(id))"
            );
        } catch (SQLException e) {
            System.out.println("Erreur migration: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) throws SQLException {
        if (!columnExists(table, column)) {
            cnx.createStatement().executeUpdate(
                "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("Migration: colonne " + column + " ajoutee a " + table);
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        DatabaseMetaData meta = cnx.getMetaData();
        ResultSet rs = meta.getColumns(null, null, table, column);
        return rs.next();
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