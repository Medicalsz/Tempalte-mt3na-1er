# 🏥 Medicare — Project Blueprint
> JavaFX + Maven | University Project | 6 Métiers Avancées

---

## 📁 Project Structure

```
medicare/
├── src/main/java/com/medicare/
│   ├── controllers/         # JavaFX controllers
│   ├── models/              # User, Medecin, Appointment...
│   ├── services/            # GoogleAuth, AI, GeoService...
│   ├── dao/                 # DB access objects
│   └── utils/               # Helpers
├── src/main/resources/
│   ├── fxml/                # All .fxml views
│   └── css/                 # Stylesheets
├── pom.xml
└── README.md
```

---

## 🗄️ Database — `medicare` (phpMyAdmin)

---

### 🔴 TABLE: `user` — Full Redesign

#### ❌ Remove These Columns
| Column | Reason |
|--------|--------|
| `online_duration` | Not useful for a medical app |
| `email_privacy` | Over-engineered, simplify with one `privacy_level` |
| `phone_privacy` | Same as above |
| `address_privacy` | Same as above |
| `is_private` | Redundant with roles/privacy_level |

---

#### ✅ Keep These Columns
| # | Name | Type | Null | Default | Notes |
|---|------|------|------|---------|-------|
| 1 | `id` | INT(11) | No | — | AUTO_INCREMENT, PRIMARY KEY |
| 2 | `nom` | VARCHAR(100) | No | — | |
| 3 | `prenom` | VARCHAR(100) | No | — | |
| 4 | `username` | VARCHAR(100) | No | — | UNIQUE |
| 5 | `email` | VARCHAR(180) | No | — | UNIQUE |
| 6 | `password` | VARCHAR(255) | Yes | NULL | NULL if Google Auth user |
| 7 | `numero` | VARCHAR(20) | Yes | NULL | |
| 8 | `adresse` | VARCHAR(255) | Yes | NULL | |
| 9 | `photo` | VARCHAR(255) | Yes | NULL | |
| 10 | `is_verified` | TINYINT(1) | No | 0 | |
| 11 | `roles` | VARCHAR(50) | No | 'ROLE_USER' | Changed from LONGTEXT |
| 12 | `date_naissance` | DATE | Yes | NULL | |
| 13 | `wants_email_notifications` | TINYINT(1) | No | 1 | |
| 14 | `password_reset_token` | VARCHAR(255) | Yes | NULL | |
| 15 | `password_reset_token_expiry` | DATETIME | Yes | NULL | |
| 16 | `last_login_at` | DATETIME | Yes | NULL | |

---

#### ➕ Add These New Columns
| # | Name | Type | Null | Default | Why |
|---|------|------|------|---------|-----|
| 17 | `google_id` | VARCHAR(255) | Yes | NULL | 🔐 Google OAuth — store Google UID |
| 18 | `google_access_token` | TEXT | Yes | NULL | 🔐 Google OAuth token |
| 19 | `latitude` | DOUBLE | Yes | NULL | 📍 Nearest Doctor feature |
| 20 | `longitude` | DOUBLE | Yes | NULL | 📍 Nearest Doctor feature |
| 21 | `city` | VARCHAR(100) | Yes | NULL | 📍 User city for matching |
| 22 | `gender` | ENUM('male','female','other') | Yes | NULL | 🧑 Profile completion |
| 23 | `blood_type` | ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') | Yes | NULL | 💊 Medical info |
| 24 | `allergies` | TEXT | Yes | NULL | 💊 Medical info |
| 25 | `profile_completed` | TINYINT(1) | No | 0 | ✅ Forces profile completion |
| 26 | `privacy_level` | ENUM('public','friends','private') | No | 'public' | Replaces 3 privacy columns |
| 27 | `created_at` | DATETIME | No | CURRENT_TIMESTAMP | ⏱ Audit |
| 28 | `updated_at` | DATETIME | Yes | NULL | ⏱ Audit |

---

#### 🛠️ SQL to Run in phpMyAdmin → SQL Tab

```sql
-- Remove old privacy columns
ALTER TABLE user
  DROP COLUMN email_privacy,
  DROP COLUMN phone_privacy,
  DROP COLUMN address_privacy,
  DROP COLUMN is_private,
  DROP COLUMN online_duration;

-- Change roles to simple VARCHAR
ALTER TABLE user MODIFY COLUMN roles VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER';

-- Add new columns
ALTER TABLE user
  ADD COLUMN google_id VARCHAR(255) NULL AFTER last_login_at,
  ADD COLUMN google_access_token TEXT NULL AFTER google_id,
  ADD COLUMN latitude DOUBLE NULL AFTER google_access_token,
  ADD COLUMN longitude DOUBLE NULL AFTER latitude,
  ADD COLUMN city VARCHAR(100) NULL AFTER longitude,
  ADD COLUMN gender ENUM('male','female','other') NULL AFTER city,
  ADD COLUMN blood_type ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') NULL AFTER gender,
  ADD COLUMN allergies TEXT NULL AFTER blood_type,
  ADD COLUMN profile_completed TINYINT(1) NOT NULL DEFAULT 0 AFTER allergies,
  ADD COLUMN privacy_level ENUM('public','friends','private') NOT NULL DEFAULT 'public' AFTER profile_completed,
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER privacy_level,
  ADD COLUMN updated_at DATETIME NULL AFTER created_at;

-- Allow NULL password for Google users
ALTER TABLE user MODIFY COLUMN password VARCHAR(255) NULL;
```

---

### 🔴 TABLE: `medecin` — Full Redesign

#### ❌ Remove These Columns
| Column | Reason |
|--------|--------|
| `online_duration` | Not relevant |
| `email_privacy` | Replace with single `privacy_level` |
| `phone_privacy` | Same |
| `address_privacy` | Same |

---

#### ✅ Keep These Columns
| # | Name | Type | Null | Default | Notes |
|---|------|------|------|---------|-------|
| 1 | `id` | INT(11) | No | — | AUTO_INCREMENT, PRIMARY KEY |
| 2 | `nom` | VARCHAR(100) | No | — | |
| 3 | `prenom` | VARCHAR(100) | No | — | |
| 4 | `username` | VARCHAR(100) | Yes | NULL | UNIQUE |
| 5 | `email` | VARCHAR(180) | No | — | UNIQUE |
| 6 | `password` | VARCHAR(255) | Yes | NULL | NULL if Google Auth |
| 7 | `numero` | VARCHAR(20) | Yes | NULL | |
| 8 | `adresse` | VARCHAR(255) | Yes | NULL | |
| 9 | `specialite` | VARCHAR(255) | No | — | |
| 10 | `cabinet` | VARCHAR(255) | No | — | Cabinet / clinic name |
| 11 | `bio` | LONGTEXT | Yes | NULL | |
| 12 | `ville` | VARCHAR(100) | Yes | NULL | |
| 13 | `prixConsultation` | DOUBLE | Yes | NULL | |
| 14 | `photo` | VARCHAR(255) | Yes | NULL | |
| 15 | `certificate` | VARCHAR(255) | Yes | NULL | Doctor diploma file |
| 16 | `isVerified` | TINYINT(1) | No | 0 | Admin must verify |
| 17 | `date_naissance` | DATE | Yes | NULL | |
| 18 | `wants_email_notifications` | TINYINT(1) | No | 1 | |
| 19 | `rank` | INT(11) | No | 0 | |
| 20 | `last_login_at` | DATETIME | Yes | NULL | |
| 21 | `profile_views` | INT(11) | No | 0 | |

---

#### ➕ Add These New Columns
| # | Name | Type | Null | Default | Why |
|---|------|------|------|---------|-----|
| 22 | `google_id` | VARCHAR(255) | Yes | NULL | 🔐 Google OAuth |
| 23 | `google_access_token` | TEXT | Yes | NULL | 🔐 Google OAuth |
| 24 | `latitude` | DOUBLE | Yes | NULL | 📍 **CRITICAL** — Nearest Doctor |
| 25 | `longitude` | DOUBLE | Yes | NULL | 📍 **CRITICAL** — Nearest Doctor |
| 26 | `experience_years` | INT(3) | Yes | NULL | 👨‍⚕️ Doctor profile quality |
| 27 | `consultation_duration` | INT(11) | No | 30 | ⏱ Minutes per appointment |
| 28 | `is_available_online` | TINYINT(1) | No | 0 | 💻 Teleconsultation |
| 29 | `rating_average` | DECIMAL(3,2) | No | 0.00 | ⭐ Calculated from reviews |
| 30 | `rating_count` | INT(11) | No | 0 | ⭐ Number of ratings |
| 31 | `disponibilite` | TEXT | Yes | NULL | 📅 JSON: available slots |
| 32 | `languages` | VARCHAR(255) | Yes | NULL | 🌍 e.g. "French,Arabic,English" |
| 33 | `gender` | ENUM('male','female') | Yes | NULL | 🧑 Doctor gender |
| 34 | `privacy_level` | ENUM('public','friends','private') | No | 'public' | Replaces 3 privacy columns |
| 35 | `profile_completed` | TINYINT(1) | No | 0 | ✅ Profile completion |
| 36 | `created_at` | DATETIME | No | CURRENT_TIMESTAMP | ⏱ Audit |
| 37 | `updated_at` | DATETIME | Yes | NULL | ⏱ Audit |

---

#### 🛠️ SQL to Run in phpMyAdmin → SQL Tab

```sql
-- Remove old columns
ALTER TABLE medecin
  DROP COLUMN email_privacy,
  DROP COLUMN phone_privacy,
  DROP COLUMN address_privacy,
  DROP COLUMN online_duration;

-- Allow NULL password for Google users
ALTER TABLE medecin MODIFY COLUMN password VARCHAR(255) NULL;

-- Add new columns
ALTER TABLE medecin
  ADD COLUMN google_id VARCHAR(255) NULL AFTER last_login_at,
  ADD COLUMN google_access_token TEXT NULL AFTER google_id,
  ADD COLUMN latitude DOUBLE NULL AFTER google_access_token,
  ADD COLUMN longitude DOUBLE NULL AFTER latitude,
  ADD COLUMN experience_years INT(3) NULL AFTER longitude,
  ADD COLUMN consultation_duration INT(11) NOT NULL DEFAULT 30 AFTER experience_years,
  ADD COLUMN is_available_online TINYINT(1) NOT NULL DEFAULT 0 AFTER consultation_duration,
  ADD COLUMN rating_average DECIMAL(3,2) NOT NULL DEFAULT 0.00 AFTER is_available_online,
  ADD COLUMN rating_count INT(11) NOT NULL DEFAULT 0 AFTER rating_average,
  ADD COLUMN disponibilite TEXT NULL AFTER rating_count,
  ADD COLUMN languages VARCHAR(255) NULL AFTER disponibilite,
  ADD COLUMN gender ENUM('male','female') NULL AFTER languages,
  ADD COLUMN privacy_level ENUM('public','friends','private') NOT NULL DEFAULT 'public' AFTER gender,
  ADD COLUMN profile_completed TINYINT(1) NOT NULL DEFAULT 0 AFTER privacy_level,
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER profile_completed,
  ADD COLUMN updated_at DATETIME NULL AFTER created_at;
```

---

### 🆕 NEW TABLES TO CREATE

#### TABLE: `rendez_vous` (Appointments)

```sql
CREATE TABLE rendez_vous (
  id              INT(11) NOT NULL AUTO_INCREMENT,
  user_id         INT(11) NOT NULL,
  medecin_id      INT(11) NOT NULL,
  date_rdv        DATETIME NOT NULL,
  duree_minutes   INT(11) NOT NULL DEFAULT 30,
  motif           VARCHAR(255) NULL,
  statut          ENUM('pending','confirmed','cancelled','completed') NOT NULL DEFAULT 'pending',
  type            ENUM('presential','online') NOT NULL DEFAULT 'presential',
  notes_patient   TEXT NULL,
  notes_medecin   TEXT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id)    REFERENCES user(id)    ON DELETE CASCADE,
  FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE
);
```

---

#### TABLE: `medicament` (Medications)

```sql
CREATE TABLE medicament (
  id              INT(11) NOT NULL AUTO_INCREMENT,
  user_id         INT(11) NOT NULL,
  nom             VARCHAR(255) NOT NULL,
  dosage          VARCHAR(100) NULL,
  frequence       VARCHAR(100) NULL,           -- e.g. "2 fois par jour"
  heure_prise     VARCHAR(100) NULL,           -- e.g. "08:00,20:00"
  date_debut      DATE NULL,
  date_fin        DATE NULL,
  stock_actuel    INT(11) NOT NULL DEFAULT 0,
  stock_minimum   INT(11) NOT NULL DEFAULT 5,  -- Alert threshold
  est_actif       TINYINT(1) NOT NULL DEFAULT 1,
  notes           TEXT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
```

---

#### TABLE: `avis` (Reviews / Ratings)

```sql
CREATE TABLE avis (
  id              INT(11) NOT NULL AUTO_INCREMENT,
  user_id         INT(11) NOT NULL,
  medecin_id      INT(11) NOT NULL,
  note            TINYINT(1) NOT NULL,         -- 1 to 5
  commentaire     TEXT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id)    REFERENCES user(id)    ON DELETE CASCADE,
  FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE
);
```

---

#### TABLE: `notification`

```sql
CREATE TABLE notification (
  id              INT(11) NOT NULL AUTO_INCREMENT,
  user_id         INT(11) NULL,
  medecin_id      INT(11) NULL,
  type            ENUM('appointment','medication','system','reminder') NOT NULL,
  titre           VARCHAR(255) NOT NULL,
  message         TEXT NOT NULL,
  is_read         TINYINT(1) NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
```

---

## 🎨 Profile Completion Onboarding — Full UI + Smart Alerts

### Overview

When any user registers (normal or Google Auth), `profile_completed = 0`.
On every login, the app checks the completion percentage and reacts accordingly.
The user can **skip** at any time but will see a persistent reminder each login until they hit **50%**.

---

### UI Design — 6 Large Tile Buttons

The onboarding screen uses **6 large rounded-square tiles**, each representing one section.
Colors follow a warm hospital palette: deep pink, saturated green, orange, gray.

#### Color Palette

| Token | Hex | Used For |
|-------|-----|----------|
| Deep Pink | `#C2185B` | Personal info, Contact tiles |
| Saturated Green | `#43A047` | Medical info tile |
| Orange | `#F57C00` | Location, Notifications tiles |
| Gray | `#546E7A` | Photo tile |
| Light Pink BG | `#FCE4EC` | Pink tile background |
| Light Green BG | `#E8F5E9` | Green tile background |
| Light Orange BG | `#FFF3E0` | Orange tile background |
| Light Gray BG | `#ECEFF1` | Gray tile background |

#### FXML — `complete-profile.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.scene.shape.*?>

<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.medicare.controllers.CompleteProfileController"
      spacing="16" style="-fx-padding: 24; -fx-background-color: #FAFAFA;">

    <!-- Header -->
    <HBox spacing="6" alignment="CENTER_LEFT">
        <Circle radius="5" fill="#C2185B"/>
        <Circle radius="5" fill="#43A047"/>
        <Circle radius="5" fill="#F57C00"/>
        <Label text="Medicare — Complete your profile"
               style="-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;"/>
    </HBox>

    <!-- Smart Alert Banner (dynamic, injected by controller) -->
    <VBox fx:id="alertZone" spacing="0"/>

    <!-- Progress Bar -->
    <VBox spacing="4">
        <HBox>
            <Label text="Profile strength" style="-fx-text-fill: #888; -fx-font-size: 12;"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Label fx:id="pctLabel" text="0%"
                   style="-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #C2185B;"/>
        </HBox>
        <ProgressBar fx:id="progressBar" progress="0" prefWidth="1000"
                     style="-fx-accent: #C2185B; -fx-pref-height: 8; -fx-background-radius: 4;"/>
    </VBox>

    <Label text="TAP A SECTION TO COMPLETE"
           style="-fx-font-size: 10; -fx-text-fill: #999; -fx-letter-spacing: 1;"/>

    <!-- 3x2 Tile Grid -->
    <GridPane hgap="10" vgap="10">
        <!-- Row 0 -->
        <Button fx:id="tilePersonal" GridPane.columnIndex="0" GridPane.rowIndex="0"
                text="👤 Personal Info" onAction="#openPersonal"
                prefWidth="140" prefHeight="140"
                style="-fx-background-color: #FCE4EC; -fx-border-color: #F48FB1;
                       -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;
                       -fx-text-fill: #C2185B; -fx-font-size: 13; -fx-font-weight: bold;
                       -fx-cursor: hand;"/>
        <Button fx:id="tileMedical" GridPane.columnIndex="1" GridPane.rowIndex="0"
                text="🩺 Medical Info" onAction="#openMedical"
                prefWidth="140" prefHeight="140"
                style="-fx-background-color: #E8F5E9; -fx-border-color: #A5D6A7;
                       -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;
                       -fx-text-fill: #2E7D32; -fx-font-size: 13; -fx-font-weight: bold;
                       -fx-cursor: hand;"/>
        <Button fx:id="tileLocation" GridPane.columnIndex="2" GridPane.rowIndex="0"
                text="📍 Location" onAction="#openLocation"
                prefWidth="140" prefHeight="140"
                style="-fx-background-color: #FFF3E0; -fx-border-color: #FFCC80;
                       -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;
                       -fx-text-fill: #E65100; -fx-font-size: 13; -fx-font-weight: bold;
                       -fx-cursor: hand;"/>
        <!-- Row 1 -->
        <Button fx:id="tileContact" GridPane.columnIndex="0" GridPane.rowIndex="1"
                text="📞 Contact" onAction="#openContact"
                prefWidth="140" prefHeight="140"
                style="-fx-background-color: #FCE4EC; -fx-border-color: #F48FB1;
                       -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;
                       -fx-text-fill: #C2185B; -fx-font-size: 13; -fx-font-weight: bold;
                       -fx-cursor: hand;"/>
        <Button fx:id="tilePhoto" GridPane.columnIndex="1" GridPane.rowIndex="1"
                text="📷 Photo" onAction="#openPhoto"
                prefWidth="140" prefHeight="140"
                style="-fx-background-color: #ECEFF1; -fx-border-color: #B0BEC5;
                       -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;
                       -fx-text-fill: #546E7A; -fx-font-size: 13; -fx-font-weight: bold;
                       -fx-cursor: hand;"/>
        <Button fx:id="tileNotif" GridPane.columnIndex="2" GridPane.rowIndex="1"
                text="🔔 Notifications" onAction="#openNotif"
                prefWidth="140" prefHeight="140"
                style="-fx-background-color: #FFF3E0; -fx-border-color: #FFCC80;
                       -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;
                       -fx-text-fill: #E65100; -fx-font-size: 13; -fx-font-weight: bold;
                       -fx-cursor: hand;"/>
    </GridPane>

    <!-- Footer Row -->
    <HBox alignment="CENTER_LEFT" spacing="16">
        <Hyperlink fx:id="skipLink" text="Skip for now" onAction="#skipProfile"
                   style="-fx-text-fill: #999; -fx-font-size: 12;"/>
        <Region HBox.hgrow="ALWAYS"/>
        <Button fx:id="goBtn" text="Go to dashboard →" onAction="#goDashboard"
                visible="false"
                style="-fx-background-color: #43A047; -fx-text-fill: white;
                       -fx-background-radius: 20; -fx-font-size: 13; -fx-font-weight: bold;
                       -fx-padding: 8 20; -fx-cursor: hand;"/>
    </HBox>

</VBox>
```

---

### Smart Alert System — `ProfileAlertService.java`

The alert engine has 5 levels. Each level maps to a specific color and message.
Alerts re-appear automatically if dismissed while the profile is still below 50%.

```java
package com.medicare.services;

import com.medicare.models.User;

public class ProfileAlertService {

    // --- Weight of each section (must sum to 100) ---
    public static int getCompletionPercent(User user) {
        int score = 0;
        if (user.getNom()           != null && !user.getNom().isEmpty())    score += 17;
        if (user.getBloodType()     != null)                                score += 17;
        if (user.getCity()          != null && !user.getCity().isEmpty())   score += 17;
        if (user.getNumero()        != null && !user.getNumero().isEmpty()) score += 17;
        if (user.getPhoto()         != null && !user.getPhoto().isEmpty())  score += 16;
        if (user.isWantsEmailNotifications())                               score += 16;
        return score;
    }

    public enum AlertLevel { CRITICAL, HIGH, MEDIUM, LOW, NONE }

    public record SmartAlert(AlertLevel level, String title, String message, String color) {}

    // Called on every login and after each dismiss
    public static SmartAlert getSmartAlert(User user) {
        int pct = getCompletionPercent(user);

        if (pct == 0) return new SmartAlert(AlertLevel.NONE, null, null, null);

        if (pct < 30)
            return new SmartAlert(AlertLevel.CRITICAL,
                "Profile incomplete — features locked",
                "Complete at least 3 sections to unlock appointment booking.",
                "#FFEBEE");  // red bg

        if (pct < 50 && user.getBloodType() == null)
            return new SmartAlert(AlertLevel.HIGH,
                "Missing medical info",
                "Without blood type and allergies, doctors can't prepare for emergencies.",
                "#FFF3E0");  // orange bg

        if (pct < 50 && (user.getCity() == null || user.getCity().isEmpty()))
            return new SmartAlert(AlertLevel.HIGH,
                "No location set",
                "The nearest doctor finder needs your city. Takes 10 seconds.",
                "#FFF3E0");  // orange bg

        if (pct < 50)
            return new SmartAlert(AlertLevel.MEDIUM,
                "Profile below 50%",
                "You're almost there — complete one more section to unlock all features.",
                "#FCE4EC");  // pink bg

        if (!user.isWantsEmailNotifications())
            return new SmartAlert(AlertLevel.LOW,
                "Notifications are off",
                "Enable reminders so you never miss an appointment or medication.",
                "#FFF3E0");  // orange bg

        return new SmartAlert(AlertLevel.NONE, null, null, null);
    }
}
```

---

### Controller — `CompleteProfileController.java`

```java
package com.medicare.controllers;

import com.medicare.models.User;
import com.medicare.services.ProfileAlertService;
import com.medicare.services.ProfileAlertService.*;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class CompleteProfileController {

    @FXML private VBox       alertZone;
    @FXML private ProgressBar progressBar;
    @FXML private Label      pctLabel;
    @FXML private Button     goBtn;
    @FXML private Button     tilePersonal, tileMedical, tileLocation,
                             tileContact,  tilePhoto,   tileNotif;

    private User currentUser;
    private int  dismissCount = 0;

    @FXML
    public void initialize() {
        currentUser = Session.getCurrentUser();
        refreshProgress();
        scheduleAlert();  // show first alert after 1.5s
    }

    // --- Progress ---
    private void refreshProgress() {
        int pct = ProfileAlertService.getCompletionPercent(currentUser);

        progressBar.setProgress(pct / 100.0);
        pctLabel.setText(pct + "%");

        // Color the bar based on level
        String barColor = pct < 33 ? "#C2185B" : pct < 66 ? "#F57C00" : "#43A047";
        progressBar.setStyle("-fx-accent: " + barColor + ";");
        pctLabel.setStyle("-fx-text-fill: " + barColor + "; -fx-font-weight: bold; -fx-font-size: 16;");

        // Show dashboard button only at 50%+
        goBtn.setVisible(pct >= 50);

        // Mark completed tiles
        markTile(tilePersonal, currentUser.getNom() != null, "#C2185B");
        markTile(tileMedical,  currentUser.getBloodType() != null, "#43A047");
        markTile(tileLocation, currentUser.getCity() != null, "#F57C00");
        markTile(tileContact,  currentUser.getNumero() != null, "#C2185B");
        markTile(tilePhoto,    currentUser.getPhoto() != null, "#546E7A");
        markTile(tileNotif,    currentUser.isWantsEmailNotifications(), "#F57C00");
    }

    private void markTile(Button tile, boolean done, String color) {
        if (done) {
            // Thicker solid border = done
            String current = tile.getStyle();
            tile.setStyle(current.replace("-fx-border-color: #F48FB1", "-fx-border-color: " + color)
                                 .replace("-fx-border-color: #A5D6A7", "-fx-border-color: " + color)
                                 .replace("-fx-border-color: #FFCC80", "-fx-border-color: " + color)
                                 .replace("-fx-border-color: #B0BEC5", "-fx-border-color: " + color)
                                 + " -fx-border-width: 3;");
            // Add checkmark prefix
            if (!tile.getText().startsWith("✓")) {
                tile.setText("✓ " + tile.getText());
            }
        }
    }

    // --- Smart Alert Injection ---
    private void scheduleAlert() {
        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
        delay.setOnFinished(e -> showAlert());
        delay.play();
    }

    private void showAlert() {
        SmartAlert alert = ProfileAlertService.getSmartAlert(currentUser);
        if (alert.level() == AlertLevel.NONE) return;

        alertZone.getChildren().clear();

        // Build banner
        HBox banner = new HBox(10);
        banner.setStyle(
            "-fx-background-color: " + alert.color() + ";" +
            "-fx-border-radius: 12; -fx-background-radius: 12;" +
            "-fx-padding: 12; -fx-alignment: CENTER_LEFT;"
        );

        String icon = switch (alert.level()) {
            case CRITICAL -> "⛔";
            case HIGH     -> "⚠️";
            case MEDIUM   -> "💊";
            case LOW      -> "🔔";
            default       -> "ℹ️";
        };

        VBox body = new VBox(2);
        Label title = new Label(icon + "  " + alert.title());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        Label msg = new Label(alert.message());
        msg.setStyle("-fx-font-size: 12; -fx-text-fill: #555; -fx-wrap-text: true;");
        msg.setMaxWidth(360);

        body.getChildren().addAll(title, msg);
        HBox.setHgrow(body, Priority.ALWAYS);

        Button close = new Button("✕");
        close.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #888;");
        close.setOnAction(e -> dismissAndReschedule());

        banner.getChildren().addAll(body, close);

        // Slide-in animation
        TranslateTransition slide = new TranslateTransition(Duration.millis(250), banner);
        slide.setFromY(-20); slide.setToY(0);
        FadeTransition fade = new FadeTransition(Duration.millis(250), banner);
        fade.setFromValue(0); fade.setToValue(1);
        new ParallelTransition(slide, fade).play();

        // Pulse if CRITICAL
        if (alert.level() == AlertLevel.CRITICAL) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(400), banner);
            pulse.setByX(0.03); pulse.setByY(0.03);
            pulse.setCycleCount(6); pulse.setAutoReverse(true);
            PauseTransition wait = new PauseTransition(Duration.millis(300));
            wait.setOnFinished(e2 -> pulse.play());
            wait.play();
        }

        alertZone.getChildren().add(banner);
    }

    private void dismissAndReschedule() {
        alertZone.getChildren().clear();
        dismissCount++;
        int pct = ProfileAlertService.getCompletionPercent(currentUser);

        if (pct >= 50) return;  // above threshold, stop annoying

        // Re-appear: 4s → 4s → 8s → 12s → 20s → stop
        int[] delays = {4, 4, 8, 12, 20};
        if (dismissCount <= delays.length) {
            int secs = delays[dismissCount - 1];
            PauseTransition retry = new PauseTransition(Duration.seconds(secs));
            retry.setOnFinished(e -> showAlert());
            retry.play();
        }
    }

    // --- Skip ---
    @FXML
    private void skipProfile() {
        // Save skip_count to DB (optional)
        UserDAO.incrementSkipCount(currentUser.getId());
        alertZone.getChildren().clear();

        // Show a passive "reminder set" notice
        Label note = new Label("⏰  You'll see a reminder on your next login until you reach 50%.");
        note.setStyle(
            "-fx-background-color: #FFF3E0; -fx-text-fill: #E65100;" +
            "-fx-padding: 10 14; -fx-background-radius: 10; -fx-font-size: 12;" +
            "-fx-wrap-text: true;"
        );
        alertZone.getChildren().add(note);
    }

    // --- Dashboard ---
    @FXML
    private void goDashboard() {
        if (ProfileAlertService.getCompletionPercent(currentUser) >= 50) {
            currentUser.setProfileCompleted(true);
            UserDAO.save(currentUser);
            App.setRoot("dashboard");
        }
    }

    // --- Open sub-forms (each loads its own FXML panel) ---
    @FXML private void openPersonal() { openSubForm("personal-info"); }
    @FXML private void openMedical()  { openSubForm("medical-info"); }
    @FXML private void openLocation() { openSubForm("location-info"); }
    @FXML private void openContact()  { openSubForm("contact-info"); }
    @FXML private void openPhoto()    { openSubForm("photo-upload"); }
    @FXML private void openNotif()    { openSubForm("notif-prefs"); }

    private void openSubForm(String fxmlName) {
        App.pushView(fxmlName);  // pushes to stack, back button pops
    }
}
```

---

### DB Column Required for Skip Counter

Add this one extra column to the `user` table:

```sql
ALTER TABLE user
  ADD COLUMN skip_count INT(11) NOT NULL DEFAULT 0 AFTER profile_completed;
```

---

### Login Hook — `LoginController.java`

```java
@FXML
private void handleLogin() {
    User user = UserDAO.findByEmail(emailField.getText());

    if (user == null || !BCrypt.checkpw(passwordField.getText(), user.getPassword())) {
        showError("Invalid credentials");
        return;
    }

    Session.setCurrentUser(user);
    int pct = ProfileAlertService.getCompletionPercent(user);

    if (pct < 50) {
        // Force onboarding screen — cannot go to dashboard yet
        App.setRoot("complete-profile");
    } else {
        // Profile good — go straight to dashboard
        // But still show a soft banner if 50–79%
        App.setRoot("dashboard");
        if (pct < 80) {
            DashboardController.showSoftBanner(
                "Your profile is at " + pct + "% — complete it in Settings for the best experience."
            );
        }
    }
}
```

---

### Alert Behavior Summary

| Profile % | Alert Type | Color | Re-appears after dismiss? |
|-----------|-----------|-------|--------------------------|
| 1–29% | CRITICAL — features locked | Red `#FFEBEE` | Yes — every 4s (×2), then 8s, 12s, 20s |
| 30–49% (no blood type) | HIGH — missing medical info | Orange `#FFF3E0` | Yes — every 8s |
| 30–49% (no location) | HIGH — no location | Orange `#FFF3E0` | Yes — every 8s |
| 30–49% (other) | MEDIUM — almost there | Pink `#FCE4EC` | Yes — every 12s |
| 50–99% (no notif) | LOW — notifications off | Orange `#FFF3E0` | No |
| 100% | NONE | — | Never |
| Skipped | Passive notice | Orange `#FFF3E0` | Only on next login |

> **Skip behavior:** The `skip_count` is incremented every time the user skips.
> After 3 skips, the reminder interval on re-login doubles (shown less aggressively).
> The "Go to dashboard" button only unlocks at **≥ 50%** — it cannot be bypassed.

---

## 🚀 6 Métiers Avancées — Status

| # | Feature | Table Needed | Status |
|---|---------|-------------|--------|
| 1 | 📊 Dashboard Charts | `rendez_vous`, `medicament` | ✅ Tables ready |
| 2 | 💊 OpenFDA Drug API | `medicament` | ✅ Ready |
| 3 | 📧 Email Notifications | `notification`, `user.wants_email_notifications` | ✅ Ready |
| 4 | 🔐 Google Auth | `user.google_id`, `medecin.google_id` | ✅ Added |
| 5 | 📍 Nearest Doctor | `medecin.latitude`, `medecin.longitude` | ✅ Added |
| 6 | 🤖 AI Symptom Checker | No table needed (stateless API call) | ✅ Ready |

---

## ⚙️ Maven Dependencies to Add in `pom.xml`

```xml
<!-- Google OAuth -->
<dependency>
    <groupId>com.google.oauth-client</groupId>
    <artifactId>google-oauth-client-jetty</artifactId>
    <version>1.34.1</version>
</dependency>
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-oauth2</artifactId>
    <version>v2-rev157-1.25.0</version>
</dependency>

<!-- JSON Parsing (for OpenFDA + AI API) -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- Email (JavaMail) -->
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>
```

---

## 🔑 Google Console Setup (for Google Auth)

```
1. Go to → https://console.cloud.google.com
2. New Project → Name: "Medicare"
3. APIs & Services → Enable: "Google+ API" and "People API"
4. Credentials → Create → OAuth 2.0 Client ID
5. Application type: Desktop App
6. Authorized redirect URIs: http://localhost:8888/callback
7. Download → client_secret.json → put in src/main/resources/
```

---

## 📋 Development Order (Recommended)

```
Week 1 → Apply all SQL migrations above
Week 1 → Profile Completion flow (FXML + Controller + DAO)
Week 2 → Google Auth
Week 2 → Nearest Doctor (Geocoding + Haversine)
Week 3 → AI Symptom Checker (Claude API)
Week 3 → Dashboard Charts
Week 4 → Email Notifications
Week 4 → OpenFDA Drug API
```

---

*Medicare — University Project | JavaFX + Maven + MySQL*


---

---

# 🎨 DESIGN INTEGRATION BLUEPRINT
> Files to create / modify to integrate the template theme into your existing Medicare JavaFX project

---

## 📂 Files Map — What to Touch

```
src/main/resources/
├── css/
│   ├── theme.css              ← CREATE — global palette & variables
│   ├── login.css              ← CREATE — login page styles
│   ├── home.css               ← CREATE — dashboard/home styles
│   └── onboarding.css         ← CREATE — profile completion tiles
├── fxml/
│   ├── Login.fxml             ← MODIFY — add Google Auth button
│   ├── Home.fxml              ← MODIFY — add alert banner zone
│   ├── CompleteProfile.fxml   ← CREATE — tile onboarding screen
│   ├── PersonalInfo.fxml      ← CREATE — sub-form
│   ├── MedicalInfo.fxml       ← CREATE — sub-form
│   ├── LocationInfo.fxml      ← CREATE — sub-form
│   ├── ContactInfo.fxml       ← CREATE — sub-form
│   ├── PhotoUpload.fxml       ← CREATE — sub-form
│   └── NotifPrefs.fxml        ← CREATE — sub-form
└── images/
    └── google-icon.png        ← ADD — Google logo for auth button

src/main/java/com/medicare/
├── controllers/
│   ├── LoginController.java   ← MODIFY — add Google Auth + profile check
│   ├── HomeController.java    ← MODIFY — add alert injection
│   └── CompleteProfileController.java ← CREATE
├── services/
│   ├── GoogleAuthService.java ← CREATE
│   ├── ProfileAlertService.java ← CREATE
│   ├── GeocodingService.java  ← CREATE
│   ├── SymptomAIService.java  ← CREATE
│   └── EmailService.java      ← CREATE
├── models/
│   ├── User.java              ← MODIFY — add new fields
│   └── Medecin.java           ← MODIFY — add new fields
└── dao/
    └── UserDAO.java           ← MODIFY — add skipCount, profileCompleted
```

---

## 🎨 STEP 1 — Create `theme.css`

**Path:** `src/main/resources/css/theme.css`

```css
/* ===== MEDICARE THEME — Hospital Warm Palette ===== */
.root {
    /* Primary Colors */
    -medicare-pink:          #C2185B;
    -medicare-pink-light:    #FCE4EC;
    -medicare-pink-border:   #F48FB1;
    -medicare-green:         #2E7D32;
    -medicare-green-sat:     #43A047;
    -medicare-green-light:   #E8F5E9;
    -medicare-green-border:  #A5D6A7;
    -medicare-orange:        #E65100;
    -medicare-orange-mid:    #F57C00;
    -medicare-orange-light:  #FFF3E0;
    -medicare-orange-border: #FFCC80;
    -medicare-gray:          #546E7A;
    -medicare-gray-mid:      #78909C;
    -medicare-gray-light:    #ECEFF1;
    -medicare-gray-border:   #B0BEC5;
    -medicare-danger:        #B71C1C;
    -medicare-danger-light:  #FFEBEE;
    -medicare-bg:            #FAFAFA;
    -medicare-text:          #212121;
    -medicare-muted:         #757575;
}

/* ===== BASE ===== */
.app-root {
    -fx-background-color: -medicare-bg;
    -fx-font-family: "Segoe UI", Arial, sans-serif;
}

/* ===== BUTTONS ===== */
.btn-pink {
    -fx-background-color: -medicare-pink;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 12;
    -fx-padding: 10 24;
    -fx-cursor: hand;
    -fx-font-size: 14px;
}
.btn-pink:hover    { -fx-background-color: #AD1457; }
.btn-pink:pressed  { -fx-scale-x: 0.97; -fx-scale-y: 0.97; }

.btn-green {
    -fx-background-color: -medicare-green-sat;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 12;
    -fx-padding: 10 24;
    -fx-cursor: hand;
}
.btn-green:hover   { -fx-background-color: -medicare-green; }

.btn-orange {
    -fx-background-color: -medicare-orange-mid;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 12;
    -fx-padding: 10 24;
    -fx-cursor: hand;
}

.btn-outline {
    -fx-background-color: transparent;
    -fx-border-color: -medicare-gray-border;
    -fx-border-width: 1.5;
    -fx-border-radius: 10;
    -fx-background-radius: 10;
    -fx-text-fill: -medicare-gray;
    -fx-padding: 8 20;
    -fx-cursor: hand;
}
.btn-outline:hover { -fx-border-color: -medicare-gray-mid; }

.btn-google {
    -fx-background-color: white;
    -fx-border-color: #DADCE0;
    -fx-border-width: 1.5;
    -fx-border-radius: 10;
    -fx-background-radius: 10;
    -fx-text-fill: #3C4043;
    -fx-font-size: 14px;
    -fx-padding: 10 20;
    -fx-cursor: hand;
}
.btn-google:hover  { -fx-background-color: #F8F9FA; }

/* ===== TILES (Profile Onboarding) ===== */
.tile {
    -fx-pref-width: 145px;
    -fx-pref-height: 145px;
    -fx-border-width: 2;
    -fx-border-radius: 18;
    -fx-background-radius: 18;
    -fx-font-size: 12px;
    -fx-font-weight: bold;
    -fx-cursor: hand;
    -fx-alignment: CENTER;
    -fx-content-display: TOP;
    -fx-graphic-text-gap: 8;
}
.tile:hover   { -fx-scale-x: 1.04; -fx-scale-y: 1.04; }
.tile:pressed { -fx-scale-x: 0.96; -fx-scale-y: 0.96; }

.tile-pink   { -fx-background-color: -medicare-pink-light;   -fx-border-color: -medicare-pink-border;   -fx-text-fill: -medicare-pink; }
.tile-green  { -fx-background-color: -medicare-green-light;  -fx-border-color: -medicare-green-border;  -fx-text-fill: -medicare-green; }
.tile-orange { -fx-background-color: -medicare-orange-light; -fx-border-color: -medicare-orange-border; -fx-text-fill: -medicare-orange; }
.tile-gray   { -fx-background-color: -medicare-gray-light;   -fx-border-color: -medicare-gray-border;   -fx-text-fill: -medicare-gray; }

.tile-pink.done   { -fx-border-color: -medicare-pink;       -fx-border-width: 3; }
.tile-green.done  { -fx-border-color: -medicare-green-sat;  -fx-border-width: 3; }
.tile-orange.done { -fx-border-color: -medicare-orange-mid; -fx-border-width: 3; }
.tile-gray.done   { -fx-border-color: -medicare-gray-mid;   -fx-border-width: 3; }

/* ===== ALERT BANNERS ===== */
.alert-banner {
    -fx-padding: 12 14;
    -fx-background-radius: 12;
    -fx-border-radius: 12;
    -fx-border-width: 1.5;
    -fx-spacing: 10;
}
.alert-critical { -fx-background-color: -medicare-danger-light; -fx-border-color: #EF9A9A; }
.alert-high     { -fx-background-color: -medicare-orange-light; -fx-border-color: -medicare-orange-border; }
.alert-medium   { -fx-background-color: -medicare-pink-light;   -fx-border-color: -medicare-pink-border; }
.alert-low      { -fx-background-color: -medicare-orange-light; -fx-border-color: -medicare-orange-border; }

.alert-title-critical { -fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: -medicare-danger; }
.alert-title-high     { -fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: -medicare-orange; }
.alert-title-medium   { -fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: -medicare-pink; }
.alert-msg            { -fx-font-size: 12; -fx-text-fill: -medicare-muted; -fx-wrap-text: true; }

/* ===== PROGRESS BAR ===== */
.profile-progress { -fx-pref-height: 8; -fx-background-radius: 4; -fx-border-radius: 4; }
.profile-progress .bar   { -fx-background-radius: 4; -fx-background-insets: 0; }
.profile-progress .track { -fx-background-color: #E0E0E0; -fx-background-radius: 4; }
.progress-low  .bar { -fx-background-color: -medicare-pink; }
.progress-mid  .bar { -fx-background-color: -medicare-orange-mid; }
.progress-high .bar { -fx-background-color: -medicare-green-sat; }

/* ===== FORM FIELDS ===== */
.field-label {
    -fx-font-size: 11px;
    -fx-font-weight: bold;
    -fx-text-fill: -medicare-muted;
    -fx-padding: 0 0 4 0;
}
.text-field-medicare, .combo-medicare {
    -fx-background-color: white;
    -fx-border-color: -medicare-gray-border;
    -fx-border-width: 1.5;
    -fx-border-radius: 10;
    -fx-background-radius: 10;
    -fx-padding: 9 12;
    -fx-font-size: 14px;
}
.text-field-medicare:focused, .combo-medicare:focused {
    -fx-border-color: -medicare-pink;
}

/* ===== CARDS ===== */
.card {
    -fx-background-color: white;
    -fx-background-radius: 16;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);
    -fx-padding: 18;
}

/* ===== SIDEBAR ===== */
.sidebar {
    -fx-background-color: white;
    -fx-pref-width: 220;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 2, 0);
}
.sidebar-item {
    -fx-background-color: transparent;
    -fx-text-fill: -medicare-text;
    -fx-font-size: 13px;
    -fx-padding: 10 16;
    -fx-cursor: hand;
    -fx-border-radius: 10;
    -fx-background-radius: 10;
    -fx-alignment: CENTER_LEFT;
    -fx-pref-width: 190px;
}
.sidebar-item:hover   { -fx-background-color: -medicare-pink-light; -fx-text-fill: -medicare-pink; }
.sidebar-item.active  { -fx-background-color: -medicare-pink-light; -fx-text-fill: -medicare-pink; -fx-font-weight: bold; }

/* ===== TOP NAV ===== */
.topnav {
    -fx-background-color: white;
    -fx-pref-height: 56;
    -fx-padding: 0 24;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);
}
.topnav-title {
    -fx-font-size: 18px;
    -fx-font-weight: bold;
    -fx-text-fill: -medicare-pink;
}
```

---

## 🔑 STEP 2 — Modify `Login.fxml`

**Add Google Auth button + apply theme**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.scene.image.*?>

<StackPane xmlns:fx="http://javafx.com/fxml"
           fx:controller="com.medicare.controllers.LoginController"
           stylesheets="@../css/theme.css, @../css/login.css">

    <!-- Background gradient -->
    <VBox style="-fx-background-color: linear-gradient(to bottom right, #FCE4EC, #E8F5E9);"
          alignment="CENTER" prefWidth="900" prefHeight="620"/>

    <!-- Login card -->
    <VBox alignment="CENTER" spacing="0" maxWidth="420" maxHeight="560"
          styleClass="card" style="-fx-padding: 40;">

        <!-- Logo -->
        <HBox alignment="CENTER" spacing="6" style="-fx-padding: 0 0 24 0;">
            <Label text="🏥" style="-fx-font-size: 28;"/>
            <Label text="Medicare" style="-fx-font-size: 26; -fx-font-weight: bold; -fx-text-fill: #C2185B;"/>
        </HBox>

        <Label text="Welcome back" style="-fx-font-size: 15; -fx-text-fill: #757575; -fx-padding: 0 0 24 0;"/>

        <!-- Email -->
        <VBox spacing="4" style="-fx-padding: 0 0 12 0;" maxWidth="340">
            <Label text="EMAIL" styleClass="field-label"/>
            <TextField fx:id="emailField" promptText="your@email.com"
                       styleClass="text-field-medicare" prefWidth="340"/>
        </VBox>

        <!-- Password -->
        <VBox spacing="4" style="-fx-padding: 0 0 8 0;" maxWidth="340">
            <Label text="PASSWORD" styleClass="field-label"/>
            <PasswordField fx:id="passwordField" promptText="••••••••"
                           styleClass="text-field-medicare" prefWidth="340"/>
        </VBox>

        <!-- Forgot password -->
        <HBox alignment="CENTER_RIGHT" maxWidth="340" style="-fx-padding: 0 0 20 0;">
            <Hyperlink text="Forgot password?" onAction="#forgotPassword"
                       style="-fx-font-size: 12; -fx-text-fill: #C2185B;"/>
        </HBox>

        <!-- Error label -->
        <Label fx:id="errorLabel" text="" wrapText="true" maxWidth="340"
               style="-fx-text-fill: #B71C1C; -fx-font-size: 12; -fx-padding: 0 0 10 0;" visible="false"/>

        <!-- Login button -->
        <Button text="Sign In" onAction="#handleLogin"
                styleClass="btn-pink" prefWidth="340" style="-fx-padding: 12;"/>

        <!-- Divider -->
        <HBox alignment="CENTER" spacing="10" style="-fx-padding: 18 0;">
            <Separator prefWidth="140"/>
            <Label text="or" style="-fx-text-fill: #9E9E9E; -fx-font-size: 12;"/>
            <Separator prefWidth="140"/>
        </HBox>

        <!-- ✅ GOOGLE AUTH BUTTON -->
        <Button fx:id="googleBtn" onAction="#handleGoogleLogin"
                prefWidth="340" styleClass="btn-google"
                style="-fx-padding: 11;">
            <graphic>
                <HBox spacing="10" alignment="CENTER">
                    <ImageView fitWidth="18" fitHeight="18" preserveRatio="true">
                        <image><Image url="@../images/google-icon.png"/></image>
                    </ImageView>
                    <Label text="Continue with Google" style="-fx-font-size: 14; -fx-text-fill: #3C4043;"/>
                </HBox>
            </graphic>
        </Button>

        <!-- Register link -->
        <HBox alignment="CENTER" spacing="4" style="-fx-padding: 20 0 0 0;">
            <Label text="Don't have an account?" style="-fx-font-size: 13; -fx-text-fill: #757575;"/>
            <Hyperlink text="Register" onAction="#goToRegister"
                       style="-fx-font-size: 13; -fx-text-fill: #C2185B; -fx-font-weight: bold;"/>
        </HBox>
    </VBox>
</StackPane>
```

---

## 🏠 STEP 3 — Modify `Home.fxml` (Dashboard)

**Add alert zone + theme**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml"
            fx:controller="com.medicare.controllers.HomeController"
            stylesheets="@../css/theme.css, @../css/home.css"
            styleClass="app-root" prefWidth="1100" prefHeight="700">

    <!-- LEFT SIDEBAR -->
    <left>
        <VBox styleClass="sidebar" spacing="4" style="-fx-padding: 20 12;">
            <Label text="🏥 Medicare" style="-fx-font-size:16; -fx-font-weight:bold;
                         -fx-text-fill:#C2185B; -fx-padding: 0 0 16 4;"/>

            <Button text="🏠  Dashboard"     styleClass="sidebar-item,active"  onAction="#showDashboard"/>
            <Button text="📅  Appointments"  styleClass="sidebar-item"         onAction="#showAppointments"/>
            <Button text="💊  Medications"   styleClass="sidebar-item"         onAction="#showMedications"/>
            <Button text="👨‍⚕️  Doctors"       styleClass="sidebar-item"         onAction="#showDoctors"/>
            <Button text="🤖  AI Checker"    styleClass="sidebar-item"         onAction="#showAIChecker"/>
            <Button text="📊  Statistics"    styleClass="sidebar-item"         onAction="#showStats"/>

            <Region VBox.vgrow="ALWAYS"/>
            <Button text="⚙️  Settings"      styleClass="sidebar-item"         onAction="#showSettings"/>
            <Button text="🚪  Logout"         styleClass="sidebar-item"         onAction="#handleLogout"
                    style="-fx-text-fill: #B71C1C;"/>
        </VBox>
    </left>

    <!-- MAIN CONTENT -->
    <center>
        <VBox spacing="16" style="-fx-padding: 24; -fx-background-color: #FAFAFA;">

            <!-- TOP NAV -->
            <HBox styleClass="topnav" alignment="CENTER_LEFT" spacing="16"
                  style="-fx-padding: 0 24; -fx-pref-height: 56;">
                <Label fx:id="pageTitle" text="Dashboard" styleClass="topnav-title"/>
                <Region HBox.hgrow="ALWAYS"/>
                <!-- Profile completion badge -->
                <Label fx:id="profileBadge" text="Profile 40%"
                       style="-fx-background-color: #FCE4EC; -fx-text-fill: #C2185B;
                              -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-size: 12;
                              -fx-font-weight: bold; -fx-cursor: hand;"/>
                <Label fx:id="userNameLabel" text="Ahmed"
                       style="-fx-font-size: 14; -fx-font-weight: bold;"/>
                <ImageView fx:id="userPhoto" fitWidth="36" fitHeight="36"
                           style="-fx-background-radius: 18;"/>
            </HBox>

            <!-- ✅ SMART ALERT ZONE — dynamically filled by HomeController -->
            <VBox fx:id="alertZone" spacing="8" style="-fx-padding: 0 0 4 0;"/>

            <!-- CONTENT AREA — swapped by sidebar buttons -->
            <StackPane fx:id="contentArea" VBox.vgrow="ALWAYS"/>

        </VBox>
    </center>

</BorderPane>
```

---

## 🔧 STEP 4 — Modify `LoginController.java`

**Wire Google Auth + profile completion redirect**

```java
@FXML
private void handleLogin() {
    User user = UserDAO.findByEmail(emailField.getText().trim());
    if (user == null || !BCrypt.checkpw(passwordField.getText(), user.getPassword())) {
        errorLabel.setText("Invalid email or password.");
        errorLabel.setVisible(true);
        return;
    }
    Session.setCurrentUser(user);
    postLoginRedirect(user);
}

@FXML
private void handleGoogleLogin() {
    try {
        GoogleAuthService auth = new GoogleAuthService();
        GoogleTokenResponse token = auth.authenticate();
        UserInfo info = auth.getUserInfo(token.getAccessToken());

        // Find or create user
        User user = UserDAO.findByGoogleId(info.getId());
        if (user == null) {
            user = new User();
            user.setEmail(info.getEmail());
            user.setNom(info.getFamilyName());
            user.setPrenom(info.getGivenName());
            user.setPhoto(info.getPicture());
            user.setGoogleId(info.getId());
            user.setGoogleAccessToken(token.getAccessToken());
            user.setVerified(true);         // Google = pre-verified
            user.setProfileCompleted(false);
            UserDAO.save(user);
        }
        Session.setCurrentUser(user);
        postLoginRedirect(user);

    } catch (Exception e) {
        errorLabel.setText("Google login failed: " + e.getMessage());
        errorLabel.setVisible(true);
    }
}

private void postLoginRedirect(User user) {
    int pct = ProfileAlertService.getCompletionPercent(user);
    // Update last_login_at
    UserDAO.updateLastLogin(user.getId());

    if (pct < 50) {
        App.setRoot("CompleteProfile");
    } else {
        App.setRoot("Home");
    }
}
```

---

## 🔧 STEP 5 — Modify `HomeController.java`

**Wire smart alert injection**

```java
@FXML private VBox alertZone;
@FXML private Label profileBadge;
@FXML private Label userNameLabel;

private int dismissCount = 0;

@FXML
public void initialize() {
    User user = Session.getCurrentUser();
    int pct = ProfileAlertService.getCompletionPercent(user);

    // Update top nav
    userNameLabel.setText(user.getPrenom());
    profileBadge.setText("Profile " + pct + "%");
    if (pct >= 80) profileBadge.setVisible(false);

    // Show alert after 1.5s delay
    PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
    delay.setOnFinished(e -> injectAlert());
    delay.play();
}

private void injectAlert() {
    User user = Session.getCurrentUser();
    ProfileAlertService.SmartAlert alert = ProfileAlertService.getSmartAlert(user);
    if (alert.level() == ProfileAlertService.AlertLevel.NONE) return;

    alertZone.getChildren().clear();

    HBox banner = new HBox(12);
    String cssClass = switch (alert.level()) {
        case CRITICAL -> "alert-critical";
        case HIGH     -> "alert-high";
        case MEDIUM   -> "alert-medium";
        default       -> "alert-low";
    };
    banner.getStyleClass().addAll("alert-banner", cssClass);
    banner.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

    VBox body = new VBox(3);
    Label title = new Label(alert.title());
    String titleCss = switch (alert.level()) {
        case CRITICAL -> "alert-title-critical";
        case HIGH     -> "alert-title-high";
        default       -> "alert-title-medium";
    };
    title.getStyleClass().add(titleCss);

    Label msg = new Label(alert.message());
    msg.getStyleClass().add("alert-msg");
    msg.setMaxWidth(600);
    body.getChildren().addAll(title, msg);
    HBox.setHgrow(body, Priority.ALWAYS);

    Button complete = new Button("Complete now →");
    complete.getStyleClass().add("btn-pink");
    complete.setStyle("-fx-font-size: 12; -fx-padding: 6 14;");
    complete.setOnAction(e -> App.setRoot("CompleteProfile"));

    Button close = new Button("✕");
    close.getStyleClass().add("btn-outline");
    close.setStyle("-fx-font-size: 12; -fx-padding: 5 10;");
    close.setOnAction(e -> dismissAndReschedule());

    banner.getChildren().addAll(body, complete, close);

    // Slide-in animation
    FadeTransition ft = new FadeTransition(Duration.millis(300), banner);
    ft.setFromValue(0); ft.setToValue(1);
    TranslateTransition tt = new TranslateTransition(Duration.millis(300), banner);
    tt.setFromY(-15); tt.setToY(0);
    new ParallelTransition(ft, tt).play();

    alertZone.getChildren().add(banner);
}

private void dismissAndReschedule() {
    alertZone.getChildren().clear();
    dismissCount++;
    int pct = ProfileAlertService.getCompletionPercent(Session.getCurrentUser());
    if (pct >= 50 || dismissCount > 5) return;

    int[] waitSecs = {5, 5, 10, 15, 25};
    int secs = waitSecs[Math.min(dismissCount - 1, waitSecs.length - 1)];
    PauseTransition retry = new PauseTransition(Duration.seconds(secs));
    retry.setOnFinished(e -> injectAlert());
    retry.play();
}
```

---

## 📋 STEP 6 — Load CSS in `App.java` (Main entry)

```java
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("Login"), 900, 620);

        // ✅ Load global theme CSS
        scene.getStylesheets().add(
            getClass().getResource("/css/theme.css").toExternalForm()
        );

        stage.setTitle("Medicare");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // Push new view keeping CSS
    public static void pushView(String fxml) throws IOException {
        Parent view = loadFXML(fxml);
        view.getStylesheets().add(
            App.class.getResource("/css/theme.css").toExternalForm()
        );
        scene.setRoot(view);
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            App.class.getResource("/fxml/" + fxml + ".fxml")
        );
        return fxmlLoader.load();
    }
}
```

---

## ✅ Integration Checklist

```
[ ] 1. Create src/main/resources/css/theme.css       (copy Step 1)
[ ] 2. Add stylesheets="" to Login.fxml              (Step 2)
[ ] 3. Add stylesheets="" to Home.fxml               (Step 3)
[ ] 4. Add Google button to Login.fxml               (Step 2)
[ ] 5. Add fx:id="alertZone" VBox to Home.fxml       (Step 3)
[ ] 6. Add handleGoogleLogin() to LoginController    (Step 4)
[ ] 7. Add postLoginRedirect() to LoginController    (Step 4)
[ ] 8. Add injectAlert() to HomeController           (Step 5)
[ ] 9. Add theme.css to App.java scene               (Step 6)
[ ] 10. Create CompleteProfile.fxml                  (see Onboarding section)
[ ] 11. Create CompleteProfileController.java        (see Alert section)
[ ] 12. Add google-icon.png to resources/images/     (download from Google Brand)
[ ] 13. Run DB migrations (all ALTER TABLE SQLs)
[ ] 14. Add skip_count column to user table
[ ] 15. Test Google Auth flow end to end
```

---

*Blueprint generated from full project context — Medicare JavaFX + Maven*
