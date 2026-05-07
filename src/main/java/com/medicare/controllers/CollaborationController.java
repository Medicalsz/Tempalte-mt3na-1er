package com.medicare.controllers;

import com.medicare.models.Collaboration;
import com.medicare.services.CollaborationService;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

public class CollaborationController {

    @FXML
    private TableView<Collaboration> collaborationsTable;
    @FXML
    private TableColumn<Collaboration, Integer> idColumn;
    @FXML
    private TableColumn<Collaboration, String> partnerNameColumn;
    @FXML
    private TableColumn<Collaboration, String> titleColumn;
    @FXML
    private TableColumn<Collaboration, String> descriptionColumn;
    @FXML
    private TableColumn<Collaboration, LocalDate> startDateColumn;
    @FXML
    private TableColumn<Collaboration, LocalDate> endDateColumn;

    private final CollaborationService collaborationService = new CollaborationService();

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        partnerNameColumn.setCellValueFactory(new PropertyValueFactory<>("partnerName"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateFin"));

        loadCollaborations();
    }

    private void loadCollaborations() {
        List<Collaboration> collaborations = collaborationService.getAll();
        collaborationsTable.getItems().setAll(collaborations);
    }
}
