package com.medicare.utils;

import com.medicare.models.Medecin;
import com.medicare.models.RendezVous;
import com.medicare.models.Specialite;
import com.medicare.services.ChatBotService;
import com.medicare.services.ChatBotService.BotResponse;
import com.medicare.services.ChatBotService.Urgency;
import com.medicare.services.RendezVousService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Widget chatbot flottant : icone bleue en bas a droite + fenetre de chat modale.
 * La callback {@link #onPrendreRdv} est appelee avec la specialite choisie
 * quand le patient clique sur "Prendre rendez-vous".
 */
public class ChatBotWidget {

    private static final int INPUT_MAX_LENGTH = 300;
    private static final int JOURS_RECHERCHE = 14;

    private final StackPane host;
    private final int patientId;
    private final Runnable onBookingSuccess;
    private final RendezVousService service = new RendezVousService();
    private Button floatingButton;
    private Stage chatStage;

    public ChatBotWidget(StackPane host, int patientId, Runnable onBookingSuccess) {
        this.host = host;
        this.patientId = patientId;
        this.onBookingSuccess = onBookingSuccess;
    }

    /** Attache (ou reattache) le bouton flottant au host. */
    public static void attachTo(StackPane host, int patientId, Runnable onBookingSuccess) {
        ChatBotWidget w = new ChatBotWidget(host, patientId, onBookingSuccess);
        w.mount();
    }

    private void mount() {
        // Eviter les doublons
        host.getChildren().removeIf(n -> "chatbot-floating-btn".equals(n.getId()));

        floatingButton = new Button();
        floatingButton.setId("chatbot-floating-btn");
        FontIcon icon = new FontIcon(FontAwesomeSolid.COMMENT_MEDICAL);
        icon.setIconSize(26);
        icon.setIconColor(Color.WHITE);
        floatingButton.setGraphic(icon);
        floatingButton.setStyle(baseFloatingStyle());
        floatingButton.setOnMouseEntered(e -> floatingButton.setStyle(hoverFloatingStyle()));
        floatingButton.setOnMouseExited(e -> floatingButton.setStyle(baseFloatingStyle()));
        floatingButton.setOnAction(e -> openChat());

        StackPane.setAlignment(floatingButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(floatingButton, new Insets(0, 25, 25, 0));
        host.getChildren().add(floatingButton);
    }

    private String baseFloatingStyle() {
        return "-fx-background-color: linear-gradient(to bottom right, #2563eb, #1d4ed8); " +
                "-fx-background-radius: 50; -fx-cursor: hand; " +
                "-fx-min-width: 58; -fx-min-height: 58; -fx-max-width: 58; -fx-max-height: 58; " +
                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.55), 18, 0, 0, 6);";
    }

    private String hoverFloatingStyle() {
        return "-fx-background-color: linear-gradient(to bottom right, #1d4ed8, #1e40af); " +
                "-fx-background-radius: 50; -fx-cursor: hand; " +
                "-fx-min-width: 62; -fx-min-height: 62; -fx-max-width: 62; -fx-max-height: 62; " +
                "-fx-effect: dropshadow(gaussian, rgba(29,78,216,0.7), 22, 0, 0, 8);";
    }

    // ========== CHAT WINDOW ==========

    private void openChat() {
        if (chatStage != null && chatStage.isShowing()) { chatStage.toFront(); return; }

        chatStage = new Stage();
        chatStage.initStyle(StageStyle.TRANSPARENT);
        chatStage.initModality(Modality.NONE);
        chatStage.initOwner(host.getScene().getWindow());

        ChatBotService bot = new ChatBotService();

        // === Carte du chat ===
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 18; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.35), 30, 0, 0, 10);");
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setPrefHeight(560);
        card.setMaxHeight(560);

        // --- Header ---
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 16, 18));
        header.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #1d4ed8); " +
                "-fx-background-radius: 18 18 0 0;");

        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20; " +
                "-fx-pref-width: 38; -fx-pref-height: 38; -fx-min-width: 38; -fx-min-height: 38;");
        FontIcon botIc = new FontIcon(FontAwesomeSolid.ROBOT);
        botIc.setIconSize(18);
        botIc.setIconColor(Color.WHITE);
        avatar.getChildren().add(botIc);

        VBox titleBox = new VBox(1);
        Label title = new Label("MediBot");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label status = new Label("Assistant medical - en ligne");
        status.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.85);");
        titleBox.getChildren().addAll(title, status);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button btnClose = new Button();
        FontIcon closeIc = new FontIcon(FontAwesomeSolid.TIMES);
        closeIc.setIconSize(13);
        closeIc.setIconColor(Color.WHITE);
        btnClose.setGraphic(closeIc);
        btnClose.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20; " +
                "-fx-cursor: hand; -fx-min-width: 30; -fx-min-height: 30; -fx-padding: 0;");
        btnClose.setOnAction(e -> chatStage.close());

        header.getChildren().addAll(avatar, titleBox, hSpacer, btnClose);

        // --- Messages area ---
        VBox messages = new VBox(10);
        messages.setPadding(new Insets(16));
        messages.setStyle("-fx-background-color: #f8fafc;");

        ScrollPane scroll = new ScrollPane(messages);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: #f8fafc; -fx-background-color: #f8fafc; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        messages.heightProperty().addListener((obs, o, n) -> scroll.setVvalue(1.0));

        // --- Input area ---
        HBox inputBox = new HBox(8);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setPadding(new Insets(12, 14, 14, 14));
        inputBox.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 18 18;");

        TextField input = new TextField();
        input.setPromptText("Ecrivez votre message...");
        input.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #cbd5e1; " +
                "-fx-padding: 8 14; -fx-font-size: 12px;");
        HBox.setHgrow(input, Priority.ALWAYS);

        // Validation saisie : limite de longueur
        input.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.length() > INPUT_MAX_LENGTH) {
                input.setText(n.substring(0, INPUT_MAX_LENGTH));
            }
        });

        Button btnSend = new Button();
        FontIcon sendIc = new FontIcon(FontAwesomeSolid.PAPER_PLANE);
        sendIc.setIconSize(14);
        sendIc.setIconColor(Color.WHITE);
        btnSend.setGraphic(sendIc);
        btnSend.setStyle("-fx-background-color: linear-gradient(to bottom right, #2563eb, #1d4ed8); " +
                "-fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 38; -fx-min-height: 38; -fx-padding: 0;");

        inputBox.getChildren().addAll(input, btnSend);

        card.getChildren().addAll(header, scroll, inputBox);

        // === Overlay qui positionne la carte en bas a droite ===
        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color: transparent;");
        StackPane.setAlignment(card, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(card, new Insets(0, 25, 100, 0));
        overlay.setPickOnBounds(false);

        Scene scene = new Scene(overlay, 470, 700);
        scene.setFill(Color.TRANSPARENT);
        chatStage.setScene(scene);

        // Positionner la fenetre en bas-droite de la fenetre principale
        Platform.runLater(() -> {
            Stage owner = (Stage) host.getScene().getWindow();
            chatStage.setX(owner.getX() + owner.getWidth() - 485);
            chatStage.setY(owner.getY() + owner.getHeight() - 720);
        });

        // === Action handlers ===
        Runnable doSend = () -> {
            String text = input.getText();
            if (!validateInput(text, messages)) return;
            addUserBubble(messages, text);
            input.clear();
            BotResponse resp = bot.process(text);
            animateBotTyping(messages, () -> {
                addBotBubble(messages, resp.message);
                renderSuggestions(messages, resp.suggestions, input, btnSend);
                if (bot.getState() == ChatBotService.State.RESULT) {
                    disableInput(input, btnSend, "Reservation en cours...");
                    startBookingFlow(messages, bot);
                }
            });
        };

        btnSend.setOnAction(e -> doSend.run());
        input.setOnAction(e -> doSend.run());

        chatStage.show();

        // Message de bienvenue
        BotResponse welcome = bot.start();
        addBotBubble(messages, welcome.message);
        renderSuggestions(messages, welcome.suggestions, input, btnSend);
    }

    // ========== VALIDATION SAISIE ==========

    private boolean validateInput(String text, VBox messages) {
        if (text == null || text.trim().isEmpty()) {
            showToast(messages, "Veuillez saisir un message.");
            return false;
        }
        if (text.length() > INPUT_MAX_LENGTH) {
            showToast(messages, "Message trop long (max " + INPUT_MAX_LENGTH + " caracteres).");
            return false;
        }
        return true;
    }

    private void showToast(VBox messages, String msg) {
        Label toast = new Label(msg);
        toast.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-size: 11px; " +
                "-fx-padding: 6 12; -fx-background-radius: 12;");
        HBox wrap = new HBox(toast);
        wrap.setAlignment(Pos.CENTER);
        messages.getChildren().add(wrap);
        FadeTransition ft = new FadeTransition(Duration.seconds(2.5), wrap);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> messages.getChildren().remove(wrap));
        ft.play();
    }

    // ========== RENDER BUBBLES ==========

    private void addUserBubble(VBox messages, String text) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(280);
        bubble.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #1d4ed8); " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 9 14; " +
                "-fx-background-radius: 16 16 4 16;");
        HBox wrap = new HBox(bubble);
        wrap.setAlignment(Pos.CENTER_RIGHT);
        messages.getChildren().add(wrap);
    }

    private void addBotBubble(VBox messages, String text) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(300);
        bubble.setStyle("-fx-background-color: white; -fx-text-fill: #1e293b; -fx-font-size: 12px; " +
                "-fx-padding: 10 14; -fx-background-radius: 16 16 16 4; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 16 16 16 4; -fx-border-width: 1;");
        HBox wrap = new HBox(bubble);
        wrap.setAlignment(Pos.CENTER_LEFT);
        messages.getChildren().add(wrap);
    }

    private void animateBotTyping(VBox messages, Runnable afterTyping) {
        Label typing = new Label("MediBot ecrit...");
        typing.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic; " +
                "-fx-padding: 4 12;");
        HBox wrap = new HBox(typing);
        wrap.setAlignment(Pos.CENTER_LEFT);
        messages.getChildren().add(wrap);
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> { messages.getChildren().remove(wrap); afterTyping.run(); });
        pause.play();
    }

    private void renderSuggestions(VBox messages, List<String> suggestions, TextField input, Button btnSend) {
        if (suggestions == null || suggestions.isEmpty()) return;
        FlowPane flow = new FlowPane(6, 6);
        flow.setPadding(new Insets(2, 0, 2, 10));
        for (String s : suggestions) {
            Button chip = new Button(s);
            chip.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-size: 11px; " +
                    "-fx-background-radius: 14; -fx-cursor: hand; -fx-padding: 5 12;");
            chip.setOnAction(e -> {
                input.setText(s);
                btnSend.fire();
            });
            flow.getChildren().add(chip);
        }
        messages.getChildren().add(flow);
    }

    private void renderRdvAction(VBox messages, ChatBotService bot) {
        Urgency u = bot.computeUrgency();

        Label badge = new Label("Urgence : " + u.label);
        badge.setStyle("-fx-background-color: " + u.color + "; -fx-text-fill: white; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 12;");

        Button btnRdv = new Button("  Prendre rendez-vous");
        FontIcon ic = new FontIcon(FontAwesomeSolid.CALENDAR_PLUS);
        ic.setIconSize(14);
        ic.setIconColor(Color.WHITE);
        btnRdv.setGraphic(ic);
        btnRdv.setStyle("-fx-background-color: linear-gradient(to right, #16a34a, #15803d); " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.35), 8, 0, 0, 3);");
        btnRdv.setOnAction(e -> {
            chatStage.close();
            // legacy: plus utilise, la reservation se fait maintenant dans le chat
        });

        VBox box = new VBox(8, badge, btnRdv);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 0, 0, 10));
        messages.getChildren().add(box);
    }

    // ========== BOOKING FLOW (MediBot reserve lui-meme) ==========

    private void disableInput(TextField input, Button btnSend, String prompt) {
        input.setDisable(true);
        input.setPromptText(prompt);
        btnSend.setDisable(true);
    }

    private void startBookingFlow(VBox messages, ChatBotService bot) {
        Urgency u = bot.computeUrgency();

        // Badge urgence
        Label badge = new Label("Urgence : " + u.label);
        badge.setStyle("-fx-background-color: " + u.color + "; -fx-text-fill: white; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 12;");
        HBox badgeWrap = new HBox(badge);
        badgeWrap.setAlignment(Pos.CENTER_LEFT);
        badgeWrap.setPadding(new Insets(2, 0, 2, 10));
        messages.getChildren().add(badgeWrap);

        if (u == Urgency.CRITIQUE) {
            addBotBubble(messages, "Je ne peux pas reserver pour une urgence critique. Appelez le SAMU (190) ou rendez-vous aux urgences immediatement.");
            return;
        }

        String specialiteNom = bot.getSpecialite().nom;
        int specialiteId = findSpecialiteId(specialiteNom);
        if (specialiteId <= 0) {
            addBotBubble(messages, "Desole, la specialite '" + specialiteNom + "' n'est pas disponible dans notre base. Je ne peux pas reserver automatiquement.");
            return;
        }

        List<Medecin> medecins = service.getMedecinsBySpecialite(specialiteId);
        if (medecins.isEmpty()) {
            addBotBubble(messages, "Aucun medecin disponible actuellement en " + specialiteNom + ".");
            return;
        }

        addBotBubble(messages, "Parfait ! Voici les medecins en " + specialiteNom +
                ".\nChoisissez celui avec qui vous souhaitez prendre rendez-vous :");
        renderMedecinChoices(messages, medecins, specialiteNom);
    }

    private int findSpecialiteId(String nom) {
        if (nom == null) return -1;
        List<Specialite> all = service.getAllSpecialites();
        String nomNorm = normalize(nom);

        // 1) Exact (insensible a la casse et aux accents)
        for (Specialite s : all) {
            if (s.getNom() != null && normalize(s.getNom()).equals(nomNorm)) return s.getId();
        }
        // 2) Prefixe commun d'au moins 5 caracteres
        for (Specialite s : all) {
            if (s.getNom() == null) continue;
            if (commonPrefixLength(nomNorm, normalize(s.getNom())) >= 5) return s.getId();
        }
        // 3) Contains
        for (Specialite s : all) {
            if (s.getNom() == null) continue;
            String dbNorm = normalize(s.getNom());
            if (dbNorm.contains(nomNorm) || nomNorm.contains(dbNorm)) return s.getId();
        }
        // 4) Similarite floue (Levenshtein) : tolere les fautes de frappe
        int bestId = -1;
        double bestScore = 0.0;
        for (Specialite s : all) {
            if (s.getNom() == null) continue;
            String dbNorm = normalize(s.getNom());
            double sim = similarity(nomNorm, dbNorm);
            if (sim > bestScore) { bestScore = sim; bestId = s.getId(); }
        }
        // Seuil : 55% de similarite minimum
        if (bestScore >= 0.55) return bestId;
        return -1;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace('é', 'e').replace('è', 'e').replace('ê', 'e').replace('ë', 'e')
                .replace('à', 'a').replace('â', 'a')
                .replace('î', 'i').replace('ï', 'i')
                .replace('ô', 'o').replace('ö', 'o')
                .replace('ù', 'u').replace('û', 'u')
                .replace('ç', 'c')
                .trim();
    }

    private int commonPrefixLength(String a, String b) {
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }

    /** Similarite entre 0 (totalement different) et 1 (identique) via Levenshtein. */
    private double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) levenshtein(a, b)) / maxLen;
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[b.length()];
    }

    private void renderMedecinChoices(VBox messages, List<Medecin> medecins, String specialiteNom) {
        VBox listBox = new VBox(6);
        listBox.setPadding(new Insets(4, 0, 4, 10));
        for (Medecin m : medecins) {
            String cabinet = (m.getCabinet() != null && !m.getCabinet().isEmpty()) ? " - " + m.getCabinet() : "";
            Button chip = new Button("Dr. " + m.getFullName() + cabinet);
            chip.setStyle("-fx-background-color: white; -fx-text-fill: #1e293b; -fx-font-size: 12px; " +
                    "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 14; " +
                    "-fx-border-color: #cbd5e1; -fx-border-radius: 10; -fx-border-width: 1;");
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setOnAction(e -> {
                disableChildren(listBox);
                addUserBubble(messages, "Dr. " + m.getFullName());
                animateBotTyping(messages, () -> askDate(messages, m, specialiteNom));
            });
            listBox.getChildren().add(chip);
        }
        messages.getChildren().add(listBox);
    }

    private void askDate(VBox messages, Medecin medecin, String specialiteNom) {
        addBotBubble(messages, "Sur quelle date souhaitez-vous le rendez-vous avec Dr. " + medecin.getFullName() + " ?\n" +
                "Voici les prochains jours avec des creneaux disponibles :");
        renderDateChoices(messages, medecin, specialiteNom);
    }

    private void renderDateChoices(VBox messages, Medecin medecin, String specialiteNom) {
        FlowPane flow = new FlowPane(6, 6);
        flow.setPadding(new Insets(2, 0, 2, 10));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH);

        LocalDate today = LocalDate.now();
        int found = 0;
        for (int i = 0; i < JOURS_RECHERCHE && found < 10; i++) {
            LocalDate date = today.plusDays(i);
            List<LocalTime> libres = getCreneauxLibres(medecin.getId(), date);
            if (libres.isEmpty()) continue;
            found++;
            Button chip = new Button(fmt.format(date) + "  (" + libres.size() + ")");
            chip.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-size: 11px; " +
                    "-fx-font-weight: bold; -fx-background-radius: 14; -fx-cursor: hand; -fx-padding: 6 12;");
            chip.setOnAction(e -> {
                disableChildren(flow);
                addUserBubble(messages, fmt.format(date));
                animateBotTyping(messages, () -> askCreneau(messages, medecin, date, libres, specialiteNom));
            });
            flow.getChildren().add(chip);
        }
        if (found == 0) {
            addBotBubble(messages, "Desole, Dr. " + medecin.getFullName() +
                    " n'a aucun creneau disponible dans les " + JOURS_RECHERCHE + " prochains jours.");
            return;
        }
        messages.getChildren().add(flow);
    }

    private void askCreneau(VBox messages, Medecin medecin, LocalDate date, List<LocalTime> libres, String specialiteNom) {
        addBotBubble(messages, "Choisissez l'horaire qui vous convient :");
        FlowPane flow = new FlowPane(6, 6);
        flow.setPadding(new Insets(2, 0, 2, 10));
        for (LocalTime t : libres) {
            String h = t.toString().substring(0, 5);
            Button chip = new Button(h);
            chip.setStyle("-fx-background-color: white; -fx-text-fill: #1e293b; -fx-font-size: 11px; " +
                    "-fx-font-weight: bold; -fx-background-radius: 14; -fx-cursor: hand; -fx-padding: 6 14; " +
                    "-fx-border-color: #cbd5e1; -fx-border-radius: 14; -fx-border-width: 1;");
            chip.setOnAction(e -> {
                disableChildren(flow);
                addUserBubble(messages, h);
                animateBotTyping(messages, () -> confirmBooking(messages, medecin, date, t, specialiteNom));
            });
            flow.getChildren().add(chip);
        }
        messages.getChildren().add(flow);
    }

    private void confirmBooking(VBox messages, Medecin medecin, LocalDate date, LocalTime heure, String specialiteNom) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        String h = heure.toString().substring(0, 5);
        addBotBubble(messages, "Recapitulatif :\n\n" +
                "- Medecin : Dr. " + medecin.getFullName() + "\n" +
                "- Specialite : " + specialiteNom + "\n" +
                "- Date : " + df.format(date) + "\n" +
                "- Heure : " + h + "\n\n" +
                "Confirmez-vous la reservation ?");

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(2, 0, 6, 10));

        Button btnYes = new Button("  Confirmer");
        FontIcon okIc = new FontIcon(FontAwesomeSolid.CHECK);
        okIc.setIconSize(12);
        okIc.setIconColor(Color.WHITE);
        btnYes.setGraphic(okIc);
        btnYes.setStyle("-fx-background-color: linear-gradient(to right, #16a34a, #15803d); " +
                "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 7 16;");

        Button btnNo = new Button("Annuler");
        btnNo.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 7 16;");

        btnYes.setOnAction(e -> {
            disableChildren(actions);
            addUserBubble(messages, "Confirmer");
            animateBotTyping(messages, () -> doCreate(messages, medecin, date, heure, specialiteNom));
        });
        btnNo.setOnAction(e -> {
            disableChildren(actions);
            addUserBubble(messages, "Annuler");
            addBotBubble(messages, "Reservation annulee.");
        });

        actions.getChildren().addAll(btnYes, btnNo);
        messages.getChildren().add(actions);
    }

    private void doCreate(VBox messages, Medecin medecin, LocalDate date, LocalTime heure, String specialiteNom) {
        RendezVous rv = new RendezVous();
        rv.setMedecinId(medecin.getId());
        rv.setPatientId(patientId);
        rv.setDate(date);
        rv.setHeure(heure);
        rv.setStatut("en_attente");

        boolean ok = service.create(rv);
        if (ok) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String h = heure.toString().substring(0, 5);
            String msg = "Rendez-vous enregistre avec succes !\n\n" +
                    "- Dr. " + medecin.getFullName() + " (" + specialiteNom + ")\n" +
                    "- " + df.format(date) + " a " + h + "\n" +
                    "- Statut : En attente de confirmation\n\n" +
                    "Vous recevrez une notification lors de la confirmation.";
            addBotBubble(messages, msg);
            addSuccessBadge(messages);
            if (onBookingSuccess != null) Platform.runLater(onBookingSuccess);
        } else {
            addBotBubble(messages, "Une erreur est survenue lors de la reservation. Veuillez reessayer.");
        }
    }

    private void addSuccessBadge(VBox messages) {
        Label lbl = new Label("  Reservation confirmee");
        FontIcon ic = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
        ic.setIconSize(13);
        ic.setIconColor(Color.web("#16a34a"));
        lbl.setGraphic(ic);
        lbl.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 12;");
        HBox wrap = new HBox(lbl);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.setPadding(new Insets(2, 0, 2, 10));
        messages.getChildren().add(wrap);
    }

    private List<LocalTime> getCreneauxLibres(int medecinId, LocalDate date) {
        List<LocalTime> all = service.getCreneauxDisponibles(medecinId, date);
        List<LocalTime> prises = service.getHeuresPrises(medecinId, date);
        List<LocalTime> libres = new ArrayList<>();
        LocalTime now = LocalTime.now();
        boolean today = date.equals(LocalDate.now());
        for (LocalTime t : all) {
            if (prises.contains(t)) continue;
            if (today && !t.isAfter(now)) continue;
            libres.add(t);
        }
        return libres;
    }

    private void disableChildren(Pane p) {
        p.getChildren().forEach(c -> c.setDisable(true));
        p.setOpacity(0.55);
    }
}

