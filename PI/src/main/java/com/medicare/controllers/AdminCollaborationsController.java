package com.medicare.controllers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
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

import com.medicare.models.Collaboration;
import com.medicare.services.CollaborationService;

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

public class AdminCollaborationsController {

    @FXML
    private VBox container;

    private final CollaborationService collaborationService = new CollaborationService();
    private TextField searchField;
    private String currentSortColumn = "date_debut"; // Default sort
    private boolean sortAscending = false;

    @FXML
    private void initialize() {
        setupUI();
        loadCollaborations(null, currentSortColumn, sortAscending);
    }

    private void setupUI() {
        container.getChildren().clear();

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 15, 0));

        Label title = new Label("Gestion des Collaborations");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Field
        searchField = new TextField();
        searchField.setPromptText("Rechercher par titre...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            loadCollaborations(newValue, currentSortColumn, sortAscending);
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
        printItem.setOnAction(e -> printCollaborations());

        exportMenuBtn.getItems().addAll(pdfItem, csvItem, printItem);

        Button addBtn = new Button("Ajouter Collaboration");
        addBtn.setGraphic(new FontIcon(FontAwesomeSolid.PLUS));
        addBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showCollaborationForm(null));

        header.getChildren().addAll(title, spacer, exportMenuBtn, searchField, addBtn);
        container.getChildren().add(header);

        // Table header
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");
        tableHeader.getChildren().addAll(
            createSortableColumn("Titre", "titre", 200),
            createSortableColumn("Partenaire", "partner_name", 150),
            createSortableColumn("Statut", "statut", 100),
            createSortableColumn("Date Fin", "date_fin", 120),
            colLabel("Actions", 120) // Actions column is not sortable
        );
        container.getChildren().add(tableHeader);
    }

    private void loadCollaborations(String searchTerm, String sortColumn, boolean ascending) {
        // Clear only the rows, not the header
        container.getChildren().removeIf(node -> node.getStyleClass().contains("collaboration-row"));
        container.getChildren().removeIf(node -> node instanceof Label && ((Label)node).getText().startsWith("Aucune"));

        List<Collaboration> collaborations;
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            collaborations = collaborationService.getAllSorted(sortColumn, ascending);
        } else {
            collaborations = collaborationService.searchByTitre(searchTerm); // Note: search might not be sorted by the same column
        }
        if (collaborations.isEmpty()) {
            Label empty = new Label("Aucune collaboration trouvée.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 0; i < collaborations.size(); i++) {
            Collaboration c = collaborations.get(i);
            HBox row = new HBox();
            row.getStyleClass().add("collaboration-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            Label titre = new Label(c.getTitre());
            titre.setPrefWidth(200);
            titre.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

            Label partner = new Label(c.getPartnerName() != null ? c.getPartnerName() : "N/A");
            partner.setPrefWidth(150);
            partner.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            Label statut = new Label(c.getStatut());
            statut.setPrefWidth(100);
            statut.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            String dateFinText = (c.getDateFin() != null) ? c.getDateFin().format(formatter) : "Indéfinie";
            Label dateFin = new Label(dateFinText);
            dateFin.setPrefWidth(120);
            dateFin.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            // Actions
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);
            actions.setPrefWidth(120);

            Button btnVoir = actionBtn(FontAwesomeSolid.EYE, "#7c3aed", "#ede9fe");
            btnVoir.setTooltip(new Tooltip("Modifier les détails"));
            btnVoir.setOnAction(e -> showCollaborationForm(c));

            Button btnDelete = actionBtn(FontAwesomeSolid.TRASH_ALT, "#dc2626", "#fee2e2");
            btnDelete.setTooltip(new Tooltip("Supprimer"));
            btnDelete.setOnAction(e -> showDeleteConfirm(c));

            actions.getChildren().addAll(btnVoir, btnDelete);

            row.getChildren().addAll(titre, partner, statut, dateFin, actions);
            container.getChildren().add(row);
        }
    }

    private Label colLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        return l;
    }

    private HBox createSortableColumn(String labelText, String dbColumnName, double width) {
        HBox headerBox = new HBox(5);
        headerBox.setPrefWidth(width);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle("-fx-cursor: hand;");

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

        FontIcon sortIcon = new FontIcon();
        sortIcon.setIconSize(12);
        sortIcon.setIconColor(Color.WHITE);

        if (dbColumnName.equals(currentSortColumn)) {
            sortIcon.setIconCode(sortAscending ? FontAwesomeSolid.SORT_UP : FontAwesomeSolid.SORT_DOWN);
        } else {
            sortIcon.setIconCode(FontAwesomeSolid.SORT);
        }
        
        headerBox.getChildren().addAll(label, sortIcon);

        headerBox.setOnMouseClicked(e -> {
            if (dbColumnName.equals(currentSortColumn)) {
                sortAscending = !sortAscending;
            } else {
                currentSortColumn = dbColumnName;
                sortAscending = true;
            }
            // Rebuild the header and reload data
            setupUI(); 
            loadCollaborations(searchField.getText(), currentSortColumn, sortAscending);
        });
        
        return headerBox;
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

    private void showCollaborationForm(Collaboration collaboration) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/collaboration-form-view.fxml"));
            Parent root = loader.load();

            CollaborationFormController controller = loader.getController();
            controller.setCollaboration(collaboration);
            controller.setOnFormClose(updated -> {
                if (updated) {
                    loadCollaborations(searchField.getText(), currentSortColumn, sortAscending);
                }
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(collaboration == null ? "Ajouter une Collaboration" : "Modifier la Collaboration");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger le formulaire de collaboration.");
        }
    }

    private void showDeleteConfirm(Collaboration collaboration) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la collaboration : " + collaboration.getTitre());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette collaboration ? Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            collaborationService.delete(collaboration.getId());
            loadCollaborations(searchField.getText(), currentSortColumn, sortAscending);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "La collaboration a été supprimée avec succès.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void exportToCsv() {
        List<Collaboration> collaborations = collaborationService.getAllSorted(currentSortColumn, sortAscending);
        if (collaborations.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Information", "Aucune collaboration à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier CSV");
        fileChooser.setInitialFileName("collaborations.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File file = fileChooser.showSaveDialog(container.getScene().getWindow());

        if (file != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            try (FileWriter writer = new FileWriter(file);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                         .withHeader("ID", "Titre", "Partenaire", "Statut", "Date Début", "Date Fin", "Description"))) {

                for (Collaboration c : collaborations) {
                    csvPrinter.printRecord(
                        c.getId(),
                        c.getTitre(),
                        c.getPartnerName(),
                        c.getStatut(),
                        c.getDateDebut() != null ? c.getDateDebut().format(formatter) : "",
                        c.getDateFin() != null ? c.getDateFin().format(formatter) : "",
                        c.getDescription()
                    );
                }
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Les données ont été exportées avec succès dans " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue lors de l'exportation du fichier CSV.");
            }
        }
    }

    private void exportToPdf() {
        List<Collaboration> collaborations = collaborationService.getAllSorted(currentSortColumn, sortAscending);
        if (collaborations.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Information", "Aucune collaboration à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier PDF");
        fileChooser.setInitialFileName("collaborations.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(container.getScene().getWindow());

        if (file != null) {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    drawPdfTable(contentStream, collaborations);
                }

                document.save(file);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Les données ont été exportées avec succès dans " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue lors de l'exportation du fichier PDF.");
            }
        }
    }

    private void drawPdfTable(PDPageContentStream contentStream, List<Collaboration> collaborations) throws IOException {
        final int rows = collaborations.size() + 1;
        final int cols = 4;
        final float rowHeight = 20f;
        final float tableWidth = 500f;
        final float tableHeight = rowHeight * rows;
        final float startX = 50f;
        final float startY = 750f;

        // Headers
        String[] headers = {"Titre", "Partenaire", "Statut", "Date Fin"};
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Collaboration c : collaborations) {
            nextX = startX;
            String[] rowData = {
                c.getTitre(),
                c.getPartnerName(),
                c.getStatut(),
                c.getDateFin() != null ? c.getDateFin().format(formatter) : "N/A"
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

    private void printCollaborations() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(container.getScene().getWindow())) {
            List<Collaboration> collaborations = collaborationService.getAllSorted(currentSortColumn, sortAscending);
            if (collaborations.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Information", "Aucune collaboration à imprimer.");
                return;
            }
            
            Node printableArea = createPrintableTable(collaborations);
            boolean success = job.printPage(printableArea);
            if (success) {
                job.endJob();
            }
        }
    }

    private Node createPrintableTable(List<Collaboration> collaborations) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));

        // Headers
        String[] headers = {"Titre", "Partenaire", "Statut", "Date Fin"};
        for (int i = 0; i < headers.length; i++) {
            Label label = new Label(headers[i]);
            label.setStyle("-fx-font-weight: bold;");
            grid.add(label, i, 0);
        }

        // Data
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int row = 1;
        for (Collaboration c : collaborations) {
            grid.add(new Label(c.getTitre()), 0, row);
            grid.add(new Label(c.getPartnerName()), 1, row);
            grid.add(new Label(c.getStatut()), 2, row);
            grid.add(new Label(c.getDateFin() != null ? c.getDateFin().format(formatter) : "N/A"), 3, row);
            row++;
        }
        return grid;
    }
}