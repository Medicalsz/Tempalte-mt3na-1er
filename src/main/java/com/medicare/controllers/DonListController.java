package com.medicare.controllers;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.medicare.models.Don;
import com.medicare.services.DonationService;
import com.medicare.services.GeminiService;
import com.medicare.services.NotificationService;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Insets;
import java.io.File;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

public class DonListController {

    @FXML private TextField searchField;
    @FXML private Label moneyCountLabel;
    @FXML private Label materialCountLabel;

    @FXML private TableView<Don> moneyTable;
    @FXML private TableColumn<Don, String> moneyDonateurCol;
    @FXML private TableColumn<Don, String> moneyCauseCol;
    @FXML private TableColumn<Don, Double> moneyMontantCol;
    @FXML private TableColumn<Don, String> moneyAdresseCol;
    @FXML private TableColumn<Don, String> moneyDateCol;
    @FXML private TableColumn<Don, String> moneyModeCol;
    @FXML private TableColumn<Don, String> moneyStatutCol;

    @FXML private TableView<Don> materialTable;
    @FXML private TableColumn<Don, String> materialDonateurCol;
    @FXML private TableColumn<Don, String> materialCauseCol;
    @FXML private TableColumn<Don, String> materialMaterielsCol;
    @FXML private TableColumn<Don, Integer> materialQuantiteCol;
    @FXML private TableColumn<Don, String> materialAdresseCol;
    @FXML private TableColumn<Don, String> materialDateCol;
    @FXML private TableColumn<Don, String> materialStatutCol;
    @FXML private TableColumn<Don, List<String>> materialPhotoCol;

    private final DonationService donationService = new DonationService();
    private final GeminiService geminiService = new GeminiService(System.getenv("GEMINI_API_KEY"));
    private final NotificationService notificationService = new NotificationService();
    private final ObservableList<Don> allDons = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        try {
            System.out.println("DonListController: Début de l'initialisation...");
            setupTableColumns();
            loadDons();
            
            if (searchField != null) {
                searchField.textProperty().addListener((obs, old, val) -> filterDons(val));
            } else {
                System.err.println("DonListController: searchField est NULL !");
            }
            System.out.println("DonListController: Initialisation terminée avec succès.");
        } catch (Exception e) {
            System.err.println("DonListController: ERREUR CRITIQUE lors de l'initialisation :");
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClick() {
        try {
            System.out.println("DonListController: Retour vers admin-donation-view.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-donation-view.fxml"));
            Scene scene = moneyTable.getScene();
            if (scene == null) {
                System.err.println("DonListController: Scène nulle lors du retour !");
                return;
            }
            StackPane contentArea = (StackPane) scene.lookup("#contentArea");
            if (contentArea == null) {
                System.err.println("DonListController: contentArea non trouvé lors du retour !");
                return;
            }
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
        } catch (Exception e) {
            System.err.println("DonListController: Erreur lors du retour :");
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        // Styling headers
        moneyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        materialTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Money Table
        moneyDonateurCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullDonateurName() + "\n" + d.getValue().getDonateurEmail()));
        moneyCauseCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCauseNom()));
        
        // TRI ARGENT : On utilise SimpleObjectProperty<Double> pour le tri numérique
        moneyMontantCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMontant()));
        moneyMontantCol.setCellFactory(tc -> new TableCell<Don, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f DT", item));
                    setStyle("-fx-text-fill: #0d9488; -fx-font-weight: bold; -fx-alignment: center-left;");
                }
            }
        });

        moneyAdresseCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAdresse() != null ? d.getValue().getAdresse() : "pas d'adresse"));
        moneyDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate() != null ? d.getValue().getDate().format(formatter) : ""));
        moneyModeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMode()));
        moneyStatutCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));

        // Material Table
        materialDonateurCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullDonateurName() + "\n" + d.getValue().getDonateurEmail()));
        materialCauseCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCauseNom()));
        materialMaterielsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMateriels()));
        
        // TRI MATÉRIEL : On utilise SimpleObjectProperty<Integer> pour le tri numérique
        materialQuantiteCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getQuantite()));
        materialQuantiteCol.setCellFactory(tc -> new TableCell<Don, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });

        materialAdresseCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAdresse() != null ? d.getValue().getAdresse() : "pas d'adresse"));
        materialDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate() != null ? d.getValue().getDate().format(formatter) : ""));
        materialStatutCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        
        materialPhotoCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getObjectPhotos()));
        materialPhotoCol.setCellFactory(tc -> new TableCell<Don, List<String>>() {
            @Override
            protected void updateItem(List<String> photos, boolean empty) {
                super.updateItem(photos, empty);
                if (empty || photos == null || photos.isEmpty()) {
                    setGraphic(null);
                } else {
                    FontIcon icon = new FontIcon(FontAwesomeSolid.IMAGES);
                    icon.setIconSize(13);
                    icon.setIconColor(javafx.scene.paint.Color.WHITE);

                    Label countBadge = new Label(String.valueOf(photos.size()));
                    countBadge.setStyle("-fx-background-color: rgba(0,0,0,0.25); -fx-text-fill: white; " +
                            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 1 5;");

                    javafx.scene.layout.HBox btnContent = new javafx.scene.layout.HBox(5, icon, countBadge);
                    btnContent.setAlignment(javafx.geometry.Pos.CENTER);

                    Button btn = new Button();
                    btn.setGraphic(btnContent);
                    btn.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 20; " +
                            "-fx-padding: 5 12; -fx-cursor: hand;");
                    btn.setOnAction(e -> showPhotosDialog(photos));
                    setGraphic(btn);
                    setStyle("-fx-alignment: center;");
                }
            }
        });

        // Styling cells
        setStatutCellFactory(moneyStatutCol);
        setStatutCellFactory(materialStatutCol);
    }

    private void setStatutCellFactory(TableColumn<Don, String> col) {
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Don don = getTableRow().getItem();
                    if (don == null) return;

                    Label label = new Label(item.replace("_", " ").toUpperCase());
                    
                    if ("en_attente".equalsIgnoreCase(item) || "en attente".equalsIgnoreCase(item)) {
                        label.setStyle("-fx-text-fill: #059669; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #ecfdf5; -fx-padding: 3 10; -fx-background-radius: 10; -fx-cursor: hand;");
                        
                        label.setOnMouseEntered(e -> {
                            label.setText("CONFIRMER ?");
                            label.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #10b981; -fx-padding: 3 10; -fx-background-radius: 10; -fx-cursor: hand;");
                        });
                        
                        label.setOnMouseExited(e -> {
                            label.setText("EN ATTENTE");
                            label.setStyle("-fx-text-fill: #059669; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #ecfdf5; -fx-padding: 3 10; -fx-background-radius: 10; -fx-cursor: hand;");
                        });
                        
                        label.setOnMouseClicked(e -> {
                            if (donationService.updateDonStatut(don.getId(), "confirmé")) {
                                // Notify the donor
                                if (don.getUserId() > 0) {
                                    notificationService.createDonationConfirmed(
                                        don.getUserId(),
                                        don.getCauseNom() != null ? don.getCauseNom() : "une cause",
                                        don.getMontant() != null ? don.getMontant() : 0
                                    );
                                }
                                don.setStatut("confirmé");
                                getTableView().refresh();

                                label.setText("CONFIRMÉ");
                                label.setStyle("-fx-text-fill: #1e40af; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #dbeafe; -fx-padding: 3 10; -fx-background-radius: 10;");
                                label.setOnMouseEntered(null);
                                label.setOnMouseExited(null);
                                label.setOnMouseClicked(null);
                            }
                        });
                    } else if ("confirme".equalsIgnoreCase(item) || "confirmé".equalsIgnoreCase(item)) {
                        label.setStyle("-fx-text-fill: #1e40af; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #dbeafe; -fx-padding: 3 10; -fx-background-radius: 10;");
                    } else {
                        label.setStyle("-fx-text-fill: #059669; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #ecfdf5; -fx-padding: 3 10; -fx-background-radius: 10;");
                    }
                    
                    setGraphic(label);
                    setStyle("-fx-alignment: center;");
                }
            }
        });
    }

    private void loadDons() {
        System.out.println("DonListController: Chargement des dons en arrière-plan...");
        
        // On montre un indicateur de chargement ou un message si possible
        moneyTable.setPlaceholder(new Label("Chargement des données..."));
        materialTable.setPlaceholder(new Label("Chargement des données..."));

        Thread thread = new Thread(() -> {
            try {
                List<Don> dons = donationService.getAllDons();
                Platform.runLater(() -> {
                    System.out.println("DonListController: " + (dons != null ? dons.size() : 0) + " dons récupérés.");
                    if (dons != null) {
                        allDons.setAll(dons);
                        updateTables(allDons);
                        
                        if (dons.isEmpty()) {
                            moneyTable.setPlaceholder(new Label("Aucun don d'argent trouvé."));
                            materialTable.setPlaceholder(new Label("Aucun don matériel trouvé."));
                        }
                    } else {
                        moneyTable.setPlaceholder(new Label("Erreur lors du chargement des données."));
                        materialTable.setPlaceholder(new Label("Erreur lors du chargement des données."));
                    }
                });
            } catch (Exception e) {
                System.err.println("DonListController: Erreur fatale lors du chargement :");
                e.printStackTrace();
                Platform.runLater(() -> {
                    moneyTable.setPlaceholder(new Label("Erreur: " + e.getMessage()));
                    materialTable.setPlaceholder(new Label("Erreur: " + e.getMessage()));
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void updateTables(List<Don> list) {
        System.out.println("DonListController: Mise à jour des tables avec " + list.size() + " éléments.");
        List<Don> moneyDons = list.stream().filter(d -> "argent".equals(d.getType())).collect(Collectors.toList());
        List<Don> materialDons = list.stream().filter(d -> "materiel".equals(d.getType())).collect(Collectors.toList());
        
        System.out.println("DonListController: Argent=" + moneyDons.size() + ", Matériel=" + materialDons.size());
        
        moneyTable.setItems(FXCollections.observableArrayList(moneyDons));
        materialTable.setItems(FXCollections.observableArrayList(materialDons));
        
        moneyCountLabel.setText(moneyDons.size() + " dons");
        materialCountLabel.setText(materialDons.size() + " dons");
    }

    private void showPhotosDialog(List<String> photos) {
        List<String> validPaths = photos.stream()
                .filter(p -> p != null && new File(p).exists())
                .collect(java.util.stream.Collectors.toList());

        if (validPaths.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Aucune photo disponible pour ce don.").showAndWait();
            return;
        }

        // ── State ──────────────────────────────────────────────────────
        int[] idx = {0};

        // ── Image view ────────────────────────────────────────────────
        ImageView imageView = new ImageView();
        imageView.setFitWidth(520);
        imageView.setFitHeight(380);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-color: #000;");

        // ── Counter label ─────────────────────────────────────────────
        Label counter = new Label();
        counter.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // ── Gemini AI result label ────────────────────────────────────
        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // ── Load image at index ───────────────────────────────────────
        Runnable loadImage = () -> {
            try {
                imageView.setImage(new Image(new File(validPaths.get(idx[0])).toURI().toString()));
            } catch (Exception ignored) {}
            counter.setText((idx[0] + 1) + " / " + validPaths.size());
            resultLabel.setText("");
        };
        loadImage.run();

        // ── Nav buttons ───────────────────────────────────────────────
        Button btnPrev = new Button("◀");
        Button btnNext = new Button("▶");
        String navStyle = "-fx-background-color: #f1f5f9; -fx-font-size: 14px; " +
                "-fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand;";
        btnPrev.setStyle(navStyle);
        btnNext.setStyle(navStyle);

        btnPrev.setOnAction(e -> {
            if (idx[0] > 0) { idx[0]--; loadImage.run(); }
        });
        btnNext.setOnAction(e -> {
            if (idx[0] < validPaths.size() - 1) { idx[0]++; loadImage.run(); }
        });

        btnPrev.setDisable(validPaths.size() == 1);
        btnNext.setDisable(validPaths.size() == 1);

        // ── Gemini verify button ──────────────────────────────────────
        Button verifyBtn = new Button("🔍 Vérifier l'état (IA)");
        verifyBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 7 16; -fx-cursor: hand;");
        verifyBtn.setOnAction(e -> {
            verifyBtn.setDisable(true);
            resultLabel.setText("Analyse en cours...");
            resultLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
            String path = validPaths.get(idx[0]);
            new Thread(() -> {
                try {
                    String result = geminiService.analyzeImageCondition(path);
                    Platform.runLater(() -> {
                        displayAnalysisResult(result, resultLabel);
                        verifyBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        resultLabel.setText("Erreur d'analyse");
                        resultLabel.setStyle("-fx-text-fill: #ef4444;");
                        verifyBtn.setDisable(false);
                    });
                }
            }).start();
        });

        // ── Layout ────────────────────────────────────────────────────
        javafx.scene.layout.StackPane imageWrap = new javafx.scene.layout.StackPane(imageView);
        imageWrap.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 10;");
        imageWrap.setPadding(new Insets(8));

        javafx.scene.layout.HBox navRow = new javafx.scene.layout.HBox(20, btnPrev, counter, btnNext);
        navRow.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(12, verifyBtn, resultLabel);
        actionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox content = new VBox(14, imageWrap, navRow, actionRow);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");
        content.setPrefWidth(580);

        // ── Dialog ────────────────────────────────────────────────────
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Photos du don");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");
        dialog.showAndWait();
    }

    private void displayAnalysisResult(String result, Label label) {
        String color;
        String emoji;
        String text;
        
        // Normalisation de la réponse
        String normalized = result.toLowerCase()
                .replace("*", "")
                .replace("\"", "")
                .trim();

        if (normalized.contains("très bon état")) {
            color = "#22c55e"; // vert
            emoji = "🟢";
            text = "Très bon état";
        } else if (normalized.contains("bon état")) {
            color = "#3b82f6"; // bleu
            emoji = "🔵";
            text = "Bon état";
        } else if (normalized.contains("en état")) {
            color = "#f59e0b"; // orange/jaune
            emoji = "🟠";
            text = "En état";
        } else if (normalized.contains("mauvais état")) {
            color = "#ef4444"; // rouge
            emoji = "🔴";
            text = "Mauvais état";
        } else if (normalized.contains("indéterminé")) {
            color = "#64748b"; // gris
            emoji = "⚪";
            text = "Indéterminé";
        } else if (normalized.contains("délai dépassé")) {
            color = "#f59e0b"; // orange
            emoji = "⏳";
            text = "Délai dépassé (Réessayez)";
        } else if (normalized.contains("connexion")) {
            color = "#ef4444"; // rouge
            emoji = "🌐";
            text = "Erreur de connexion";
        } else {
            // C'est probablement une erreur (Erreur API, Erreur Analyse, etc.)
            color = "#ef4444"; // rouge pour l'erreur
            emoji = "⚠️";
            text = result; // On affiche le message d'erreur brut
        }
        
        label.setText(emoji + " " + text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private void filterDons(String query) {
        if (query == null || query.isEmpty()) {
            updateTables(allDons);
            return;
        }
        String q = query.toLowerCase();
        List<Don> filtered = allDons.stream()
                .filter(d -> (d.getCauseNom() != null && d.getCauseNom().toLowerCase().contains(q)) ||
                             (d.getDonateurNom() != null && d.getDonateurNom().toLowerCase().contains(q)) ||
                             (d.getDonateurEmail() != null && d.getDonateurEmail().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        updateTables(filtered);
    }
}
