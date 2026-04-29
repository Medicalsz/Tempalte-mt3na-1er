package com.medicare.controllers;

import com.medicare.services.EvaluationService;
import com.medicare.services.EvaluationService.MedecinStats;
import com.medicare.models.Evaluation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Vue admin "Évaluations" : KPIs globaux + tableau des médecins triés par performance.
 */
public class AdminEvaluationsController {

    private final EvaluationService service = new EvaluationService();
    private List<MedecinStats> all;
    private VBox listContainer;
    private String searchQuery = "";

    public Node buildView() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(8));

        all = service.getAllMedecinStats();
        Evaluation.Stats global = service.getGlobalStats();

        root.getChildren().add(buildKpiRow(global));
        root.getChildren().add(buildToolbar());

        // Header colonnes
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 15, 10, 15));
        header.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");
        header.getChildren().addAll(
                colHeader("Médecin",      Priority.ALWAYS, 0),
                colHeader("Spécialité",   Priority.ALWAYS, 0),
                colHeader("Évaluations",  null,            120),
                colHeader("Note moyenne", null,            180)
        );
        root.getChildren().add(header);

        listContainer = new VBox();
        applyFilter();

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        return root;
    }

    // ============== KPIs ==============
    private HBox buildKpiRow(Evaluation.Stats global) {
        // Top médecin (par nb éval)
        MedecinStats topByCount = all.stream()
                .filter(m -> m.nbEvaluations > 0)
                .max(Comparator.comparingInt((MedecinStats m) -> m.nbEvaluations))
                .orElse(null);

        // Meilleure note (avec au moins 1 éval)
        MedecinStats topByNote = all.stream()
                .filter(m -> m.nbEvaluations > 0)
                .max(Comparator.comparingDouble((MedecinStats m) -> m.moyenne))
                .orElse(null);

        long medecinsEvalues = all.stream().filter(m -> m.nbEvaluations > 0).count();

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
                kpi("Évaluations", String.valueOf(global.total),
                        FontAwesomeSolid.STAR, "#f59e0b", "#fef3c7",
                        "Toutes les évaluations enregistrées."),
                kpi("Note moyenne", global.total > 0
                        ? String.format(Locale.FRENCH, "%.1f", global.moyenneGlobale) : "—",
                        FontAwesomeSolid.CHART_LINE, "#16a34a", "#dcfce7",
                        global.total > 0
                                ? String.format(Locale.FRENCH, "Sur %d évaluations", global.total)
                                : "Pas encore d'avis."),
                kpi("Médecins notés", medecinsEvalues + " / " + all.size(),
                        FontAwesomeSolid.USER_MD, "#0ea5e9", "#e0f2fe",
                        "Médecins ayant reçu au moins un avis."),
                kpi("Top évaluations",
                        topByCount != null ? "Dr. " + topByCount.prenom + " " + topByCount.nom : "—",
                        FontAwesomeSolid.AWARD, "#ec4899", "#fce7f3",
                        topByCount != null
                                ? topByCount.nbEvaluations + " avis  ·  " + String.format(Locale.FRENCH, "%.1f/5", topByCount.moyenne)
                                : "Aucun médecin évalué."),
                kpi("Mieux noté",
                        topByNote != null ? "Dr. " + topByNote.prenom + " " + topByNote.nom : "—",
                        FontAwesomeSolid.MEDAL, "#d97706", "#fef3c7",
                        topByNote != null
                                ? String.format(Locale.FRENCH, "%.1f/5  ·  %d avis", topByNote.moyenne, topByNote.nbEvaluations)
                                : "Aucun médecin évalué.")
        );
        return row;
    }

    private VBox kpi(String title, String value, FontAwesomeSolid icon, String color, String bg, String tooltip) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 10, 14, 10));
        String base  = "-fx-background-color: white; -fx-background-radius: 12; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);";
        String hover = "-fx-background-color: white; -fx-background-radius: 12; -fx-cursor: hand; " +
                       "-fx-effect: dropshadow(gaussian, " + color + "40, 14, 0, 0, 4); " +
                       "-fx-border-color: " + color + "; -fx-border-radius: 12; -fx-border-width: 1.5;";
        card.setStyle(base);
        card.setPickOnBounds(true);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        StackPane wrap = new StackPane();
        wrap.setMinSize(38, 38); wrap.setMaxSize(38, 38);
        wrap.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 19;");
        FontIcon ic = new FontIcon(icon);
        ic.setIconSize(16);
        ic.setIconColor(Color.web(color));
        wrap.getChildren().add(ic);

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        v.setWrapText(true);
        v.setMaxWidth(180);
        v.setAlignment(Pos.CENTER);
        v.setStyle(v.getStyle() + "-fx-text-alignment: center;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(wrap, v, t);

        Tooltip tp = new Tooltip(tooltip);
        tp.setShowDelay(javafx.util.Duration.millis(200));
        tp.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-size: 12px; " +
                    "-fx-background-radius: 8; -fx-padding: 10 14;");
        Tooltip.install(card, tp);

        card.setOnMouseEntered(e -> card.setStyle(hover));
        card.setOnMouseExited(e  -> card.setStyle(base));
        return card;
    }

    // ============== Toolbar ==============
    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Performance des médecins");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        TextField search = new TextField();
        search.setPromptText("Rechercher un médecin ou spécialité...");
        search.setPrefWidth(280);
        search.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #d1d5db; " +
                        "-fx-padding: 5 12; -fx-font-size: 12px;");
        search.textProperty().addListener((obs, o, n) -> {
            searchQuery = n == null ? "" : n.trim().toLowerCase();
            applyFilter();
        });

        bar.getChildren().addAll(title, sp, search);
        return bar;
    }

    // ============== Liste ==============
    private void applyFilter() {
        listContainer.getChildren().clear();
        List<MedecinStats> filtered = all.stream().filter(m -> {
            if (searchQuery.isEmpty()) return true;
            return (m.nom != null && m.nom.toLowerCase().contains(searchQuery))
                || (m.prenom != null && m.prenom.toLowerCase().contains(searchQuery))
                || (m.specialite != null && m.specialite.toLowerCase().contains(searchQuery));
        }).toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("Aucun médecin trouvé.");
            empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 20;");
            listContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < filtered.size(); i++) {
            listContainer.getChildren().add(buildRow(filtered.get(i), i));
        }
    }

    private HBox buildRow(MedecinStats m, int idx) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));
        String bg = idx % 2 == 0 ? "white" : "#f9fafb";
        row.setStyle("-fx-background-color: " + bg + ";");

        // Avatar + nom
        StackPane avatar = new StackPane();
        avatar.setMinSize(36, 36); avatar.setMaxSize(36, 36);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #7c3aed); " +
                        "-fx-background-radius: 50;");
        String ini = "" + (m.prenom != null && !m.prenom.isEmpty() ? m.prenom.charAt(0) : '?')
                + (m.nom != null && !m.nom.isEmpty() ? m.nom.charAt(0) : ' ');
        Label iniLbl = new Label(ini.toUpperCase());
        iniLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        avatar.getChildren().add(iniLbl);

        Label nameLbl = new Label("Dr. " + m.prenom + " " + m.nom);
        nameLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        HBox nameBox = new HBox(10, avatar, nameLbl);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        nameBox.setMaxWidth(Double.MAX_VALUE);

        // Spécialité
        Label spec = new Label(m.specialite != null ? m.specialite : "-");
        spec.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        HBox.setHgrow(spec, Priority.ALWAYS);
        spec.setMaxWidth(Double.MAX_VALUE);

        // Nombre éval (badge)
        Label nb = new Label(String.valueOf(m.nbEvaluations));
        String badgeColor = m.nbEvaluations == 0 ? "#94a3b8" : (m.nbEvaluations >= 5 ? "#16a34a" : "#0ea5e9");
        nb.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white; " +
                    "-fx-background-color: " + badgeColor + "; -fx-background-radius: 10; " +
                    "-fx-padding: 3 12;");
        HBox nbBox = new HBox(nb);
        nbBox.setAlignment(Pos.CENTER_LEFT);
        nbBox.setMinWidth(120);

        // Note moyenne (étoiles + valeur)
        HBox noteBox = new HBox(6);
        noteBox.setAlignment(Pos.CENTER_LEFT);
        noteBox.setMinWidth(180);
        if (m.nbEvaluations == 0) {
            Label none = new Label("Pas d'avis");
            none.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
            noteBox.getChildren().add(none);
        } else {
            HBox stars = new HBox(2);
            int rounded = (int) Math.round(m.moyenne);
            for (int i = 0; i < 5; i++) {
                FontIcon st = new FontIcon(FontAwesomeSolid.STAR);
                st.setIconSize(12);
                st.setIconColor(Color.web(i < rounded ? "#f59e0b" : "#e2e8f0"));
                stars.getChildren().add(st);
            }
            Label noteLbl = new Label(String.format(Locale.FRENCH, "%.1f/5", m.moyenne));
            noteLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            noteBox.getChildren().addAll(stars, noteLbl);
        }

        row.getChildren().addAll(nameBox, spec, nbBox, noteBox);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f3f0ff; -fx-cursor: hand;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color: " + bg + ";"));

        // Tooltip ligne
        String tooltipText = "Dr. " + m.prenom + " " + m.nom
                + "\n────────────────────"
                + "\nSpécialité    : " + (m.specialite != null ? m.specialite : "—")
                + "\nÉvaluations   : " + m.nbEvaluations
                + (m.nbEvaluations > 0
                        ? "\nNote moyenne : " + String.format(Locale.FRENCH, "%.2f / 5", m.moyenne)
                        : "");
        Tooltip tp = new Tooltip(tooltipText);
        tp.setShowDelay(javafx.util.Duration.millis(300));
        tp.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-size: 12px; " +
                    "-fx-background-radius: 8; -fx-padding: 10 14;");
        Tooltip.install(row, tp);

        return row;
    }

    private Label colHeader(String text, Priority hgrow, int minWidth) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        l.setMaxWidth(Double.MAX_VALUE);
        if (hgrow != null) HBox.setHgrow(l, hgrow);
        if (minWidth > 0) l.setMinWidth(minWidth);
        return l;
    }
}

