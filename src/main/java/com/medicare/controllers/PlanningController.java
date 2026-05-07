package com.medicare.controllers;

import com.medicare.models.Disponibilite;
import com.medicare.services.RendezVousService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlanningController {

    @FXML private VBox container;

    private final RendezVousService service = new RendezVousService();
    private int medecinId;
    private StackPane contentArea;

    // Stocke les composants par jour pour la sauvegarde
    private final Map<String, DayRow> dayRows = new LinkedHashMap<>();

    private static final String[] JOURS = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
    private static final String[] JOURS_ICONS_COLORS = {"#3b82f6", "#8b5cf6", "#ec4899", "#f59e0b", "#10b981", "#06b6d4", "#ef4444"};
    private static final FontAwesomeSolid[] JOURS_ICONS = {
        FontAwesomeSolid.BRIEFCASE, FontAwesomeSolid.BRIEFCASE, FontAwesomeSolid.BRIEFCASE,
        FontAwesomeSolid.BRIEFCASE, FontAwesomeSolid.BRIEFCASE, FontAwesomeSolid.STAR, FontAwesomeSolid.HOME
    };

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }

    public void setMedecinId(int medecinId) {
        this.medecinId = medecinId;
        buildUI();
    }

    private void buildUI() {
        container.getChildren().clear();
        container.setSpacing(16);

        // Titre
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon clockIcon = new FontIcon(FontAwesomeSolid.CALENDAR_WEEK);
        clockIcon.setIconSize(26);
        clockIcon.setIconColor(Color.web("#0d9488"));

        Label title = new Label("Mon Planning Hebdomadaire");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSave = new Button("  Enregistrer");
        FontIcon saveIcon = new FontIcon(FontAwesomeSolid.SAVE);
        saveIcon.setIconSize(15);
        saveIcon.setIconColor(Color.WHITE);
        btnSave.setGraphic(saveIcon);
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #0d9488, #14b8a6); -fx-text-fill: white; " +
                         "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 10 24; " +
                         "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.3), 10, 0, 0, 3);");
        btnSave.setOnAction(e -> saveAll());

        titleRow.getChildren().addAll(clockIcon, title, spacer, btnSave);
        container.getChildren().add(titleRow);

        // Sous-titre
        Label subtitle = new Label("Configurez vos horaires pour chaque jour de la semaine");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        container.getChildren().add(subtitle);

        // Légende
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(5, 0, 5, 0));
        legend.getChildren().addAll(
            legendItem(FontAwesomeSolid.SUN, "#f59e0b", "Matin"),
            legendItem(FontAwesomeSolid.COFFEE, "#8b5cf6", "Pause"),
            legendItem(FontAwesomeSolid.CLOUD_SUN, "#3b82f6", "Après-midi"),
            legendItem(FontAwesomeSolid.BAN, "#ef4444", "Fermé")
        );
        container.getChildren().add(legend);

        // Charger les dispos existantes
        List<Disponibilite> dispos = service.getAllDisponibilites(medecinId);

        for (int i = 0; i < JOURS.length; i++) {
            Disponibilite d = dispos.get(i);
            DayRow row = buildDayCard(d, JOURS_ICONS_COLORS[i], JOURS_ICONS[i]);
            dayRows.put(JOURS[i], row);
            container.getChildren().add(row.card);
        }
    }

    private HBox legendItem(FontAwesomeSolid iconType, String color, String text) {
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(13);
        icon.setIconColor(Color.web(color));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        HBox box = new HBox(5, icon, lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private DayRow buildDayCard(Disponibilite d, String accentColor, FontAwesomeSolid dayIcon) {
        DayRow row = new DayRow();

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        // Header du jour
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setStyle("-fx-background-color: " + accentColor + "10; -fx-background-radius: 12 12 0 0;");

        // Cercle icône
        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(36, 36);
        iconCircle.setMaxSize(36, 36);
        iconCircle.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 18;");
        FontIcon icon = new FontIcon(dayIcon);
        icon.setIconSize(16);
        icon.setIconColor(Color.WHITE);
        iconCircle.getChildren().add(icon);

        Label dayLabel = new Label(d.getJourSemaine());
        dayLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        // Toggle fermé/ouvert
        CheckBox fermeCheck = new CheckBox("Fermé");
        fermeCheck.setSelected(d.isFerme());
        fermeCheck.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444; -fx-cursor: hand;");
        row.fermeCheck = fermeCheck;

        header.getChildren().addAll(iconCircle, dayLabel, hSpacer, fermeCheck);

        // Contenu horaires
        VBox content = new VBox(10);
        content.setPadding(new Insets(14, 18, 14, 18));

        // Matin
        row.matinDebut = createTimeCombo();
        row.matinFin = createTimeCombo();
        if (d.getMatinDebut() != null) row.matinDebut.setValue(d.getMatinDebut().format(TIME_FMT));
        if (d.getMatinFin() != null) row.matinFin.setValue(d.getMatinFin().format(TIME_FMT));
        HBox matinRow = createTimeRow(FontAwesomeSolid.SUN, "#f59e0b", "Matin", row.matinDebut, row.matinFin);

        // Pause
        row.pauseDebut = createTimeCombo();
        row.pauseFin = createTimeCombo();
        if (d.getPauseDebut() != null) row.pauseDebut.setValue(d.getPauseDebut().format(TIME_FMT));
        if (d.getPauseFin() != null) row.pauseFin.setValue(d.getPauseFin().format(TIME_FMT));
        HBox pauseRow = createTimeRow(FontAwesomeSolid.COFFEE, "#8b5cf6", "Pause", row.pauseDebut, row.pauseFin);

        // Après-midi
        row.amDebut = createTimeCombo();
        row.amFin = createTimeCombo();
        if (d.getApresMidiDebut() != null) row.amDebut.setValue(d.getApresMidiDebut().format(TIME_FMT));
        if (d.getApresMidiFin() != null) row.amFin.setValue(d.getApresMidiFin().format(TIME_FMT));
        HBox amRow = createTimeRow(FontAwesomeSolid.CLOUD_SUN, "#3b82f6", "Après-midi", row.amDebut, row.amFin);

        content.getChildren().addAll(matinRow, pauseRow, amRow);

        // Gérer l'état fermé
        fermeCheck.selectedProperty().addListener((obs, old, val) -> {
            content.setDisable(val);
            content.setOpacity(val ? 0.35 : 1.0);
        });
        if (d.isFerme()) {
            content.setDisable(true);
            content.setOpacity(0.35);
        }

        card.getChildren().addAll(header, content);
        row.card = card;
        return row;
    }

    private HBox createTimeRow(FontAwesomeSolid iconType, String color, String label,
                                ComboBox<String> startCombo, ComboBox<String> endCombo) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(14);
        icon.setIconColor(Color.web(color));

        Label lbl = new Label(label);
        lbl.setMinWidth(75);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        Label arrow = new Label("→");
        arrow.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");


        row.getChildren().addAll(icon, lbl, startCombo, arrow, endCombo);
        return row;
    }

    private ComboBox<String> createTimeCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setPrefWidth(100);
        combo.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 6; " +
                       "-fx-background-radius: 6; -fx-font-size: 12px;");
        // Générer les heures de 06:00 à 22:00 par pas de 30 min
        for (int h = 6; h <= 22; h++) {
            combo.getItems().add(String.format("%02d:00", h));
            if (h < 22) combo.getItems().add(String.format("%02d:30", h));
        }
        combo.setEditable(false);
        return combo;
    }

    private void saveAll() {
        for (Map.Entry<String, DayRow> entry : dayRows.entrySet()) {
            DayRow row = entry.getValue();
            Disponibilite d = new Disponibilite();
            d.setMedecinId(medecinId);
            d.setJourSemaine(entry.getKey());
            d.setFerme(row.fermeCheck.isSelected());

            if (!row.fermeCheck.isSelected()) {
                d.setMatinDebut(parseTime(row.matinDebut));
                d.setMatinFin(parseTime(row.matinFin));
                d.setPauseDebut(parseTime(row.pauseDebut));
                d.setPauseFin(parseTime(row.pauseFin));
                d.setApresMidiDebut(parseTime(row.amDebut));
                d.setApresMidiFin(parseTime(row.amFin));
            }

            service.saveOrUpdateDisponibilite(d);
        }

        showSuccessToast();
    }

    private LocalTime parseTime(ComboBox<String> combo) {
        if (combo == null || combo.getValue() == null || combo.getValue().isEmpty()) return null;
        return LocalTime.parse(combo.getValue(), TIME_FMT);
    }

    private void showSuccessToast() {
        if (contentArea == null) return;

        // Icône dans un cercle
        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 50; " +
                            "-fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon checkIcon = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
        checkIcon.setIconSize(34);
        checkIcon.setIconColor(Color.web("#16a34a"));
        iconCircle.getChildren().add(checkIcon);

        Label titleLbl = new Label("Planning enregistre !");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label msgLbl = new Label("Vos horaires ont ete mis a jour avec succes.");
        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        msgLbl.setWrapText(true);

        // Barre de progression animée
        javafx.scene.shape.Rectangle progressBar = new javafx.scene.shape.Rectangle(200, 4);
        progressBar.setFill(Color.web("#16a34a"));
        progressBar.setArcWidth(4);
        progressBar.setArcHeight(4);

        VBox box = new VBox(18, iconCircle, titleLbl, msgLbl, progressBar);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(35));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");
        box.setMaxWidth(380);
        box.setMaxHeight(280);

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        contentArea.getChildren().add(overlay);

        // Animation de la barre
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                new javafx.animation.KeyValue(progressBar.widthProperty(), 200)),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2),
                new javafx.animation.KeyValue(progressBar.widthProperty(), 0))
        );
        timeline.play();

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> contentArea.getChildren().remove(overlay));
        pause.play();
    }

    // Classe interne pour stocker les composants d'un jour
    private static class DayRow {
        VBox card;
        CheckBox fermeCheck;
        ComboBox<String> matinDebut, matinFin;
        ComboBox<String> pauseDebut, pauseFin;
        ComboBox<String> amDebut, amFin;
    }
}

