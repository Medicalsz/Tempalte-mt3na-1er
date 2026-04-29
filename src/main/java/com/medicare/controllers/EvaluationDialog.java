package com.medicare.controllers;

import com.medicare.models.Evaluation;
import com.medicare.models.RendezVous;
import com.medicare.models.User;
import com.medicare.services.EmailService;
import com.medicare.services.EvaluationService;
import com.medicare.services.UserService;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;

/**
 * Modal d'évaluation d'une consultation par le patient.
 * - Étoiles cliquables (note globale + 3 critères)
 * - Commentaire libre
 * - Envoi de 2 emails (patient = remerciement, médecin = notification)
 * - Popup succès animé
 */
public class EvaluationDialog {

    private static final String COLOR_GOLD       = "#f59e0b";
    private static final String COLOR_GOLD_HOVER = "#fbbf24";
    private static final String COLOR_EMPTY      = "#e2e8f0";

    public static void show(RendezVous rv, int patientId, Window owner, Runnable onSaved) {
        EvaluationService service     = new EvaluationService();
        EmailService      emailService = new EmailService();
        UserService       userService  = new UserService();

        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) popup.initOwner(owner);

        // ====== Header ======
        StackPane iconWrap = new StackPane();
        iconWrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #fde68a, #f59e0b); " +
                "-fx-background-radius: 50; -fx-pref-width: 70; -fx-pref-height: 70; " +
                "-fx-min-width: 70; -fx-min-height: 70; -fx-max-width: 70; -fx-max-height: 70;");
        FontIcon starIcon = new FontIcon(FontAwesomeSolid.STAR);
        starIcon.setIconSize(34);
        starIcon.setIconColor(Color.WHITE);
        iconWrap.getChildren().add(starIcon);

        Label title = new Label("Évaluez votre consultation");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Label sub = new Label("Dr. " + rv.getMedecinFullName()
                + (rv.getSpecialite() != null ? " · " + rv.getSpecialite() : "")
                + " · " + rv.getDate().format(df));
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // ====== Note globale (5 grandes étoiles) ======
        int[] noteGlobale = {0};
        StarRow globalStars = new StarRow(36, noteGlobale);
        Label globalLbl = new Label("Note globale");
        globalLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        VBox globalBox = new VBox(8, globalLbl, globalStars.node);
        globalBox.setAlignment(Pos.CENTER);

        // ====== 3 critères ======
        int[] notePonct = {0}, noteEcoute = {0}, noteClarte = {0};
        HBox critPonct  = buildCriteriaRow("Ponctualité",          FontAwesomeSolid.CLOCK,     notePonct);
        HBox critEcoute = buildCriteriaRow("Écoute & empathie",    FontAwesomeSolid.HEART,     noteEcoute);
        HBox critClarte = buildCriteriaRow("Clarté du diagnostic", FontAwesomeSolid.LIGHTBULB, noteClarte);

        VBox criteres = new VBox(10, critPonct, critEcoute, critClarte);
        criteres.setPadding(new Insets(6, 0, 0, 0));

        // ====== Commentaire ======
        HBox commentHeader = new HBox(8);
        commentHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon comIcon = new FontIcon(FontAwesomeSolid.COMMENT_DOTS);
        comIcon.setIconSize(14);
        comIcon.setIconColor(Color.web("#3b82f6"));
        Label commentLbl = new Label("Votre commentaire (optionnel)");
        commentLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        commentHeader.getChildren().addAll(comIcon, commentLbl);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Partagez votre expérience pour aider d'autres patients...");
        commentArea.setPrefRowCount(3);
        commentArea.setWrapText(true);
        commentArea.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; " +
                "-fx-border-color: #e2e8f0; -fx-font-size: 12px; -fx-control-inner-background: #f8fafc;");

        // ====== Boutons ======
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 22;");

        Button btnSave = new Button("  Envoyer mon avis");
        FontIcon sendIcon = new FontIcon(FontAwesomeSolid.PAPER_PLANE);
        sendIcon.setIconSize(14);
        sendIcon.setIconColor(Color.WHITE);
        btnSave.setGraphic(sendIcon);
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #d97706); -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; " +
                "-fx-padding: 9 22; -fx-effect: dropshadow(gaussian, rgba(245,158,11,0.4), 12, 0, 0, 4);");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox actions = new HBox(10, btnCancel, sp, btnSave);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(8, 0, 0, 0));

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
        errLbl.setVisible(false);

        VBox box = new VBox(14, iconWrap, title, sub, globalBox, criteres,
                commentHeader, commentArea, errLbl, actions);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        box.setMaxWidth(480);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.35), 35, 0, 0, 10);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPadding(new Insets(20));

        Scene scene = new Scene(overlay, 540, 720);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);

        if (owner != null && owner.getScene() != null) {
            GaussianBlur blur = new GaussianBlur(12);
            owner.getScene().getRoot().setEffect(blur);
            popup.setOnHidden(ev -> owner.getScene().getRoot().setEffect(null));
        }

        btnCancel.setOnAction(e -> popup.close());
        btnSave.setOnAction(e -> {
            if (noteGlobale[0] == 0 || notePonct[0] == 0 || noteEcoute[0] == 0 || noteClarte[0] == 0) {
                errLbl.setText("⚠ Merci de noter tous les critères avant d'envoyer.");
                errLbl.setVisible(true);
                return;
            }
            Evaluation ev = new Evaluation();
            ev.setRendezVousId(rv.getId());
            ev.setPatientId(patientId);
            ev.setMedecinId(rv.getMedecinId());
            ev.setNote(noteGlobale[0]);
            ev.setNotePonctualite(notePonct[0]);
            ev.setNoteEcoute(noteEcoute[0]);
            ev.setNoteClarte(noteClarte[0]);
            ev.setCommentaire(commentArea.getText());

            int newId = service.create(ev);
            if (newId > 0) {
                // ===== Envoi des emails (le service envoie déjà en thread asynchrone) =====
                try {
                    User patient = userService.getUserByPatientId(patientId);
                    User medecin = userService.getUserByMedecinId(rv.getMedecinId());
                    String nomMedecinFull = medecin != null
                            ? medecin.getPrenom() + " " + medecin.getNom()
                            : rv.getMedecinFullName();

                    if (patient != null && patient.getEmail() != null && !patient.getEmail().isBlank()) {
                        emailService.envoyerRemerciementEvaluation(
                                patient.getEmail(),
                                patient.getPrenom() + " " + patient.getNom(),
                                nomMedecinFull,
                                ev.getNote()
                        );
                    }
                    if (medecin != null && medecin.getEmail() != null && !medecin.getEmail().isBlank()) {
                        String nomPatientFull = patient != null
                                ? patient.getPrenom() + " " + patient.getNom()
                                : "Patient";
                        emailService.envoyerNotificationEvaluation(
                                medecin.getEmail(),
                                medecin.getPrenom() + " " + medecin.getNom(),
                                nomPatientFull,
                                ev.getNote(),
                                ev.getCommentaire()
                        );
                    }
                } catch (Exception ex) {
                    System.err.println("Erreur envoi email évaluation : " + ex.getMessage());
                }

                popup.close();
                showSuccessPopup(owner, ev.getNote(), () -> {
                    if (onSaved != null) onSaved.run();
                });
            } else {
                errLbl.setText("Erreur lors de l'enregistrement (déjà évalué ?).");
                errLbl.setVisible(true);
            }
        });

        popup.show();
    }

    /** Ligne d'un critère : icône + label + 5 mini étoiles cliquables. */
    private static HBox buildCriteriaRow(String label, FontAwesomeSolid icon, int[] noteHolder) {
        FontIcon ic = new FontIcon(icon);
        ic.setIconSize(14);
        ic.setIconColor(Color.web("#0ea5e9"));

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        lbl.setMinWidth(170);

        StarRow stars = new StarRow(18, noteHolder);

        HBox row = new HBox(10, ic, lbl, stars.node);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10;");
        return row;
    }

    /** Composant ligne de 5 étoiles cliquables. Chaque étoile est wrappée dans un StackPane
     *  pour rendre les évènements souris fiables (le FontIcon seul peut perdre des hovers). */
    private static class StarRow {
        final HBox node;
        final FontIcon[] icons = new FontIcon[5];

        StarRow(int size, int[] holder) {
            node = new HBox(6);
            node.setAlignment(Pos.CENTER);
            for (int i = 0; i < 5; i++) {
                final int idx = i + 1;
                FontIcon st = new FontIcon(FontAwesomeSolid.STAR);
                st.setIconSize(size);
                st.setIconColor(Color.web(COLOR_EMPTY));
                icons[i] = st;

                StackPane cell = new StackPane(st);
                int padH = Math.max(4, size / 6);
                cell.setPadding(new Insets(padH / 2.0, padH, padH / 2.0, padH));
                cell.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
                cell.setOnMouseEntered(e -> paint(idx, COLOR_GOLD_HOVER));
                cell.setOnMouseExited(e  -> paint(holder[0], COLOR_GOLD));
                cell.setOnMouseClicked(e -> {
                    holder[0] = idx;
                    paint(idx, COLOR_GOLD);
                });
                node.getChildren().add(cell);
            }
        }

        void paint(int upTo, String activeColor) {
            for (int i = 0; i < icons.length; i++) {
                icons[i].setIconColor(Color.web(i < upTo ? activeColor : COLOR_EMPTY));
            }
        }
    }

    // ============== POPUP SUCCÈS ==============
    private static void showSuccessPopup(Window owner, int note, Runnable onClose) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) popup.initOwner(owner);

        StackPane circle = new StackPane();
        circle.setStyle("-fx-background-color: linear-gradient(to bottom right, #fde68a, #f59e0b); " +
                "-fx-background-radius: 50; -fx-pref-width: 80; -fx-pref-height: 80; " +
                "-fx-min-width: 80; -fx-min-height: 80; -fx-max-width: 80; -fx-max-height: 80;");
        FontIcon check = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
        check.setIconSize(42);
        check.setIconColor(Color.WHITE);
        circle.getChildren().add(check);

        Label title = new Label("Merci pour votre avis !");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Étoiles récap (animation visuelle)
        HBox stars = new HBox(4);
        stars.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            FontIcon s = new FontIcon(FontAwesomeSolid.STAR);
            s.setIconSize(20);
            s.setIconColor(Color.web(i < note ? COLOR_GOLD : COLOR_EMPTY));
            stars.getChildren().add(s);
        }

        Label msg = new Label("Votre évaluation a bien été envoyée.\nUn email de confirmation vous a été envoyé.");
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        msg.setWrapText(true);

        javafx.scene.shape.Rectangle progress = new javafx.scene.shape.Rectangle(220, 4);
        progress.setFill(Color.web(COLOR_GOLD));
        progress.setArcWidth(4);
        progress.setArcHeight(4);

        VBox box = new VBox(14, circle, title, stars, msg, progress);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32, 36, 28, 36));
        box.setMaxWidth(420);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.35), 35, 0, 0, 10);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 460, 360);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO,
                        new javafx.animation.KeyValue(progress.widthProperty(), 220)),
                new javafx.animation.KeyFrame(Duration.seconds(2.2),
                        new javafx.animation.KeyValue(progress.widthProperty(), 0))
        );
        timeline.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(2.2));
        pause.setOnFinished(e -> {
            popup.close();
            if (onClose != null) onClose.run();
        });
        pause.play();
    }
}
