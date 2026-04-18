package com.medicare.controllers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.models.Partner;
import com.medicare.services.PartnerService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AdminPartnersController {

    @FXML
    private VBox container;

    private final PartnerService partnerService = new PartnerService();
    private TextField searchField;

    @FXML
    private void initialize() {
        setupUI();
        loadPartners(null);
    }

    private void setupUI() {
        container.getChildren().clear();

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 15, 0));

        Label title = new Label("Gestion des Partenaires");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Field
        searchField = new TextField();
        searchField.setPromptText("Rechercher par nom...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            loadPartners(newValue);
        });

        // Export MenuButton
        MenuButton exportMenuBtn = new MenuButton("Exporter");
        exportMenuBtn.setGraphic(new FontIcon(FontAwesomeSolid.DOWNLOAD));
        exportMenuBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");

        MenuItem pdfItem = new MenuItem("Exporter en PDF");
        pdfItem.setGraphic(new FontIcon(FontAwesomeSolid.FILE_PDF));
        pdfItem.setOnAction(e -> exportToPdf());

        MenuItem csvItem = new MenuItem("Exporter en CSV");
        csvItem.setGraphic(new FontIcon(FontAwesomeSolid.FILE_CSV));
        csvItem.setOnAction(e -> exportToCsv());

        MenuItem printItem = new MenuItem("Imprimer la liste");
        printItem.setGraphic(new FontIcon(FontAwesomeSolid.PRINT));
        printItem.setOnAction(e -> printPartners());

        exportMenuBtn.getItems().addAll(pdfItem, csvItem, printItem);

        Button addBtn = new Button("Ajouter Partenaire");
        addBtn.setGraphic(new FontIcon(FontAwesomeSolid.PLUS));
        addBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showPartnerForm(null));

        header.getChildren().addAll(title, spacer, exportMenuBtn, searchField, addBtn);
        container.getChildren().add(header);

        // Table header
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");
        tableHeader.getChildren().addAll(
            colLabel("Nom", 150),
            colLabel("Type", 120),
            colLabel("Email", 200),
            colLabel("Actions", 120)
        );
        container.getChildren().add(tableHeader);
    }

    private void loadPartners(String searchTerm) {
        // Clear only the rows, not the header
        container.getChildren().removeIf(node -> node.getStyleClass().contains("partner-row"));
        container.getChildren().removeIf(node -> node instanceof Label && ((Label)node).getText().startsWith("Aucun"));


        List<Partner> partners;
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            partners = partnerService.getAll();
        } else {
            partners = partnerService.searchByName(searchTerm);
        }

        if (partners.isEmpty()) {
            Label empty = new Label("Aucun partenaire trouvé.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < partners.size(); i++) {
            Partner p = partners.get(i);
            HBox row = new HBox();
            row.getStyleClass().add("partner-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            Label nom = new Label(p.getName());
            nom.setPrefWidth(150);
            nom.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

            Label type = new Label(p.getTypePartenaire());
            type.setPrefWidth(120);
            type.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
            
            Label email = new Label(p.getEmail());
            email.setPrefWidth(200);
            email.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            // Actions
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);
            actions.setPrefWidth(120);

            Button btnVoir = actionBtn(FontAwesomeSolid.EYE, "#7c3aed", "#ede9fe");
            btnVoir.setTooltip(new Tooltip("Modifier les détails"));
            btnVoir.setOnAction(e -> showPartnerForm(p));

            Button btnDelete = actionBtn(FontAwesomeSolid.TRASH_ALT, "#dc2626", "#fee2e2");
            btnDelete.setTooltip(new Tooltip("Supprimer"));
            btnDelete.setOnAction(e -> showDeleteConfirm(p));

            actions.getChildren().addAll(btnVoir, btnDelete);

            row.getChildren().addAll(nom, type, email, actions);
            container.getChildren().add(row);
        }
    }

    private Label colLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        return l;
    }

    private Button actionBtn(FontAwesomeSolid iconType, String iconColor, String bgColor) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(13);
        icon.setIconColor(Color.web(iconColor));
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 6;");
        return btn;
    }

    private void showPartnerForm(Partner partner) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminPartnersController.class.getResource("/com/medicare/partner-form-view.fxml"));
            VBox formRoot = loader.load();

            PartnerFormController controller = loader.getController();
            controller.setPartner(partner);
            controller.setOnSave(() -> loadPartners(searchField.getText()));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(partner == null ? "Ajouter un Partenaire" : "Modifier le Partenaire");
            stage.setScene(new Scene(formRoot));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showDeleteConfirm(Partner partner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le partenaire : " + partner.getName());
        alert.setContentText("Êtes-vous sûr de vouloir continuer ? Cette action est irréversible.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                partnerService.delete(partner.getId());
                loadPartners(searchField.getText()); // Refresh the list
            }
        });
    }

    private void exportToPdf() {
        List<Partner> partners = partnerService.searchByName(searchField.getText());
        if (partners.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Information", "Aucun partenaire à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier PDF");
        fileChooser.setInitialFileName("partenaires.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(container.getScene().getWindow());

        if (file != null) {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    drawPdfTable(contentStream, partners);
                }

                document.save(file);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Les données ont été exportées avec succès dans " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue lors de l'exportation du fichier PDF.");
            }
        }
    }

    private void drawPdfTable(PDPageContentStream contentStream, List<Partner> partners) throws IOException {
        final int rows = partners.size() + 1;
        final int cols = 5; // ID, Nom, Type, Email, Statut
        final float rowHeight = 20f;
        final float tableWidth = 500f;
        final float tableHeight = rowHeight * rows;
        final float startX = 50f;
        final float startY = 750f;

        // Headers
        String[] headers = {"ID", "Nom", "Type", "Email", "Statut"};
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);

        float nextX = startX;
        float nextY = startY;

        for (String header : headers) {
            contentStream.beginText();
            contentStream.newLineAtOffset(nextX, nextY);
            contentStream.showText(header);
            contentStream.endText();
            nextX += tableWidth / cols;
        }

        // Data
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        nextY -= rowHeight;

        for (Partner p : partners) {
            nextX = startX;
            String[] rowData = {
                String.valueOf(p.getId()),
                p.getName(),
                p.getTypePartenaire(),
                p.getEmail(),
                p.getStatut()
            };

            for (String data : rowData) {
                contentStream.beginText();
                contentStream.newLineAtOffset(nextX, nextY);
                contentStream.showText(data != null ? data : "");
                contentStream.endText();
                nextX += tableWidth / cols;
            }
            nextY -= rowHeight;
        }
    }

    private void exportToCsv() {
        List<Partner> partners = partnerService.searchByName(searchField.getText());
        if (partners.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Information", "Aucun partenaire à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier CSV");
        fileChooser.setInitialFileName("partenaires.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File file = fileChooser.showSaveDialog(container.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                         .withHeader("ID", "Nom", "Type", "Email", "Téléphone", "Adresse", "Statut", "Date Partenariat"))) {
                for (Partner p : partners) {
                    csvPrinter.printRecord(
                        p.getId(),
                        p.getName(),
                        p.getTypePartenaire(),
                        p.getEmail(),
                        p.getTelephone(),
                        p.getAdresse(),
                        p.getStatut(),
                        p.getDatePartenariat() != null ? p.getDatePartenariat().toString() : ""
                    );
                }
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Les données ont été exportées avec succès dans " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue lors de l'exportation du fichier CSV.");
            }
        }
    }

    private void printPartners() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(container.getScene().getWindow())) {
            List<Partner> partners = partnerService.searchByName(searchField.getText());
            if (partners.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Information", "Aucun partenaire à imprimer.");
                return;
            }
            
            Node printableArea = createPrintableTable(partners);
            boolean success = job.printPage(printableArea);
            if (success) {
                job.endJob();
            }
        }
    }

    private Node createPrintableTable(List<Partner> partners) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));

        // Headers
        String[] headers = {"ID", "Nom", "Type", "Email", "Statut"};
        for (int i = 0; i < headers.length; i++) {
            Label label = new Label(headers[i]);
            label.setStyle("-fx-font-weight: bold;");
            grid.add(label, i, 0);
        }

        // Data
        int row = 1;
        for (Partner p : partners) {
            grid.add(new Label(String.valueOf(p.getId())), 0, row);
            grid.add(new Label(p.getName()), 1, row);
            grid.add(new Label(p.getTypePartenaire()), 2, row);
            grid.add(new Label(p.getEmail()), 3, row);
            grid.add(new Label(p.getStatut()), 4, row);
            row++;
        }
        return grid;
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}