module com.medicare.medicarejavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires jbcrypt;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires itextpdf;
    requires jakarta.mail;
    requires com.google.zxing;
    requires com.google.zxing.javase;


    opens com.medicare to javafx.fxml;
    opens com.medicare.controllers to javafx.fxml;
    opens com.medicare.models to javafx.fxml;
    exports com.medicare;
    exports com.medicare.controllers;
}