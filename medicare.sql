-- =========================================================
-- Medicare - Schema MySQL
-- Base: medicare (jdbc:mysql://localhost:3306/medicare)
-- =========================================================

DROP DATABASE IF EXISTS medicare;
CREATE DATABASE medicare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medicare;

-- =========================================================
-- USER
-- =========================================================
CREATE TABLE user (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL,
    prenom      VARCHAR(100) NOT NULL,
    email       VARCHAR(180) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    numero      VARCHAR(30),
    adresse     VARCHAR(255),
    photo       VARCHAR(255),
    roles       VARCHAR(100) NOT NULL DEFAULT 'PATIENT',
    is_verified TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_user_email (email)
) ENGINE=InnoDB;

-- =========================================================
-- SPECIALITE
-- =========================================================
CREATE TABLE specialite (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nom    VARCHAR(100) NOT NULL,
    slug   VARCHAR(120) UNIQUE,
    active TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- =========================================================
-- MEDECIN
-- =========================================================
CREATE TABLE medecin (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    user_id            INT NOT NULL UNIQUE,
    specialite         VARCHAR(120),
    cabinet            VARCHAR(255),
    bio                TEXT,
    specialite_ref_id  INT,
    CONSTRAINT fk_medecin_user        FOREIGN KEY (user_id)           REFERENCES user(id)       ON DELETE CASCADE,
    CONSTRAINT fk_medecin_specialite  FOREIGN KEY (specialite_ref_id) REFERENCES specialite(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- PATIENT
-- =========================================================
CREATE TABLE patient (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL UNIQUE,
    date_naissance  DATE,
    groupe_sanguin  VARCHAR(5),
    CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- DISPONIBILITE
-- =========================================================
CREATE TABLE disponibilite (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    medecin_id       INT NOT NULL,
    jour_semaine     VARCHAR(15) NOT NULL,
    ferme            TINYINT(1) NOT NULL DEFAULT 0,
    matin_debut      TIME,
    matin_fin        TIME,
    pause_debut      TIME,
    pause_fin        TIME,
    apres_midi_debut TIME,
    apres_midi_fin   TIME,
    CONSTRAINT fk_dispo_medecin FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE,
    UNIQUE KEY uq_dispo_medecin_jour (medecin_id, jour_semaine)
) ENGINE=InnoDB;

-- =========================================================
-- RENDEZ-VOUS
-- =========================================================
CREATE TABLE rendez_vous (
    id                               INT AUTO_INCREMENT PRIMARY KEY,
    medecin_id                       INT NOT NULL,
    patient_id                       INT NOT NULL,
    date                             DATE NOT NULL,
    heure                            TIME NOT NULL,
    statut                           VARCHAR(20) NOT NULL DEFAULT 'en_attente',
    proposed_date                    DATE,
    proposed_heure                   TIME,
    report_pending_patient_response  TINYINT(1) NOT NULL DEFAULT 0,
    hidden_by_medecin                TINYINT(1) NOT NULL DEFAULT 0,
    hidden_by_patient                TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_rv_medecin FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE,
    CONSTRAINT fk_rv_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    INDEX idx_rv_date (date),
    INDEX idx_rv_statut (statut)
) ENGINE=InnoDB;

-- =========================================================
-- PRODUIT
-- =========================================================
CREATE TABLE produit (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    sku         VARCHAR(80) UNIQUE,
    price       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    quantity    INT NOT NULL DEFAULT 0,
    type        VARCHAR(60),
    dosage      VARCHAR(60),
    expiry_date DATETIME,
    is_active   TINYINT(1) NOT NULL DEFAULT 1,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_produit_name (name),
    INDEX idx_produit_type (type)
) ENGINE=InnoDB;

-- =========================================================
-- COMMANDE
-- =========================================================
CREATE TABLE commande (
    id                         INT AUTO_INCREMENT PRIMARY KEY,
    commande_number            VARCHAR(60) NOT NULL UNIQUE,
    product_id                 INT NOT NULL,
    user_id                    INT,
    quantity                   INT NOT NULL DEFAULT 1,
    total_price                DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status                     VARCHAR(20) NOT NULL DEFAULT 'en_attente',
    notes                      TEXT,
    commande_date              DATETIME,
    delivery_date              DATETIME,
    created_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stripe_payment_intent_id   VARCHAR(120),
    CONSTRAINT fk_commande_produit FOREIGN KEY (product_id) REFERENCES produit(id) ON DELETE RESTRICT,
    CONSTRAINT fk_commande_user    FOREIGN KEY (user_id)    REFERENCES user(id)    ON DELETE SET NULL,
    INDEX idx_commande_status (status),
    INDEX idx_commande_date (commande_date)
) ENGINE=InnoDB;

-- =========================================================
-- COLLABORATION (placeholder)
-- =========================================================
CREATE TABLE collaboration (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    titre       VARCHAR(150),
    description TEXT,
    user_id     INT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_collab_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- DONATION (placeholder)
-- =========================================================
CREATE TABLE donation (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    montant     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    message     TEXT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donation_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- FORUM (placeholder)
-- =========================================================
CREATE TABLE forum (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT,
    titre      VARCHAR(200),
    contenu    TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_forum_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- SEED DATA (optionnel)
-- =========================================================
INSERT INTO specialite (nom, slug, active) VALUES
    ('Cardiologie',    'cardiologie',    1),
    ('Dermatologie',   'dermatologie',   1),
    ('Pediatrie',      'pediatrie',      1),
    ('Medecine generale', 'medecine-generale', 1);

INSERT INTO produit (name, description, sku, price, quantity, type, dosage, is_active)
VALUES
    ('Paracetamol 500mg', 'Antidouleur et antipyretique', 'PARA-500', 2.50, 150, 'medicament', '500mg', 1),
    ('Ibuprofene 400mg',  'Anti-inflammatoire',           'IBU-400',  3.20, 100, 'medicament', '400mg', 1),
    ('Masque FFP2',       'Masque de protection',          'MASK-FFP2', 1.10, 500, 'equipement', NULL, 1);
