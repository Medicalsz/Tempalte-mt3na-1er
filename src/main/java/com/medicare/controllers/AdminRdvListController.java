package com.medicare.controllers;

import com.medicare.models.RendezVous;
import com.medicare.services.RendezVousService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminRdvListController {

    @FXML private VBox container;

    private final RendezVousService service = new RendezVousService();
    private List<RendezVous> allRdvs;
    private String currentFilter = "all";
    private String searchQuery = "";
    private Button btnAll, btnConfirme, btnAttente, btnAnnule;

    // Indices pour savoir où commencent les rows dans container
    private int rowStartIndex;

    @FXML
    private void initialize() {
        allRdvs = service.getAllRendezVous();
        buildUI();
    }

    private void buildUI() {
        container.getChildren().clear();
        container.setSpacing(14);

        // ==================== STATS SECTION ====================
        buildStatsSection();

        // ==================== SEPARATOR ====================
        Region sep = new Region();
        sep.setMinHeight(5);
        container.getChildren().add(sep);

        // ==================== HEADER + FILTERS + SEARCH ====================
        buildListHeader();

        // ==================== TABLE HEADER ====================
        buildTableHeader();

        // Marquer où commencent les data rows
        rowStartIndex = container.getChildren().size();

        // ==================== ROWS ====================
        applyFilters();
    }

    // ==================== STATS ====================

    private void buildStatsSection() {
        // Compter par statut
        long total = allRdvs.size();
        long confirmes = allRdvs.stream().filter(r -> "confirme".equals(r.getStatut())).count();
        long attente = allRdvs.stream().filter(r -> "en_attente".equals(r.getStatut())).count();
        long annules = allRdvs.stream().filter(r -> "annule".equals(r.getStatut())).count();

        // KPI Cards
        HBox kpiRow = new HBox(12);
        kpiRow.setAlignment(Pos.CENTER);
        kpiRow.getChildren().addAll(
            buildKpiCard("Total", String.valueOf(total), FontAwesomeSolid.CALENDAR_ALT, "#7c3aed", "#f3f0ff"),
            buildKpiCard("Confirmes", String.valueOf(confirmes), FontAwesomeSolid.CHECK_CIRCLE, "#16a34a", "#dcfce7"),
            buildKpiCard("En attente", String.valueOf(attente), FontAwesomeSolid.CLOCK, "#f59e0b", "#fef3c7"),
            buildKpiCard("Annules", String.valueOf(annules), FontAwesomeSolid.TIMES_CIRCLE, "#dc2626", "#fee2e2")
        );
        container.getChildren().add(kpiRow);

        // Charts row
        HBox chartsRow = new HBox(14);
        chartsRow.setAlignment(Pos.TOP_CENTER);
        chartsRow.setPrefHeight(250);

        // PieChart
        VBox pieBox = buildPieChart(confirmes, attente, annules);
        HBox.setHgrow(pieBox, Priority.ALWAYS);

        // BarChart par jour
        VBox barBox = buildBarChart();
        HBox.setHgrow(barBox, Priority.ALWAYS);

        chartsRow.getChildren().addAll(pieBox, barBox);
        container.getChildren().add(chartsRow);
    }

    private VBox buildKpiCard(String title, String value, FontAwesomeSolid iconType, String color, String bgColor) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 10, 14, 10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(38, 38);
        iconCircle.setMaxSize(38, 38);
        iconCircle.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 19;");
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(color));
        iconCircle.getChildren().add(icon);

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(iconCircle, valLbl, titleLbl);
        return card;
    }

    private VBox buildPieChart(long confirmes, long attente, long annules) {
        PieChart.Data dConfirme = new PieChart.Data("Confirmes (" + confirmes + ")", confirmes);
        PieChart.Data dAttente = new PieChart.Data("En attente (" + attente + ")", attente);
        PieChart.Data dAnnule = new PieChart.Data("Annules (" + annules + ")", annules);

        PieChart pie = new PieChart(FXCollections.observableArrayList(dConfirme, dAttente, dAnnule));
        pie.setTitle("Repartition par statut");
        pie.setLegendSide(Side.BOTTOM);
        pie.setLabelsVisible(false);
        pie.setPrefSize(350, 230);
        pie.setMaxHeight(230);
        pie.setStyle("-fx-font-size: 11px;");

        // Couleurs custom après rendu
        pie.applyCss();
        pie.layout();

        VBox box = new VBox(pie);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");

        // Appliquer les couleurs + interactivité après que le chart est ajouté à la scène
        pie.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                applyPieColors(pie, new String[]{"#16a34a", "#f59e0b", "#dc2626"});
                applyPieInteractivity(pie);
            }
        });

        return box;
    }

    private void applyPieColors(PieChart pie, String[] colors) {
        javafx.application.Platform.runLater(() -> {
            for (int i = 0; i < pie.getData().size() && i < colors.length; i++) {
                PieChart.Data d = pie.getData().get(i);
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-pie-color: " + colors[i] + ";");
                }
            }
            // Legend colors
            int idx = 0;
            for (javafx.scene.Node node : pie.lookupAll(".chart-legend-item-symbol")) {
                if (idx < colors.length) {
                    node.setStyle("-fx-background-color: " + colors[idx] + ";");
                    idx++;
                }
            }
        });
    }

    private void applyPieInteractivity(PieChart pie) {
        javafx.application.Platform.runLater(() -> {
            double total = pie.getData().stream().mapToDouble(PieChart.Data::getPieValue).sum();
            for (PieChart.Data d : pie.getData()) {
                if (d.getNode() == null) continue;
                javafx.scene.Node node = d.getNode();
                double pct = total > 0 ? (d.getPieValue() / total) * 100 : 0;
                String tooltipText = d.getName() + "\n" + (int) d.getPieValue() + " RDV  -  " + String.format("%.1f", pct) + "%";

                Tooltip tp = new Tooltip(tooltipText);
                tp.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-size: 12px; " +
                            "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 12;");
                Tooltip.install(node, tp);

                node.setOnMouseEntered(e -> {
                    node.setScaleX(1.08);
                    node.setScaleY(1.08);
                    node.setOpacity(0.85);
                    node.setCursor(javafx.scene.Cursor.HAND);
                });
                node.setOnMouseExited(e -> {
                    node.setScaleX(1.0);
                    node.setScaleY(1.0);
                    node.setOpacity(1.0);
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private VBox buildBarChart() {
        // Compter par jour de la semaine
        Map<DayOfWeek, Long> byDay = allRdvs.stream()
            .collect(Collectors.groupingBy(r -> r.getDate().getDayOfWeek(), Collectors.counting()));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        yAxis.setAutoRanging(false);
        long maxCount = byDay.values().stream().mapToLong(Long::longValue).max().orElse(1);
        yAxis.setUpperBound(maxCount + 1);
        yAxis.setLowerBound(0);
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number object) {
                if (object.intValue() == object.doubleValue()) {
                    return String.valueOf(object.intValue());
                }
                return "";
            }
        });

        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setTitle("RDV par jour");
        bar.setLegendVisible(false);
        bar.setPrefSize(350, 230);
        bar.setMaxHeight(230);
        bar.setCategoryGap(8);
        bar.setBarGap(2);
        bar.setStyle("-fx-font-size: 11px;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[] joursFr = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        DayOfWeek[] days = DayOfWeek.values();
        for (int i = 0; i < 7; i++) {
            long count = byDay.getOrDefault(days[i], 0L);
            series.getData().add(new XYChart.Data<>(joursFr[i], count));
        }
        bar.getData().add(series);

        // Couleur des barres + interactivité
        bar.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                javafx.application.Platform.runLater(() -> {
                    for (XYChart.Data<String, Number> d : series.getData()) {
                        if (d.getNode() == null) continue;
                        javafx.scene.Node node = d.getNode();
                        node.setStyle("-fx-bar-fill: #7c3aed;");

                        String tooltipText = d.getXValue() + " : " + d.getYValue().intValue() + " RDV";
                        Tooltip tp = new Tooltip(tooltipText);
                        tp.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-size: 12px; " +
                                    "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 12;");
                        Tooltip.install(node, tp);

                        node.setOnMouseEntered(e -> {
                            node.setStyle("-fx-bar-fill: #9333ea;");
                            node.setScaleX(1.05);
                            node.setScaleY(1.05);
                            node.setCursor(javafx.scene.Cursor.HAND);
                        });
                        node.setOnMouseExited(e -> {
                            node.setStyle("-fx-bar-fill: #7c3aed;");
                            node.setScaleX(1.0);
                            node.setScaleY(1.0);
                        });
                    }
                });
            }
        });

        VBox box = new VBox(bar);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
        return box;
    }

    // ==================== LIST HEADER + FILTERS ====================

    private void buildListHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Liste des rendez-vous");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Filter buttons
        btnAll = createFilterBtn("Tout", "all");
        btnConfirme = createFilterBtn("Confirmes", "confirme");
        btnAttente = createFilterBtn("En attente", "en_attente");
        btnAnnule = createFilterBtn("Annules", "annule");

        HBox filters = new HBox(6, btnAll, btnConfirme, btnAttente, btnAnnule);
        filters.setAlignment(Pos.CENTER_LEFT);
        highlightFilter(btnAll);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher patient, medecin...");
        searchField.setPrefWidth(220);
        searchField.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #d1d5db; " +
                             "-fx-padding: 5 12; -fx-font-size: 12px;");
        searchField.textProperty().addListener((obs, old, val) -> {
            searchQuery = val == null ? "" : val.trim().toLowerCase();
            applyFilters();
        });

        header.getChildren().addAll(title, filters, spacer, searchField);
        container.getChildren().add(header);
    }

    private Button createFilterBtn(String text, String filter) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #555; -fx-font-size: 11px; " +
                     "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 4 12;");
        btn.setOnAction(e -> {
            currentFilter = filter;
            highlightFilter(btn);
            applyFilters();
        });
        return btn;
    }

    private void highlightFilter(Button active) {
        String normal = "-fx-background-color: #e5e7eb; -fx-text-fill: #555; -fx-font-size: 11px; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 4 12;";
        String selected = "-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 11px; " +
                          "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 4 12;";
        btnAll.setStyle(normal);
        btnConfirme.setStyle(normal);
        btnAttente.setStyle(normal);
        btnAnnule.setStyle(normal);
        active.setStyle(selected);
    }

    // ==================== TABLE ====================

    private void buildTableHeader() {
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");

        Label hPatient = colLabel("Patient"); HBox.setHgrow(hPatient, Priority.ALWAYS);
        Label hMedecin = colLabel("Medecin"); HBox.setHgrow(hMedecin, Priority.ALWAYS);
        Label hSpec = colLabel("Specialite"); HBox.setHgrow(hSpec, Priority.ALWAYS);
        Label hDate = colLabel("Date"); hDate.setMinWidth(90);
        Label hHeure = colLabel("Heure"); hHeure.setMinWidth(55);
        Label hStatut = colLabel("Statut"); hStatut.setMinWidth(90);
        Label hAction = colLabel(""); hAction.setMinWidth(45);

        tableHeader.getChildren().addAll(hPatient, hMedecin, hSpec, hDate, hHeure, hStatut, hAction);
        container.getChildren().add(tableHeader);
    }

    private void applyFilters() {
        // Remove old rows
        if (container.getChildren().size() > rowStartIndex) {
            container.getChildren().remove(rowStartIndex, container.getChildren().size());
        }

        List<RendezVous> filtered = allRdvs.stream()
            .filter(rv -> currentFilter.equals("all") || rv.getStatut().equals(currentFilter))
            .filter(rv -> searchQuery.isEmpty() ||
                    rv.getPatientFullName().toLowerCase().contains(searchQuery) ||
                    rv.getMedecinFullName().toLowerCase().contains(searchQuery) ||
                    (rv.getSpecialite() != null && rv.getSpecialite().toLowerCase().contains(searchQuery)))
            .toList();

        addRows(filtered);
    }

    private void addRows(List<RendezVous> rdvs) {
        if (rdvs.isEmpty()) {
            Label empty = new Label("Aucun rendez-vous trouve.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < rdvs.size(); i++) {
            RendezVous rv = rdvs.get(i);
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            Label patient = new Label(rv.getPatientFullName());
            patient.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(patient, Priority.ALWAYS);
            patient.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-font-weight: bold;");

            Label medecin = new Label("Dr. " + rv.getMedecinFullName());
            medecin.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(medecin, Priority.ALWAYS);
            medecin.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

            Label spec = new Label(rv.getSpecialite() != null ? rv.getSpecialite() : "-");
            spec.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(spec, Priority.ALWAYS);
            spec.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

            Label date = new Label(rv.getDate().format(dateFmt));
            date.setMinWidth(90);
            date.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            Label heure = new Label(rv.getHeure().format(heureFmt));
            heure.setMinWidth(55);
            heure.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            String statutColor = switch (rv.getStatut()) {
                case "confirme" -> "#16a34a";
                case "annule" -> "#dc2626";
                default -> "#f59e0b";
            };
            Label statut = new Label(rv.getStatut());
            statut.setPrefWidth(90);
            statut.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 8; " +
                            "-fx-background-color: " + statutColor + "; -fx-background-radius: 12;");

            Button btnVoir = new Button();
            FontIcon eyeIcon = new FontIcon(FontAwesomeSolid.EYE);
            eyeIcon.setIconSize(13);
            eyeIcon.setIconColor(Color.web("#7c3aed"));
            btnVoir.setGraphic(eyeIcon);
            btnVoir.setStyle("-fx-background-color: #ede9fe; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 6;");
            btnVoir.setTooltip(new Tooltip("Voir details"));
            btnVoir.setOnAction(e -> showDetails(rv));

            row.getChildren().addAll(patient, medecin, spec, date, heure, statut, btnVoir);
            container.getChildren().add(row);
        }
    }

    private Label colLabel(String text) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        return l;
    }

    // ========== DETAILS MODAL ==========

    private void showDetails(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(FontAwesomeSolid.CALENDAR_CHECK);
        icon.setIconSize(40);
        icon.setIconColor(Color.web("#7c3aed"));

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        Label titleLbl = new Label("Details du rendez-vous");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Label details = new Label(
            "Patient :  " + rv.getPatientFullName() + "\n" +
            "Medecin :  Dr. " + rv.getMedecinFullName() + "\n" +
            "Specialite :  " + (rv.getSpecialite() != null ? rv.getSpecialite() : "-") + "\n" +
            "Date :  " + rv.getDate().format(dateFmt) + "\n" +
            "Heure :  " + rv.getHeure().format(heureFmt) + "\n" +
            "Statut :  " + rv.getStatut()
        );
        details.setStyle("-fx-font-size: 14px; -fx-text-fill: #444; -fx-line-spacing: 4;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; " +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        closeBtn.setOnAction(e -> popup.close());

        VBox box = new VBox(12, icon, titleLbl, details, closeBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 420, 360);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();
    }
}

