package com.medicare;

import com.medicare.services.RappelEmailScheduler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    private RappelEmailScheduler rappelScheduler;

    @Override
    public void start(Stage stage) throws IOException {
        // Démarrer le scheduler de rappels email en arrière-plan
        rappelScheduler = new RappelEmailScheduler();
        rappelScheduler.demarrer();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Medicare");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (rappelScheduler != null) {
            rappelScheduler.arreter();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}