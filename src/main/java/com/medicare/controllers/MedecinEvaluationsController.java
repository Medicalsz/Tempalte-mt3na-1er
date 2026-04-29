package com.medicare.controllers;

import com.medicare.models.Evaluation;
import com.medicare.services.EvaluationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Vue "Mes évaluations" du médecin.
 * Construite programmatiquement pour rester self-contained (pas de FXML).
 */
public class MedecinEvaluationsController {

    private final EvaluationService service = new EvaluationService();
    private final int medecinId;

    private List<Evaluation> all;
    private String currentFilter = "all";
    private Button btnAll, btn5, btn4, btnComment;
    private VBox listContainer;

    public MedecinEvaluationsController(int medecinId) {
        this.medecinId = medecinId;
    }

    /** Construit et retourne la vue. */
    public Node buildView() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(8));

        Evaluation.Stats stats = service.getStats(medecinId);
        all = service.getByMedecin(medecinId);

        root.getChildren().add(buildHeaderBanner(stats));
        root.getChildren().add(buildCriteriaCards(stats));
        root.getChildren().add(buildFilters());

        listContainer = new VBox(12);
        applyFilter();

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        return root;
    }

    // ============== HEADER ==============
    private Node buildHeaderBanner(Evaluation.Stats s) {
        HBox banner = new HBox(20);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(22, 26, 22, 26));
        banner.setStyle("-fx-background-color: linear-gradient(to right, #0f172a, #1e293b); " +
                "-fx-background-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.25), 18, 0, 0, 6);");

        StackPane iconWrap = new StackPane();
        iconWrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #fbbf24, #f59e0b); " +
                "-fx-background-radius: 50; -fx-pref-width: 64; -fx-pref-height: 64;");
        FontIcon star = new FontIcon(FontAwesomeSolid.STAR);
        star.setIconSize(28);
        star.setIconColor(Color.WHITE);
        iconWrap.getChildren().add(star);

        VBox left = new VBox(4);
        Label title = new Label("Mes évaluations");
        title.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        Label note = new Label(s.total == 0 ? "—" : String.format(Locale.FRENCH, "%.1f / 5", s.moyenneGlobale));
        note.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox starsRow = new HBox(3);
        for (int i = 0; i < 5; i++) {
            FontIcon st = new FontIcon(FontAwesomeSolid.STAR);
            st.setIconSize(16);
            boolean filled = i < Math.round((float) s.moyenneGlobale);
            st.setIconColor(Color.web(filled ? "#fbbf24" : "#475569"));
            starsRow.getChildren().add(st);
        }
        Label count = new Label(s.total == 0 ? "Aucun avis pour le moment"
                : (s.total + " avis"));
        count.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        HBox bottom = new HBox(10, starsRow, count);
        bottom.setAlignment(Pos.CENTER_LEFT);

        left.getChildren().addAll(title, note, bottom);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        banner.getChildren().addAll(iconWrap, left, sp);
        return banner;
    }

    // ============== 3 CARTES CRITÈRES ==============
    private Node buildCriteriaCards(Evaluation.Stats s) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
                criteriaCard("Ponctualité", s.moyennePonctualite, s.total, FontAwesomeSolid.CLOCK, "#0ea5e9"),
                criteriaCard("Écoute & empathie", s.moyenneEcoute, s.total, FontAwesomeSolid.HEART, "#ec4899"),
                criteriaCard("Clarté du diagnostic", s.moyenneClarte, s.total, FontAwesomeSolid.LIGHTBULB, "#22c55e")
        );
        return row;
    }

    private Node criteriaCard(String label, double avg, int total, FontAwesomeSolid icon, String color) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 12, 0, 0, 3);");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        StackPane iconWrap = new StackPane();
        iconWrap.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 10; " +
                "-fx-pref-width: 38; -fx-pref-height: 38;");
        FontIcon ic = new FontIcon(icon);
        ic.setIconSize(18);
        ic.setIconColor(Color.web(color));
        iconWrap.getChildren().add(ic);

        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Label val = new Label(total == 0 ? "—" : String.format(Locale.FRENCH, "%.1f", avg));
        val.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        HBox stars = new HBox(2);
        for (int i = 0; i < 5; i++) {
            FontIcon st = new FontIcon(FontAwesomeSolid.STAR);
            st.setIconSize(11);
            boolean filled = i < Math.round((float) avg);
            st.setIconColor(Color.web(filled ? color : "#e2e8f0"));
            stars.getChildren().add(st);
        }

        card.getChildren().addAll(iconWrap, l, val, stars);
        return card;
    }

    // ============== FILTRES ==============
    private Node buildFilters() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        btnAll = filterBtn("Toutes", "all");
        btn5 = filterBtn("★ 5 étoiles", "5");
        btn4 = filterBtn("★ 4 étoiles", "4");
        btnComment = filterBtn("Avec commentaire", "comment");
        highlight(btnAll);
        row.getChildren().addAll(btnAll, btn5, btn4, btnComment);
        return row;
    }

    private Button filterBtn(String text, String key) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #475569; -fx-font-size: 12px; " +
                "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-weight: bold;");
        b.setOnAction(e -> {
            currentFilter = key;
            highlight(b);
            applyFilter();
        });
        return b;
    }

    private void highlight(Button active) {
        String normal = "-fx-background-color: #e5e7eb; -fx-text-fill: #475569; -fx-font-size: 12px; " +
                "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-weight: bold;";
        String act = "-fx-background-color: linear-gradient(to right, #0f766e, #0d9488); -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-weight: bold;";
        for (Button b : new Button[]{btnAll, btn5, btn4, btnComment}) b.setStyle(normal);
        active.setStyle(act);
    }

    private void applyFilter() {
        listContainer.getChildren().clear();
        List<Evaluation> filtered = all.stream().filter(e -> switch (currentFilter) {
            case "5" -> e.getNote() == 5;
            case "4" -> e.getNote() == 4;
            case "comment" -> e.getCommentaire() != null && !e.getCommentaire().isBlank();
            default -> true;
        }).toList();

        if (filtered.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            FontIcon ic = new FontIcon(FontAwesomeSolid.INBOX);
            ic.setIconSize(36);
            ic.setIconColor(Color.web("#94a3b8"));
            Label l = new Label("Aucun avis pour ce filtre.");
            l.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            empty.getChildren().addAll(ic, l);
            listContainer.getChildren().add(empty);
            return;
        }

        for (Evaluation e : filtered) listContainer.getChildren().add(buildReviewCard(e));
    }

    private Node buildReviewCard(Evaluation e) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 10, 0, 0, 2);");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        // Avatar avec initiales
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #0d9488, #0f766e); " +
                "-fx-background-radius: 50; -fx-pref-width: 42; -fx-pref-height: 42;");
        String initials = "" + (e.getPatientPrenom() != null && !e.getPatientPrenom().isEmpty() ? e.getPatientPrenom().charAt(0) : '?')
                + (e.getPatientNom() != null && !e.getPatientNom().isEmpty() ? e.getPatientNom().charAt(0) : ' ');
        Label ini = new Label(initials.toUpperCase());
        ini.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatar.getChildren().add(ini);

        VBox info = new VBox(2);
        Label name = new Label(e.getPatientFullName().trim());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        HBox starsRow = new HBox(3);
        for (int i = 0; i < 5; i++) {
            FontIcon s = new FontIcon(FontAwesomeSolid.STAR);
            s.setIconSize(12);
            s.setIconColor(Color.web(i < e.getNote() ? "#f59e0b" : "#e2e8f0"));
            starsRow.getChildren().add(s);
        }
        info.getChildren().addAll(name, starsRow);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label date = new Label();
        if (e.getCreatedAt() != null) {
            date.setText(e.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH)));
        }
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        top.getChildren().addAll(avatar, info, sp, date);
        card.getChildren().add(top);

        // Mini critères
        HBox crits = new HBox(14);
        crits.setPadding(new Insets(4, 0, 0, 54));
        crits.getChildren().addAll(
                miniCrit(FontAwesomeSolid.CLOCK, "Ponctualité", e.getNotePonctualite(), "#0ea5e9"),
                miniCrit(FontAwesomeSolid.HEART, "Écoute", e.getNoteEcoute(), "#ec4899"),
                miniCrit(FontAwesomeSolid.LIGHTBULB, "Clarté", e.getNoteClarte(), "#22c55e")
        );
        card.getChildren().add(crits);

        if (e.getCommentaire() != null && !e.getCommentaire().isBlank()) {
            Label com = new Label("« " + e.getCommentaire() + " »");
            com.setWrapText(true);
            com.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-font-style: italic; " +
                    "-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-padding: 10 12;");
            VBox.setMargin(com, new Insets(4, 0, 0, 54));
            card.getChildren().add(com);
        }

        return card;
    }

    private Node miniCrit(FontAwesomeSolid icon, String label, int note, String color) {
        FontIcon ic = new FontIcon(icon);
        ic.setIconSize(11);
        ic.setIconColor(Color.web(color));
        Label l = new Label(label + " " + note + "/5");
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        HBox h = new HBox(4, ic, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}

