package com.medicare;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        java.net.URL themeResource = getClass().getResource("css/theme.css");
        if (themeResource != null) {
            scene.getStylesheets().add(themeResource.toExternalForm());
        } else {
            System.out.println("Warning: css/theme.css not found in resources.");
        }
        stage.setTitle("Medicare");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}