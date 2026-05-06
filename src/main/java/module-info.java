module com.medicare.medicarejavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires jbcrypt;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    requires com.google.gson;
    requires com.google.api.client;          // google-http-client (core + json ifaces)
    requires com.google.api.client.json.gson; // google-http-client-gson
    requires com.google.api.client.auth;      // google-oauth-client
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;
    requires google.api.client;               // google-api-client (googleapis auth)
    requires google.api.services.oauth2.v2.rev157;
    requires jdk.httpserver;
    requires java.desktop;
    requires javafx.web;
    requires jdk.jsobject;
    opens com.medicare to javafx.fxml;
    opens com.medicare.controllers to javafx.fxml;
    opens com.medicare.models to javafx.fxml;
    opens com.medicare.ui to javafx.web;
    exports com.medicare;
    exports com.medicare.controllers;
}
