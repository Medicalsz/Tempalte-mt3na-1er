package com.medicare.controllers;

import com.medicare.models.RendezVous;
import com.medicare.services.RendezVousService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MedecinRdvListController {

    @FXML private VBox container;

    private final RendezVousService service = new RendezVousService();
    private int medecinId;
    private StackPane contentArea;
    private List<RendezVous> allRdvs;
    private String currentFilter = "all";
    private String searchQuery = "";
    private Button btnAll, btnAttente, btnAccepte, btnAnnule;

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }

    public void setMedecinId(int medecinId) {
        this.medecinId = medecinId;
        loadRendezVous();
    }

    private void reloadFullList() {
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                com.medicare.HelloApplication.class.getResource("medecin-rdv-list-view.fxml"));
            Node view = loader.load();
            MedecinRdvListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setMedecinId(medecinId);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadRendezVous() {
        container.getChildren().clear();

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(15);

        // Filtres
        btnAll = createFilterBtn("Tout", "all");
        btnAttente = createFilterBtn("En attente", "en_attente");
        btnAccepte = createFilterBtn("Confirme", "confirme");
        btnAnnule = createFilterBtn("Annule", "annule");

        HBox filters = new HBox(8, btnAll, btnAttente, btnAccepte, btnAnnule);
        filters.setAlignment(Pos.CENTER_LEFT);

        // Barre de recherche
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher un patient...");
        searchField.setPrefWidth(220);
        searchField.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #d1d5db; " +
                             "-fx-padding: 5 12; -fx-font-size: 12px;");
        searchField.textProperty().addListener((obs, old, val) -> {
            searchQuery = val.trim().toLowerCase();
            applyFilters();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(filters, searchField, spacer);
        container.getChildren().add(header);

        allRdvs = service.getByMedecin(medecinId);
        highlightFilter(btnAll);
        applyFilters();
    }

    private Button createFilterBtn(String text, String filter) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #555; -fx-font-size: 12px; " +
                     "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14;");
        btn.setOnAction(e -> {
            currentFilter = filter;
            highlightFilter(btn);
            applyFilters();
        });
        return btn;
    }

    private void highlightFilter(Button active) {
        String normal = "-fx-background-color: #e5e7eb; -fx-text-fill: #555; -fx-font-size: 12px; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14;";
        String selected = "-fx-background-color: #0d9488; -fx-text-fill: white; -fx-font-size: 12px; " +
                          "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14;";
        btnAll.setStyle(normal);
        btnAttente.setStyle(normal);
        btnAccepte.setStyle(normal);
        btnAnnule.setStyle(normal);
        active.setStyle(selected);
    }

    private void applyFilters() {
        if (container.getChildren().size() > 1) {
            container.getChildren().remove(1, container.getChildren().size());
        }

        List<RendezVous> filtered = allRdvs.stream()
                .filter(rv -> currentFilter.equals("all") || rv.getStatut().equals(currentFilter))
                .filter(rv -> searchQuery.isEmpty() ||
                        (rv.getMedecinPrenom() + " " + rv.getMedecinNom()).toLowerCase().contains(searchQuery))
                .toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("Aucun rendez-vous trouve.");
            empty.setStyle("-fx-font-size: 15px; -fx-text-fill: #888; -fx-padding: 40 0 0 0;");
            container.getChildren().add(empty);
            return;
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (RendezVous rv : filtered) {
            container.getChildren().add(buildCard(rv, dateFmt, heureFmt));
        }
    }

    private HBox buildCard(RendezVous rv, DateTimeFormatter dateFmt, DateTimeFormatter heureFmt) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        FontIcon userIcon = new FontIcon(FontAwesomeSolid.USER);
        userIcon.setIconSize(26);
        userIcon.setIconColor(Color.web("#0d9488"));

        VBox infos = new VBox(3);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label patientLabel = new Label(rv.getMedecinPrenom() + " " + rv.getMedecinNom());
        patientLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label dateLabel = new Label(rv.getDate().format(dateFmt) + " a " + rv.getHeure().format(heureFmt));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

        Label statutLabel = new Label(rv.getStatut());
        String statutColor = switch (rv.getStatut()) {
            case "confirme" -> "#16a34a";
            case "annule" -> "#dc2626";
            case "termine" -> "#6b7280";
            default -> "#f59e0b";
        };
        statutLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 10; " +
                             "-fx-background-color: " + statutColor + "; -fx-background-radius: 12;");

        infos.getChildren().addAll(patientLabel, dateLabel, statutLabel);

        // Étiquette si report en attente de réponse patient
        if (rv.isReportPending() && rv.getProposedDate() != null) {
            Label reportLabel = new Label("Report en attente → " + rv.getProposedDate().format(dateFmt) + " a " + rv.getProposedHeure().format(heureFmt));
            reportLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 10; " +
                                 "-fx-background-color: #8b5cf6; -fx-background-radius: 12;");
            infos.getChildren().add(reportLabel);
        }

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER);

        if (rv.isReportPending() && rv.getProposedDate() != null) {
            // Report en attente → seulement bouton annuler le report
            Button btnCancelReport = actionBtn(FontAwesomeSolid.UNDO, "#8b5cf6", "#ede9fe", "Annuler le report");
            btnCancelReport.setOnAction(e -> {
                service.cancelReport(rv.getId());
                showPopup("Report annule", "La proposition de report a ete annulee.",
                          FontAwesomeSolid.UNDO, "#8b5cf6");
            });
            actions.getChildren().add(btnCancelReport);
        } else if (rv.getStatut().equals("en_attente")) {
            Button btnAccept = actionBtn(FontAwesomeSolid.CHECK, "#16a34a", "#dcfce7", "Accepter");
            btnAccept.setOnAction(e -> {
                service.accept(rv.getId());
                showPopup("Rendez-vous accepte", "Le rendez-vous a ete confirme.",
                          FontAwesomeSolid.CHECK_CIRCLE, "#16a34a");
            });

            Button btnRefuse = actionBtn(FontAwesomeSolid.TIMES, "#dc2626", "#fee2e2", "Refuser");
            btnRefuse.setOnAction(e -> showRefuseDialog(rv));

            Button btnReport = actionBtn(FontAwesomeSolid.CALENDAR_ALT, "#f59e0b", "#fef3c7", "Reporter");
            btnReport.setOnAction(e -> showReportDialog(rv));

            actions.getChildren().addAll(btnAccept, btnRefuse, btnReport);
        }

        // Bouton ordonnance sur les RDV confirmés
        if (rv.getStatut().equals("confirme")) {
            boolean hasOrdo = service.hasOrdonnance(rv.getId());
            if (!hasOrdo) {
                Button btnOrdo = actionBtn(FontAwesomeSolid.FILE_MEDICAL, "#3b82f6", "#dbeafe", "Ajouter ordonnance");
                btnOrdo.setOnAction(e -> showOrdonnanceForm(rv));
                actions.getChildren().add(btnOrdo);
            } else {
                Button btnOrdo = actionBtn(FontAwesomeSolid.FILE_PDF, "#16a34a", "#dcfce7", "Voir ordonnance");
                btnOrdo.setOnAction(e -> downloadOrdonnancePDF(rv));
                actions.getChildren().add(btnOrdo);
            }
        }

        Button btnVoir = actionBtn(FontAwesomeSolid.EYE, "#0d9488", "#ccfbf1", "Voir");
        btnVoir.setOnAction(e -> showDetails(rv));
        actions.getChildren().add(btnVoir);

        card.getChildren().addAll(userIcon, infos, actions);
        return card;
    }

    private Button actionBtn(FontAwesomeSolid iconType, String iconColor, String bgColor, String tip) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(14);
        icon.setIconColor(Color.web(iconColor));
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 8;");
        btn.setTooltip(new Tooltip(tip));
        return btn;
    }

    // ========== REFUS AVEC MOTIF ==========

    private void showRefuseDialog(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 50; -fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.TIMES_CIRCLE);
        warnIcon.setIconSize(32);
        warnIcon.setIconColor(Color.web("#dc2626"));
        iconCircle.getChildren().add(warnIcon);

        Label titleLbl = new Label("Refuser ce rendez-vous");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label motifLabel = new Label("Motif d'annulation :");
        motifLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        TextArea motifArea = new TextArea();
        motifArea.setPromptText("Ex: Indisponible ce jour, urgence medicale...");
        motifArea.setPrefRowCount(3);
        motifArea.setPrefWidth(300);
        motifArea.setWrapText(true);
        motifArea.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0; -fx-font-size: 13px;");

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        Button btnConfirm = new Button("Confirmer le refus");
        btnConfirm.setStyle("-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 30;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 30;");

        HBox btns = new HBox(15, btnAnnuler, btnConfirm);
        btns.setAlignment(Pos.CENTER);

        VBox box = new VBox(14, iconCircle, titleLbl, motifLabel, motifArea, errorLbl, btns);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 420, 400);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        btnAnnuler.setOnAction(e -> popup.close());
        btnConfirm.setOnAction(e -> {
            String motif = motifArea.getText().trim();
            if (motif.isEmpty()) {
                errorLbl.setText("Veuillez saisir un motif d'annulation.");
                return;
            }
            popup.close();
            service.refuse(rv.getId(), motif);
            showPopup("Rendez-vous refuse", "Le rendez-vous a ete refuse.",
                      FontAwesomeSolid.TIMES_CIRCLE, "#dc2626");
        });
    }

    // ========== REPORT DIALOG ==========

    private void showReportDialog(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 50; -fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon calIcon = new FontIcon(FontAwesomeSolid.CALENDAR_ALT);
        calIcon.setIconSize(32);
        calIcon.setIconColor(Color.web("#f59e0b"));
        iconCircle.getChildren().add(calIcon);

        Label titleLbl = new Label("Proposer un nouveau creneau");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        DatePicker dp = new DatePicker();
        dp.setPromptText("Nouvelle date");
        dp.setPrefWidth(270);
        dp.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0;");
        dp.setDayCellFactory(d -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });

        ComboBox<String> heureCombo = new ComboBox<>();
        heureCombo.setPromptText("Nouvelle heure");
        heureCombo.setPrefWidth(270);
        heureCombo.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0;");
        for (int h = 8; h < 18; h++) {
            heureCombo.getItems().add(String.format("%02d:00", h));
            heureCombo.getItems().add(String.format("%02d:30", h));
        }

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        Button btnConfirm = new Button("Proposer");
        btnConfirm.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #d97706); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 30;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 30;");

        HBox btns = new HBox(15, btnAnnuler, btnConfirm);
        btns.setAlignment(Pos.CENTER);

        VBox box = new VBox(14, iconCircle, titleLbl, dp, heureCombo, errorLbl, btns);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 400, 400);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        btnAnnuler.setOnAction(e -> popup.close());
        btnConfirm.setOnAction(e -> {
            if (dp.getValue() == null || heureCombo.getValue() == null) {
                errorLbl.setText("Remplissez la date et l'heure.");
                return;
            }
            popup.close();
            service.proposeReport(rv.getId(), dp.getValue(), LocalTime.parse(heureCombo.getValue()));
            showPopup("Report propose", "Le nouveau creneau a ete propose au patient.",
                      FontAwesomeSolid.CALENDAR_CHECK, "#f59e0b");
        });
    }

    // ========== DETAILS MODAL ==========

    private void showDetails(RendezVous rv) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        VBox modal = new VBox(12);
        modal.setAlignment(Pos.CENTER);
        modal.setPadding(new Insets(30));
        modal.setMaxWidth(420);
        modal.setMaxHeight(350);
        modal.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);");

        FontIcon icon = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
        icon.setIconSize(40);
        icon.setIconColor(Color.web("#0d9488"));

        Label titleLabel = new Label("Details du rendez-vous");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0d9488;");

        Label details = new Label(
            "Patient :  " + rv.getMedecinPrenom() + " " + rv.getMedecinNom() + "\n" +
            "Date :  " + rv.getDate().format(dateFmt) + "\n" +
            "Heure :  " + rv.getHeure().format(heureFmt) + "\n" +
            "Statut :  " + rv.getStatut()
        );
        details.setStyle("-fx-font-size: 14px; -fx-text-fill: #444; -fx-line-spacing: 4;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #0d9488; -fx-text-fill: white; -fx-font-size: 13px; " +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        closeBtn.setOnAction(e -> reloadFullList());

        modal.getChildren().addAll(icon, titleLabel, details, closeBtn);

        StackPane overlay = new StackPane(modal);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.4);");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) reloadFullList(); });

        contentArea.getChildren().clear();
        contentArea.getChildren().add(overlay);
    }

    // ========== ORDONNANCE ==========

    private void showOrdonnanceForm(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 50; -fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon ordoIcon = new FontIcon(FontAwesomeSolid.FILE_MEDICAL);
        ordoIcon.setIconSize(32);
        ordoIcon.setIconColor(Color.web("#3b82f6"));
        iconCircle.getChildren().add(ordoIcon);

        Label titleLbl = new Label("Rediger une ordonnance");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Infos pré-remplies
        Label patientInfo = new Label("Patient: " + rv.getMedecinPrenom() + " " + rv.getMedecinNom());
        patientInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Label dateInfo = new Label("Date du RDV: " + rv.getDate().format(dateFmt));
        dateInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        Label contenuLabel = new Label("Prescription :");
        contenuLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TextArea contenuArea = new TextArea();
        contenuArea.setPromptText("Saisir la prescription medicale...\nEx:\n- Doliprane 1000mg : 3 fois par jour pendant 5 jours\n- Amoxicilline 500mg : 2 fois par jour pendant 7 jours");
        contenuArea.setPrefRowCount(6);
        contenuArea.setPrefWidth(400);
        contenuArea.setWrapText(true);
        contenuArea.setStyle("-fx-border-radius: 10; -fx-border-color: #e2e8f0; -fx-font-size: 13px;");

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        Button btnValider = new Button("Generer l'ordonnance");
        btnValider.setStyle("-fx-background-color: linear-gradient(to right, #3b82f6, #6366f1); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 30;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 30;");

        HBox btns = new HBox(15, btnAnnuler, btnValider);
        btns.setAlignment(Pos.CENTER);

        VBox box = new VBox(12, iconCircle, titleLbl, patientInfo, dateInfo, contenuLabel, contenuArea, errorLbl, btns);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 500, 520);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        btnAnnuler.setOnAction(e -> popup.close());
        btnValider.setOnAction(e -> {
            String contenu = contenuArea.getText().trim();
            if (contenu.isEmpty()) {
                errorLbl.setText("Veuillez saisir la prescription.");
                return;
            }
            popup.close();
            service.createOrdonnance(rv.getId(), contenu);
            showPopup("Ordonnance creee", "L'ordonnance a ete enregistree avec succes.",
                      FontAwesomeSolid.FILE_MEDICAL, "#3b82f6");
        });
    }

    private void downloadOrdonnancePDF(RendezVous rv) {
        com.medicare.models.Ordonnance ord = service.getOrdonnanceByRdv(rv.getId());
        if (ord == null) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer l'ordonnance");
        fileChooser.setInitialFileName("ordonnance_" + rv.getId() + ".pdf");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));

        java.io.File file = fileChooser.showSaveDialog(container.getScene().getWindow());
        if (file != null) {
            String result = com.medicare.utils.OrdonnancePDF.generate(ord, file.getAbsolutePath());
            if (result != null) {
                showPopup("PDF genere", "L'ordonnance a ete enregistree en PDF.",
                          FontAwesomeSolid.FILE_PDF, "#16a34a");
                // Ouvrir le PDF
                try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    }

    // ========== POPUP ==========

    private void showPopup(String title, String message, FontAwesomeSolid iconType, String color) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 50; " +
                            "-fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(32);
        icon.setIconColor(Color.web(color));
        iconCircle.getChildren().add(icon);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        msgLbl.setWrapText(true);

        javafx.scene.shape.Rectangle progressBar = new javafx.scene.shape.Rectangle(200, 4);
        progressBar.setFill(Color.web(color));
        progressBar.setArcWidth(4);
        progressBar.setArcHeight(4);

        VBox box = new VBox(18, iconCircle, titleLbl, msgLbl, progressBar);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(35));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 380, 270);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(progressBar.widthProperty(), 200)),
            new javafx.animation.KeyFrame(Duration.seconds(2), new javafx.animation.KeyValue(progressBar.widthProperty(), 0))
        );
        timeline.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            popup.close();
            reloadFullList();
        });
        pause.play();
    }
}

