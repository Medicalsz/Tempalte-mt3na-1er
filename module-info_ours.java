module com.medicare.medicarejavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires java.desktop;
    requires java.sql;
    requires jbcrypt;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires com.github.librepdf.openpdf;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires webcam.capture;
    requires cloudinary.core;
    requires cloudinary.http5;
    requires stripe.java;
    requires com.google.gson;


    opens com.medicare to javafx.fxml;
    opens com.medicare.controllers to javafx.fxml;
    opens com.medicare.models to javafx.fxml;
    opens com.medicare.services to com.google.gson;
    exports com.medicare;
    exports com.medicare.controllers;
}
