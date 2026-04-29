package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.Commande;
import com.medicare.models.Produit;
import com.medicare.models.User;
import com.medicare.services.CommandeService;
import com.medicare.services.ProduitService;
import com.medicare.utils.Validators;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Patient catalogue: browse active products, order them via "Commander".
 */
public class PatientProduitsController {

    @FXML private VBox container;

    private final ProduitService produitService = new ProduitService();
    private final CommandeService commandeService = new CommandeService();

    @FXML
    private void initialize() {
        loadCatalogue();
    }

    private void loadCatalogue() {
        container.getChildren().clear();

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Nos produits");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher un produit...");
        searchField.setPrefWidth(240);
        searchField.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        searchField.textProperty().addListener((obs, old, val) -> filter(val));

        header.getChildren().addAll(title, spacer, searchField);
        container.getChildren().add(header);

        renderGrid(produitService.getActive());
    }

    private void filter(String search) {
        if (search == null || search.trim().isEmpty()) {
            renderGrid(produitService.getActive());
        } else {
            List<Produit> filtered = produitService.search(search)
                    .stream().filter(Produit::isActive).toList();
            renderGrid(filtered);
        }
    }

    private void renderGrid(List<Produit> produits) {
        while (container.getChildren().size() > 1) {
            container.getChildren().remove(1);
        }

        if (produits.isEmpty()) {
            Label empty = new Label("Aucun produit disponible.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        FlowPane grid = new FlowPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPadding(new Insets(10, 0, 10, 0));

        for (Produit p : produits) grid.getChildren().add(buildCard(p));

        container.getChildren().add(grid);
    }

    private VBox buildCard(Produit p) {
        VBox card = new VBox(10);
        card.setPrefWidth(240);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                      "-fx-border-color: #e5e7eb; -fx-border-radius: 14; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        StackPane imageBox = new StackPane();
        imageBox.setPrefSize(204, 130);
        imageBox.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 10;");
        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(204);
            iv.setFitHeight(130);
            iv.setPreserveRatio(true);
            try {
                iv.setImage(new Image(p.getImageUrl(), 204, 130, true, true, true));
            } catch (Exception ignored) {}
            imageBox.getChildren().add(iv);
        } else {
            FontIcon ph = new FontIcon(FontAwesomeSolid.CAPSULES);
            ph.setIconSize(48);
            ph.setIconColor(Color.web("#1a73e8"));
            imageBox.getChildren().add(ph);
        }

        Label name = new Label(p.getName());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");
        name.setWrapText(true);

        Label type = new Label((p.getType() != null ? p.getType() : "") +
                (p.getDosage() != null && !p.getDosage().isBlank() ? " · " + p.getDosage() : ""));
        type.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        Label desc = new Label(p.getDescription() != null ? p.getDescription() : "");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        desc.setMaxHeight(50);

        HBox info = new HBox(10);
        info.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label((p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO) + " DT");
        price.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label stock = new Label("Stock: " + p.getQuantity());
        stock.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (p.getQuantity() > 0 ? "#555" : "#dc2626") + ";");
        info.getChildren().addAll(price, sp, stock);

        Button btnOrder = new Button("  Commander");
        FontIcon cartIcon = new FontIcon(FontAwesomeSolid.SHOPPING_CART);
        cartIcon.setIconSize(13);
        cartIcon.setIconColor(Color.WHITE);
        btnOrder.setGraphic(cartIcon);
        btnOrder.setMaxWidth(Double.MAX_VALUE);
        btnOrder.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 13px; " +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8;");
        btnOrder.setDisable(p.getQuantity() <= 0);
        btnOrder.setOnAction(e -> showCommandeForm(p));

        card.getChildren().addAll(imageBox, name, type, desc, info, btnOrder);
        return card;
    }

    // ========== COMMANDE FORM ==========

    private void showCommandeForm(Produit product) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        Label titleLbl = new Label("Commander : " + product.getName());
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label priceLbl = new Label("Prix unitaire : " + product.getPrice() + " DT   ·   Stock : " + product.getQuantity());
        priceLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        TextField qtyField = styledField("Quantite");
        qtyField.setText("1");

        TextField totalField = styledField("Total (DT)");
        totalField.setEditable(false);
        totalField.setStyle(totalField.getStyle() + "-fx-background-color: #f3f4f6;");

        DatePicker delivDatePicker = new DatePicker();
        delivDatePicker.setPromptText("Date de livraison souhaitee");
        delivDatePicker.setPrefHeight(36);
        delivDatePicker.setMaxWidth(Double.MAX_VALUE);
        delivDatePicker.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes (optionnel)");
        notesArea.setPrefRowCount(2);
        notesArea.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");

        Runnable recompute = () -> {
            if (Validators.isStrictlyPositiveInt(qtyField.getText())) {
                int q = Validators.parseInt(qtyField.getText());
                BigDecimal total = (product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(q));
                totalField.setText(total.toString());
            } else {
                totalField.setText("");
            }
        };
        qtyField.textProperty().addListener((obs, o, n) -> recompute.run());
        recompute.run();

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errorLbl.setWrapText(true);

        Button btnSave = new Button("Confirmer la commande");
        btnSave.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 13px; " +
                         "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 24;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; " +
                           "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 24;");
        btnCancel.setOnAction(e -> popup.close());

        btnSave.setOnAction(e -> {
            Validators.markValid(qtyField);
            errorLbl.setText("");

            // === Controle de saisie ===
            if (!Validators.isStrictlyPositiveInt(qtyField.getText())) {
                Validators.markInvalid(qtyField);
                errorLbl.setText("Quantite invalide (entier > 0).");
                return;
            }
            int qty = Validators.parseInt(qtyField.getText());
            if (qty > product.getQuantity()) {
                Validators.markInvalid(qtyField);
                errorLbl.setText("Stock insuffisant. Disponible : " + product.getQuantity());
                return;
            }
            if (delivDatePicker.getValue() != null && !Validators.isStrictlyFuture(delivDatePicker.getValue())) {
                errorLbl.setText("La date de livraison doit etre dans le futur.");
                return;
            }

            User cu = DashboardPatientController.getCurrentUser();
            if (cu == null) {
                errorLbl.setText("Session expiree. Veuillez vous reconnecter.");
                return;
            }

            Commande c = new Commande();
            c.setProductId(product.getId());
            c.setUserId(cu.getId());
            c.setQuantity(qty);
            c.setTotalPrice((product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(qty)));
            c.setStatus("en_attente_paiement");
            c.setNotes(notesArea.getText());
            c.setCommandeDate(LocalDateTime.now());
            c.setCreatedAt(LocalDateTime.now());
            if (delivDatePicker.getValue() != null) c.setDeliveryDate(delivDatePicker.getValue().atStartOfDay());

            commandeService.add(c);
            popup.close();
            openStripeCheckout(c, product);
        });

        HBox btns = new HBox(12, btnCancel, btnSave);
        btns.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(labeled("Quantite *", qtyField), 0, 0);
        grid.add(labeled("Total", totalField), 1, 0);
        grid.add(labeled("Livraison souhaitee", delivDatePicker), 0, 1, 2, 1);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        VBox box = new VBox(14, titleLbl, priceLbl, new Separator(),
                grid, labeled("Notes", notesArea), errorLbl, btns);
        box.setPadding(new Insets(25));
        box.setPrefWidth(520);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box));
        popup.showAndWait();
        loadCatalogue();
    }

    // ========== POPUP ==========

    private void showSuccess(String title, String message) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
        icon.setIconSize(48);
        icon.setIconColor(Color.web("#16a34a"));

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label m = new Label(message);
        m.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        m.setWrapText(true);

        VBox box = new VBox(14, icon, t, m);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");
        popup.setScene(new Scene(box, 380, 240));
        popup.show();

        PauseTransition p = new PauseTransition(Duration.seconds(1.6));
        p.setOnFinished(e -> popup.close());
        p.play();
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(36);
        tf.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        return tf;
    }

    private VBox labeled(String label, javafx.scene.Node field) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #555;");
        return new VBox(4, l, field);
    }

    // ========== STRIPE ==========

    private void openStripeCheckout(Commande c, Produit product) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("stripe-checkout-view.fxml"));
            Parent root = loader.load();
            StripeCheckoutController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(container.getScene().getWindow());
            stage.setTitle("Paiement Medicare");
            stage.setScene(new Scene(root, 820, 760));
            ctrl.start(stage, c, product, paid -> {
                if (paid) {
                    showSuccess("Paiement reussi",
                            product.getName() + " x" + c.getQuantity() + " — commande confirmee.");
                } else {
                    showSuccess("Paiement non finalise",
                            "Tu peux payer plus tard depuis 'Mes commandes'.");
                }
                loadCatalogue();
            });
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            showSuccess("Erreur",
                    "Impossible d'ouvrir le paiement : " + ex.getMessage());
        }
    }
}
