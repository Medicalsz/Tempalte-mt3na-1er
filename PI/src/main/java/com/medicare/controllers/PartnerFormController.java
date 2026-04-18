package com.medicare.controllers;

import com.medicare.models.Partner;
import com.medicare.services.PartnerService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.collections.FXCollections;

import java.time.LocalDate;

public class PartnerFormController {

    @FXML private TextField nomField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField adresseField;
    @FXML private ComboBox<String> statutComboBox;
    @FXML private DatePicker datePicker;
    @FXML private Button saveBtn;

    private PartnerService partnerService = new PartnerService();
    private Partner currentPartner;
    private Runnable onSaveCallback;

    @FXML
    private void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList("Hôpital", "Clinique", "Laboratoire", "Pharmacie", "Association"));
        statutComboBox.setItems(FXCollections.observableArrayList("Actif", "Inactif"));
    }

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setPartner(Partner partner) {
        this.currentPartner = partner;
        if (partner != null) {
            nomField.setText(partner.getName());
            typeComboBox.setValue(partner.getTypePartenaire());
            emailField.setText(partner.getEmail());
            phoneField.setText(partner.getTelephone());
            adresseField.setText(partner.getAdresse());
            statutComboBox.setValue(partner.getStatut());
            if (partner.getDatePartenariat() != null) {
                datePicker.setValue(partner.getDatePartenariat());
            }
        }
    }

    @FXML
    private void handleSave() {
        String nom = nomField.getText();
        String type = typeComboBox.getValue();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String adresse = adresseField.getText();
        String statut = statutComboBox.getValue();
        LocalDate date = datePicker.getValue();


        if (currentPartner == null) {
            // Create new partner
            Partner newPartner = new Partner();
            newPartner.setName(nom);
            newPartner.setTypePartenaire(type);
            newPartner.setEmail(email);
            newPartner.setTelephone(phone);
            newPartner.setAdresse(adresse);
            newPartner.setStatut(statut);
            newPartner.setDatePartenariat(date);
            partnerService.add(newPartner);
        } else {
            // Update existing partner
            currentPartner.setName(nom);
            currentPartner.setTypePartenaire(type);
            currentPartner.setEmail(email);
            currentPartner.setTelephone(phone);
            currentPartner.setAdresse(adresse);
            currentPartner.setStatut(statut);
            currentPartner.setDatePartenariat(date);
            partnerService.update(currentPartner);
        }

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
        
        closeStage();
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }
}