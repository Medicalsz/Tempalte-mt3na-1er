package com.medicare.controllers;

import com.medicare.models.Partner;
import com.medicare.services.PartnerService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class PartnerFormController {

    @FXML private TextField nomField;
    @FXML private TextField typeField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField adresseField;
    @FXML private TextField statutField;
    @FXML private DatePicker datePicker;
    @FXML private Button saveBtn;

    private PartnerService partnerService = new PartnerService();
    private Partner currentPartner;
    private Runnable onSaveCallback;

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setPartner(Partner partner) {
        this.currentPartner = partner;
        if (partner != null) {
            nomField.setText(partner.getName());
            typeField.setText(partner.getTypePartenaire());
            emailField.setText(partner.getEmail());
            phoneField.setText(partner.getTelephone());
            adresseField.setText(partner.getAdresse());
            statutField.setText(partner.getStatut());
            if (partner.getDatePartenariat() != null) {
                datePicker.setValue(partner.getDatePartenariat());
            }
        }
    }

    @FXML
    private void handleSave() {
        String nom = nomField.getText();
        String type = typeField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String adresse = adresseField.getText();
        String statut = statutField.getText();
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