package com.medicare.controllers;

import com.medicare.models.ListeAttente;
import com.medicare.models.RendezVous;
import com.medicare.models.User;
import com.medicare.services.EmailService;
import com.medicare.services.ListeAttenteService;
import com.medicare.services.RendezVousService;
import com.medicare.services.UserService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RendezVousListController {

    @FXML private VBox container;

    private final RendezVousService   service             = new RendezVousService();
    private final EmailService        emailService        = new EmailService();
    private final UserService         userService         = new UserService();
    private final ListeAttenteService listeAttenteService = new ListeAttenteService();
    private int patientId;
    private StackPane contentArea;
    private List<RendezVous> allRdvs;
    private String currentFilter = "all";
    private String searchQuery = "";
    private Button btnAll, btnAccepte, btnAttente, btnAnnule;
    private Button btnCalendrier;

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
        loadRendezVous();
        // Attacher le chatbot flottant (differe apres le rendu)
        javafx.application.Platform.runLater(() -> {
            if (contentArea != null) {
                com.medicare.utils.ChatBotWidget.attachTo(contentArea, patientId, this::reloadFullList);
            }
        });
    }

    private void reloadFullList() {
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                com.medicare.HelloApplication.class.getResource("rendez-vous-list-view.fxml"));
            Node view = loader.load();
            RendezVousListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setPatientId(patientId);
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

        // Boutons filtres
        btnAll = createFilterBtn("Tout", "all");
        btnAccepte = createFilterBtn("Confirme", "confirme");
        btnAttente = createFilterBtn("En attente", "en_attente");
        btnAnnule = createFilterBtn("Annule", "annule");

        HBox filters = new HBox(8, btnAll, btnAccepte, btnAttente, btnAnnule);
        filters.setAlignment(Pos.CENTER_LEFT);

        // Bouton calendrier
        btnCalendrier = new Button("  Calendrier");
        FontIcon calIcon = new FontIcon(FontAwesomeSolid.CALENDAR_ALT);
        calIcon.setIconSize(14);
        calIcon.setIconColor(Color.WHITE);
        btnCalendrier.setGraphic(calIcon);
        btnCalendrier.setStyle("-fx-background-color: linear-gradient(to right, #0ea5e9, #2563eb); " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.25), 6, 0, 0, 2);");
        btnCalendrier.setOnAction(e -> showCalendarModal());

        // Barre de recherche
        javafx.scene.control.TextField searchField = new javafx.scene.control.TextField();
        searchField.setPromptText("Rechercher par medecin ou specialite...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #d1d5db; " +
                             "-fx-padding: 5 12; -fx-font-size: 12px;");
        FontIcon searchIcon = new FontIcon(FontAwesomeSolid.SEARCH);
        searchIcon.setIconSize(12);
        searchIcon.setIconColor(Color.web("#9ca3af"));
        searchField.textProperty().addListener((obs, old, val) -> {
            searchQuery = val.trim().toLowerCase();
            applyFilters();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNew = new Button("  Prendre un rendez-vous");
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS_CIRCLE);
        plusIcon.setIconSize(16);
        plusIcon.setIconColor(Color.WHITE);
        btnNew.setGraphic(plusIcon);
        btnNew.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
        btnNew.setOnAction(e -> openForm(null));

        header.getChildren().addAll(filters, btnCalendrier, searchField, spacer, btnNew);
        container.getChildren().add(header);

        allRdvs = service.getByPatient(patientId);
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

    private void applyFilters() {
        // Retirer les anciennes cards (garder le header en index 0)
        if (container.getChildren().size() > 1) {
            container.getChildren().remove(1, container.getChildren().size());
        }

        List<RendezVous> filtered = allRdvs.stream()
                .filter(rv -> currentFilter.equals("all") || rv.getStatut().equals(currentFilter))
                .filter(rv -> searchQuery.isEmpty() ||
                        rv.getMedecinFullName().toLowerCase().contains(searchQuery) ||
                        (rv.getSpecialite() != null && rv.getSpecialite().toLowerCase().contains(searchQuery)))
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

    private void highlightFilter(Button active) {
        String normal = "-fx-background-color: #e5e7eb; -fx-text-fill: #555; -fx-font-size: 12px; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14;";
        String selected = "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 12px; " +
                          "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14;";
        btnAll.setStyle(normal);
        btnAccepte.setStyle(normal);
        btnAttente.setStyle(normal);
        btnAnnule.setStyle(normal);
        active.setStyle(selected);
    }

    private VBox buildCard(RendezVous rv, DateTimeFormatter dateFmt, DateTimeFormatter heureFmt) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        // Ligne principale : icône + infos + actions
        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon calIcon = new FontIcon(FontAwesomeSolid.CALENDAR_CHECK);
        calIcon.setIconSize(28);
        calIcon.setIconColor(Color.web("#1a73e8"));

        VBox infos = new VBox(3);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label medecinLabel = new Label("Dr. " + rv.getMedecinFullName());
        medecinLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label specLabel = new Label(rv.getSpecialite());
        specLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

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

        infos.getChildren().addAll(medecinLabel, specLabel, dateLabel, statutLabel);

        // Étiquette "Reporté" si le médecin a proposé un report
        if (rv.isReportPending() && rv.getProposedDate() != null) {
            Label reportLabel = new Label("Reporté → " + rv.getProposedDate().format(dateFmt) + " a " + rv.getProposedHeure().format(heureFmt));
            reportLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 10; " +
                                 "-fx-background-color: #8b5cf6; -fx-background-radius: 12;");
            infos.getChildren().add(reportLabel);
        }

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        Button btnVoir = createActionBtn(FontAwesomeSolid.EYE, "#1a73e8", "#e8f0fe", "Voir");
        btnVoir.setOnAction(e -> showDetails(rv));
        actions.getChildren().add(btnVoir);

        // Si report en attente → boutons accepter/refuser
        if (rv.isReportPending() && rv.getProposedDate() != null) {
            Button btnAcceptReport = createActionBtn(FontAwesomeSolid.CHECK, "#16a34a", "#dcfce7", "Accepter le report");
            btnAcceptReport.setOnAction(e -> showAcceptReportConfirmation(rv));

            Button btnRefuseReport = createActionBtn(FontAwesomeSolid.TIMES, "#dc2626", "#fee2e2", "Refuser le report");
            btnRefuseReport.setOnAction(e -> showRefuseReportConfirmation(rv));

            actions.getChildren().addAll(btnAcceptReport, btnRefuseReport);
        } else if (!rv.getStatut().equals("annule") && !rv.getStatut().equals("termine")) {
            Button btnEdit = createActionBtn(FontAwesomeSolid.PEN, "#f59e0b", "#fef3c7", "Modifier");
            btnEdit.setOnAction(e -> openForm(rv));

            Button btnCancel = createActionBtn(FontAwesomeSolid.BAN, "#dc2626", "#fee2e2", "Annuler");
            btnCancel.setOnAction(e -> showCancelConfirmation(rv));

            actions.getChildren().addAll(btnEdit, btnCancel);
        }

        topRow.getChildren().addAll(calIcon, infos, actions);
        card.getChildren().add(topRow);

        // Bouton ordonnance centré en bas de la card
        if (rv.getStatut().equals("confirme") && service.hasOrdonnance(rv.getId())) {
            // Séparateur
            Region separator = new Region();
            separator.setStyle("-fx-background-color: #e5e7eb; -fx-min-height: 1; -fx-max-height: 1;");

            Button btnOrdo = new Button("  Télécharger mon ordonnance");
            FontIcon pdfIcon = new FontIcon(FontAwesomeSolid.FILE_PDF);
            pdfIcon.setIconSize(16);
            pdfIcon.setIconColor(Color.web("#3b82f6"));
            btnOrdo.setGraphic(pdfIcon);
            btnOrdo.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #3b82f6; -fx-font-size: 13px; " +
                             "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 24;");
            btnOrdo.setOnAction(e -> downloadOrdonnancePDF(rv));

            HBox ordoRow = new HBox(btnOrdo);
            ordoRow.setAlignment(Pos.CENTER);
            ordoRow.setPadding(new Insets(5, 0, 0, 0));

            card.getChildren().addAll(separator, ordoRow);
        }

        return card;
    }

    private Stage calendarStage;

    // Applique un flou sur la fenêtre parente pendant l'affichage du popup
    private void attachBlur(Stage popup, Window owner) {
        Node ownerRoot = (owner != null && owner instanceof Stage)
                ? ((Stage) owner).getScene().getRoot()
                : (owner != null ? owner.getScene().getRoot() : null);
        if (ownerRoot == null) return;
        GaussianBlur blur = new GaussianBlur(12);
        ownerRoot.setEffect(blur);
        popup.setOnHidden(ev -> ownerRoot.setEffect(null));
    }

    private void showCalendarModal() {
        List<RendezVous> rdvs = service.getByPatient(patientId);
        Map<LocalDate, List<RendezVous>> byDate = groupByDate(rdvs);

        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());
        calendarStage = popup;

        VBox modal = new VBox(16);
        modal.setPadding(new Insets(28));
        modal.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 20; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.35), 40, 0, 0, 12);");
        modal.setMaxWidth(900);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Calendrier des rendez-vous");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button();
        FontIcon closeIcon = new FontIcon(FontAwesomeSolid.TIMES);
        closeIcon.setIconSize(12);
        closeIcon.setIconColor(Color.web("#1e293b"));
        btnClose.setGraphic(closeIcon);
        btnClose.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 10;");
        btnClose.setTooltip(new Tooltip("Fermer"));
        btnClose.setOnAction(e -> popup.close());

        header.getChildren().addAll(title, spacer, btnClose);

        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER_LEFT);

        Button prev = new Button("<");
        prev.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12;");
        Button next = new Button(">");
        next.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12;");

        Label monthLabel = new Label();
        monthLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        nav.getChildren().addAll(prev, monthLabel, next);

        GridPane calendarGrid = new GridPane();
        calendarGrid.setHgap(8);
        calendarGrid.setVgap(8);
        calendarGrid.setPadding(new Insets(10, 0, 0, 0));

        YearMonth[] currentMonth = new YearMonth[] { YearMonth.now() };
        renderCalendar(calendarGrid, monthLabel, currentMonth[0], byDate);

        prev.setOnAction(e -> {
            currentMonth[0] = currentMonth[0].minusMonths(1);
            renderCalendar(calendarGrid, monthLabel, currentMonth[0], byDate);
        });
        next.setOnAction(e -> {
            currentMonth[0] = currentMonth[0].plusMonths(1);
            renderCalendar(calendarGrid, monthLabel, currentMonth[0], byDate);
        });

        modal.getChildren().addAll(header, nav, calendarGrid);

        StackPane overlay = new StackPane(modal);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPadding(new Insets(20));

        Scene scene = new Scene(overlay, 980, 680);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        attachBlur(popup, container.getScene().getWindow());
        popup.show();
    }

    private void renderCalendar(GridPane grid, Label monthLabel, YearMonth month, Map<LocalDate, List<RendezVous>> byDate) {
        grid.getChildren().clear();

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
        monthLabel.setText(month.atDay(1).format(monthFmt));

        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < days.length; i++) {
            Label d = new Label(days[i]);
            d.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            grid.add(d, i, 0);
        }

        LocalDate first = month.atDay(1);
        int startCol = (first.getDayOfWeek().getValue() + 6) % 7; // lundi=0
        int dayCount = month.lengthOfMonth();

        int row = 1;
        int col = startCol;

        for (int day = 1; day <= dayCount; day++) {
            LocalDate date = month.atDay(day);
            VBox cell = new VBox(4);
            cell.setPadding(new Insets(8));
            cell.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-cursor: hand;");

            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            cell.getChildren().add(dayLabel);

            List<RendezVous> list = byDate.getOrDefault(date, new ArrayList<>());
            if (!list.isEmpty()) {
                HBox dots = new HBox(3);
                dots.setAlignment(Pos.CENTER_LEFT);
                int shown = Math.min(list.size(), 4);
                for (int i = 0; i < shown; i++) {
                    RendezVous rv = list.get(i);
                    String color = switch (rv.getStatut()) {
                        case "confirme" -> "#16a34a";
                        case "annule" -> "#dc2626";
                        default -> "#f59e0b";
                    };
                    Region dot = new Region();
                    dot.setPrefSize(8, 8);
                    dot.setMinSize(8, 8);
                    dot.setMaxSize(8, 8);
                    dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");
                    dots.getChildren().add(dot);
                }
                cell.getChildren().add(dots);
                if (list.size() > 4) {
                    Label more = new Label("+" + (list.size() - 4));
                    more.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                    cell.getChildren().add(more);
                }
            }

            cell.setOnMouseClicked(e -> {
                if (!list.isEmpty()) {
                    showDayDetailsModal(date, list);
                }
            });

            grid.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private Map<LocalDate, List<RendezVous>> groupByDate(List<RendezVous> rdvs) {
        Map<LocalDate, List<RendezVous>> map = new HashMap<>();
        for (RendezVous rv : rdvs) {
            map.computeIfAbsent(rv.getDate(), k -> new ArrayList<>()).add(rv);
        }
        return map;
    }

    private void showDayDetailsModal(LocalDate date, List<RendezVous> list) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        // Ouvrir par-dessus le modal calendrier s'il est ouvert
        Window owner = (calendarStage != null && calendarStage.isShowing())
                ? calendarStage
                : container.getScene().getWindow();
        popup.initOwner(owner);

        VBox modal = new VBox(14);
        modal.setPadding(new Insets(26));
        modal.setStyle("-fx-background-color: white; -fx-background-radius: 18; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 18; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.35), 35, 0, 0, 10);");
        modal.setMaxWidth(520);

        // En-tête avec icône + titre + bouton fermer
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrap = new StackPane();
        iconWrap.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 12; " +
                "-fx-pref-width: 40; -fx-pref-height: 40; -fx-min-width: 40; -fx-min-height: 40;");
        FontIcon calIcon = new FontIcon(FontAwesomeSolid.CALENDAR_DAY);
        calIcon.setIconSize(18);
        calIcon.setIconColor(Color.web("#3b82f6"));
        iconWrap.getChildren().add(calIcon);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
        VBox titleBox = new VBox(2);
        Label title = new Label("Rendez-vous du jour");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label subtitle = new Label(date.format(fmt));
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button btnCloseTop = new Button();
        FontIcon closeIc = new FontIcon(FontAwesomeSolid.TIMES);
        closeIc.setIconSize(14);
        closeIc.setIconColor(Color.web("#1e293b"));
        btnCloseTop.setGraphic(closeIc);
        String closeBaseStyle = "-fx-background-color: #e2e8f0; -fx-background-radius: 20; -fx-cursor: hand; " +
                "-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32; -fx-padding: 0;";
        String closeHoverStyle = "-fx-background-color: #fecaca; -fx-background-radius: 20; -fx-cursor: hand; " +
                "-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32; -fx-padding: 0;";
        btnCloseTop.setStyle(closeBaseStyle);
        btnCloseTop.setOnMouseEntered(e -> {
            btnCloseTop.setStyle(closeHoverStyle);
            closeIc.setIconColor(Color.web("#dc2626"));
        });
        btnCloseTop.setOnMouseExited(e -> {
            btnCloseTop.setStyle(closeBaseStyle);
            closeIc.setIconColor(Color.web("#1e293b"));
        });
        btnCloseTop.setTooltip(new Tooltip("Fermer"));
        btnCloseTop.setOnAction(e -> popup.close());

        header.getChildren().addAll(iconWrap, titleBox, hSpacer, btnCloseTop);

        VBox listBox = new VBox(10);
        for (RendezVous rv : list) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(12));
            row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; " +
                    "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

            StackPane timeWrap = new StackPane();
            timeWrap.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; -fx-padding: 6 10;");
            Label time = new Label(rv.getHeure().toString().substring(0, 5));
            time.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
            timeWrap.getChildren().add(time);

            VBox info = new VBox(2);
            Label name = new Label("Dr. " + rv.getMedecinFullName());
            name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            info.getChildren().add(name);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(rv.getStatut());
            String color = switch (rv.getStatut()) {
                case "confirme" -> "#16a34a";
                case "annule" -> "#dc2626";
                default -> "#f59e0b";
            };
            status.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 10px; " +
                    "-fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 10;");

            row.getChildren().addAll(timeWrap, info, spacer, status);
            listBox.getChildren().add(row);
        }

        Button close = new Button("Fermer");
        close.setStyle("-fx-background-color: linear-gradient(to right, #1e293b, #0f172a); -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 10 28;");
        close.setOnAction(e -> popup.close());
        HBox closeRow = new HBox(close);
        closeRow.setAlignment(Pos.CENTER_RIGHT);

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(320);
        scroll.setMaxHeight(320);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        modal.getChildren().addAll(header, scroll, closeRow);

        StackPane overlay = new StackPane(modal);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPadding(new Insets(20));

        Scene scene = new Scene(overlay, 620, 520);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        attachBlur(popup, owner);
        popup.show();
    }

    private Button createActionBtn(FontAwesomeSolid iconType, String iconColor, String bgColor, String tooltipText) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(14);
        icon.setIconColor(Color.web(iconColor));
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-cursor: hand; -fx-padding: 6; " +
                     "-fx-background-radius: 8;");
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    // ========== CONFIRMATION ANNULATION ==========

    private void showCancelConfirmation(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        // Icône dans un cercle
        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 50; -fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(32);
        warnIcon.setIconColor(Color.web("#f59e0b"));
        iconCircle.getChildren().add(warnIcon);

        Label titleLbl = new Label("Annuler ce rendez-vous ?");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label msgLbl = new Label("Dr. " + rv.getMedecinFullName() + "\n" +
                rv.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " a " +
                rv.getHeure().format(DateTimeFormatter.ofPattern("HH:mm")));
        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center; -fx-line-spacing: 2;");
        msgLbl.setWrapText(true);

        Button btnOui = new Button("Oui, annuler");
        btnOui.setStyle("-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 35; " +
                        "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.3), 10, 0, 0, 3);");

        Button btnNon = new Button("Non, garder");
        btnNon.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 35;");

        HBox buttons = new HBox(15, btnNon, btnOui);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(18, iconCircle, titleLbl, msgLbl, buttons);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(35));
        box.setMaxWidth(420);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 20; -fx-border-width: 1; " +
                     "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.35), 35, 0, 0, 10);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPadding(new Insets(20));

        Scene scene = new Scene(overlay, 460, 340);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        attachBlur(popup, container.getScene().getWindow());
        popup.show();

        btnNon.setOnAction(e -> popup.close());
        btnOui.setOnAction(e -> {
            popup.close();
            service.cancel(rv.getId());
            // Email de notification au médecin
            User medecin = userService.getUserByMedecinId(rv.getMedecinId());
            User patient = userService.getUserByPatientId(patientId);
            if (medecin != null && medecin.getEmail() != null && patient != null) {
                emailService.envoyerAnnulationParPatient(
                        medecin.getEmail(),
                        medecin.getPrenom() + " " + medecin.getNom(),
                        patient.getPrenom() + " " + patient.getNom(),
                        rv.getDate().toString(),
                        rv.getHeure().toString()
                );
            }
            // Notifier le 1er patient en liste d'attente pour ce médecin ce jour
            ListeAttente premierEnAttente = listeAttenteService.getPremierEnAttente(rv.getMedecinId(), rv.getDate());
            if (premierEnAttente != null) {
                User patientAttente = userService.getUserByPatientId(premierEnAttente.getPatientId());
                if (patientAttente != null && patientAttente.getEmail() != null) {
                    String nomMedecin = medecin != null ? medecin.getPrenom() + " " + medecin.getNom() : "votre médecin";
                    emailService.envoyerCreneauLibere(
                            patientAttente.getEmail(),
                            patientAttente.getPrenom() + " " + patientAttente.getNom(),
                            nomMedecin,
                            rv.getDate().toString(),
                            rv.getHeure().toString()
                    );
                    listeAttenteService.marquerNotifie(premierEnAttente.getId());
                    System.out.println("✅ Créneau libéré notifié à : " + patientAttente.getEmail());
                }
            }
            showSuccessPopup("Rendez-vous annule",
                    "Votre rendez-vous a ete annule avec succes.",
                    FontAwesomeSolid.CALENDAR_TIMES, "#ef4444");
        });
    }

    // ========== POPUP SUCCES ==========

    private void showSuccessPopup(String title, String message, FontAwesomeSolid iconType, String color) {
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

        // Barre de progression animée
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
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        // Animation de la barre
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

    // ========== DETAILS MODAL ==========

    private void showDetails(RendezVous rv) {
        RendezVous full = service.getById(rv.getId());
        if (full == null) return;

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        VBox modal = new VBox(12);
        modal.setAlignment(Pos.CENTER);
        modal.setPadding(new Insets(30));
        modal.setMaxWidth(420);
        modal.setMaxHeight(450);
        modal.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);");

        FontIcon icon = new FontIcon(FontAwesomeSolid.NOTES_MEDICAL);
        icon.setIconSize(40);
        icon.setIconColor(Color.web("#1a73e8"));

        Label titleLabel = new Label("Details du rendez-vous");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label details = new Label(
            "Medecin :  Dr. " + full.getMedecinFullName() + "\n" +
            "Specialite :  " + full.getSpecialite() + "\n" +
            "Date :  " + full.getDate().format(dateFmt) + "\n" +
            "Heure :  " + full.getHeure().format(heureFmt) + "\n" +
            "Statut :  " + full.getStatut()
        );
        details.setStyle("-fx-font-size: 14px; -fx-text-fill: #444; -fx-line-spacing: 4;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 13px; " +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        closeBtn.setOnAction(e -> reloadFullList());

        modal.getChildren().addAll(icon, titleLabel, details);

        // Afficher le motif de consultation si présent
        if (full.getMotif() != null && !full.getMotif().trim().isEmpty()) {
            VBox motifBox = new VBox(5);
            motifBox.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 8; -fx-padding: 10;");

            Label motifTitle = new Label("Motif de consultation :");
            motifTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");

            Label motifText = new Label(full.getMotif());
            motifText.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e3a8a;");
            motifText.setWrapText(true);

            motifBox.getChildren().addAll(motifTitle, motifText);
            modal.getChildren().add(motifBox);
        }

        // Afficher le motif d'annulation si le RDV est annulé
        if ("annule".equals(full.getStatut()) && full.getMotifAnnulation() != null && !full.getMotifAnnulation().isEmpty()) {
            VBox motifBox = new VBox(5);
            motifBox.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 8; -fx-padding: 10;");

            Label motifTitle = new Label("Motif d'annulation :");
            motifTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");

            Label motifText = new Label(full.getMotifAnnulation());
            motifText.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f1d1d;");
            motifText.setWrapText(true);

            motifBox.getChildren().addAll(motifTitle, motifText);
            modal.getChildren().add(motifBox);
        }

        modal.getChildren().add(closeBtn);

        StackPane overlay = new StackPane(modal);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.4);");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) reloadFullList(); });

        contentArea.getChildren().clear();
        contentArea.getChildren().add(overlay);
    }

    // ========== REPORT PATIENT ==========

    // ========== ORDONNANCE PDF ==========

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
                showSuccessPopup("PDF genere", "L'ordonnance a ete telechargee.",
                          FontAwesomeSolid.FILE_PDF, "#16a34a");
                try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    }

    private void showAcceptReportConfirmation(RendezVous rv) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 50; " +
                            "-fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon icon = new FontIcon(FontAwesomeSolid.CALENDAR_ALT);
        icon.setIconSize(32);
        icon.setIconColor(Color.web("#3b82f6"));
        iconCircle.getChildren().add(icon);

        Label titleLbl = new Label("Report propose par le medecin");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label msgLbl = new Label("Nouvelle date : " + rv.getProposedDate().format(dateFmt) +
                "\nNouvel horaire : " + rv.getProposedHeure().format(heureFmt) +
                "\n\nAcceptez-vous ce report ?");
        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center; -fx-line-spacing: 2;");
        msgLbl.setWrapText(true);

        Button btnAccept = new Button("Accepter");
        btnAccept.setStyle("-fx-background-color: linear-gradient(to right, #16a34a, #15803d); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                           "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 35; " +
                           "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.3), 10, 0, 0, 3);");

        Button btnRefuse = new Button("Refuser");
        btnRefuse.setStyle("-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                           "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 35; " +
                           "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.3), 10, 0, 0, 3);");

        HBox buttons = new HBox(15, btnRefuse, btnAccept);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(18, iconCircle, titleLbl, msgLbl, buttons);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(35));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 440, 330);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        btnAccept.setOnAction(e -> {
            popup.close();
            service.acceptReport(rv.getId());
            showSuccessPopup("Report accepte", "Le rendez-vous a ete reporte avec succes.",
                    FontAwesomeSolid.CALENDAR_CHECK, "#16a34a");
        });

        btnRefuse.setOnAction(e -> {
            popup.close();
            service.refuseReport(rv.getId());
            showSuccessPopup("Report refuse", "Le rendez-vous a ete annule.",
                    FontAwesomeSolid.CALENDAR_TIMES, "#ef4444");
        });
    }

    private void showRefuseReportConfirmation(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 50; " +
                            "-fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon icon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        icon.setIconSize(32);
        icon.setIconColor(Color.web("#ef4444"));
        iconCircle.getChildren().add(icon);

        Label titleLbl = new Label("Refuser le report ?");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label msgLbl = new Label("Si vous refusez, le rendez-vous sera automatiquement annule.");
        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        msgLbl.setWrapText(true);

        Button btnConfirm = new Button("Oui, refuser");
        btnConfirm.setStyle("-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 35;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: bold; " +
                           "-fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 10 35;");

        HBox buttons = new HBox(15, btnCancel, btnConfirm);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(18, iconCircle, titleLbl, msgLbl, buttons);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(35));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 420, 290);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        btnCancel.setOnAction(e -> popup.close());
        btnConfirm.setOnAction(e -> {
            popup.close();
            service.refuseReport(rv.getId());
            showSuccessPopup("Report refuse", "Le rendez-vous a ete annule.",
                    FontAwesomeSolid.CALENDAR_TIMES, "#ef4444");
        });
    }

    // ========== NAVIGATION ==========

    private void openForm(RendezVous rvToEdit) {
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                com.medicare.HelloApplication.class.getResource("rendez-vous-form-view.fxml"));
            Node form = loader.load();
            RendezVousFormController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setPatientId(patientId);
            if (rvToEdit != null) {
                ctrl.setRendezVousToEdit(rvToEdit);
            }
            // Cacher le sidebar pour mode plein écran
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) contentArea.getScene().getRoot();
            Node sidebar = root.getLeft();
            if (sidebar != null) {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            }
            contentArea.getChildren().clear();
            contentArea.getChildren().add(form);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
