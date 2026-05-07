package com.medicare.controllers;

import com.medicare.models.Partner;
import com.medicare.services.PartnerService;
import com.medicare.controllers.PartnerCardController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.animation.FadeTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class UserPartnersController {

    @FXML
    private TilePane partnersPane;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private ComboBox<String> sortComboBox;

    private StackPane contentArea;

    private final PartnerService partnerService = new PartnerService();
    private List<Partner> allPartners;

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @FXML
    private void initialize() {
        allPartners = partnerService.getAll();
        populateCategoryFilter();
        populateSortComboBox();
        loadPartners(null);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterAndSortPartners();
        });
        
        categoryComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            filterAndSortPartners();
        });

        sortComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            filterAndSortPartners();
        });
    }

    private void populateCategoryFilter() {
        List<String> categories = allPartners.stream()
            .map(Partner::getTypePartenaire)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        categoryComboBox.getItems().add("Toutes les catégories");
        categoryComboBox.getItems().addAll(categories);
        categoryComboBox.getSelectionModel().selectFirst();
    }

    private void populateSortComboBox() {
        sortComboBox.getItems().addAll("Trier par Nom (A-Z)", "Trier par Nom (Z-A)");
    }

    private void filterAndSortPartners() {
        partnersPane.getChildren().clear();
        
        String searchTerm = searchField.getText();
        String selectedCategory = categoryComboBox.getSelectionModel().getSelectedItem();

        List<Partner> filteredPartners = allPartners.stream()
            .filter(partner -> {
                boolean matchesCategory = selectedCategory == null || "Toutes les catégories".equals(selectedCategory) || 
                                          partner.getTypePartenaire().equals(selectedCategory);
                
                boolean matchesSearch = searchTerm == null || searchTerm.trim().isEmpty() ||
                                        partner.getName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                        partner.getTypePartenaire().toLowerCase().contains(searchTerm.toLowerCase());
                
                return matchesCategory && matchesSearch;
            })
            .collect(Collectors.toList());

        // Sorting logic
        String sortSelection = sortComboBox.getSelectionModel().getSelectedItem();
        if (sortSelection != null) {
            if (sortSelection.equals("Trier par Nom (A-Z)")) {
                filteredPartners.sort(Comparator.comparing(Partner::getName, String.CASE_INSENSITIVE_ORDER));
            } else if (sortSelection.equals("Trier par Nom (Z-A)")) {
                filteredPartners.sort(Comparator.comparing(Partner::getName, String.CASE_INSENSITIVE_ORDER).reversed());
            }
        }
        
        if (filteredPartners.isEmpty()) {
            partnersPane.getChildren().add(new Label("Aucun partenaire ne correspond à votre recherche."));
        } else {
            loadPartnerCards(filteredPartners);
        }
    }

    public void loadPartners(StackPane contentArea) {
        this.contentArea = contentArea;
        if (partnersPane == null) {
            System.err.println("partnersPane is not initialized. Check FXML file.");
            return;
        }
        partnersPane.getChildren().clear();

        if (allPartners.isEmpty()) {
            partnersPane.getChildren().add(new Label("Aucun partenaire disponible pour le moment."));
            return;
        }
        
        loadPartnerCards(allPartners);
    }

    private void loadPartnerCards(List<Partner> partners) {
        for (Partner partner : partners) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/collaboration-card-view.fxml"));
                
                loader.setControllerFactory(c -> new PartnerCardController(contentArea));
                
                Node card = loader.load();

                PartnerCardController controller = loader.getController();
                controller.setData(partner);

                // Add fade-in animation
                FadeTransition ft = new FadeTransition(Duration.millis(500), card);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.play();

                partnersPane.getChildren().add(card);
            } catch (Exception e) {
                System.err.println("Failed to load partner card for: " + partner.getName());
                e.printStackTrace();
            }
        }
    }
}
