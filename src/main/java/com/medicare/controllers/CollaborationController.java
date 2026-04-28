package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.Collaboration;
import com.medicare.models.User;
import com.medicare.services.CollaborationService;
import com.medicare.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CollaborationController implements Initializable {

    @FXML
    private TilePane collaborationsPane;

    private final CollaborationService collaborationService = new CollaborationService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadUserCollaborations();
    }

    private void loadUserCollaborations() {
        User currentUser = Session.getInstance().getCurrentUser();

        if (currentUser == null) {
            collaborationsPane.getChildren().add(new Label("Veuillez vous connecter pour voir vos collaborations."));
            return;
        }

        List<Collaboration> collaborationList = collaborationService.getCollaborationsForUser(currentUser.getId());

        if (collaborationList == null || collaborationList.isEmpty()) {
            collaborationsPane.getChildren().add(new Label("Vous n'avez aucune collaboration pour le moment."));
        } else {
            collaborationsPane.getChildren().clear();
            for (Collaboration collaboration : collaborationList) {
                try {
                    FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("collaboration-card-view.fxml"));
                    Node card = loader.load();
                    CollaborationCardController controller = loader.getController();
                    controller.setData(collaboration);
                    collaborationsPane.getChildren().add(card);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}