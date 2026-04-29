module com.medicare.medicarejavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires jakarta.mail;
    requires jakarta.activation;
    requires javafx.web;
    requires com.google.gson;
    requires java.net.http;
    requires java.desktop;

    opens com.medicare to javafx.fxml;
    opens com.medicare.controllers to javafx.fxml;
    opens com.medicare.models to javafx.fxml;
    exports com.medicare;
    exports com.medicare.controllers;
}