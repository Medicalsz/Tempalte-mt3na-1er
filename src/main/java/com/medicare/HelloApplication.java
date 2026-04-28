package com.medicare;

<<<<<<< HEAD
import java.io.IOException;

=======
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

<<<<<<< HEAD
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
          FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Medicare - Connexion");
=======
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Medicare");
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}