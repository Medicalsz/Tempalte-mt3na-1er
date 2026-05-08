-- =========================================================
-- MEDICARE - MIGRATION DE BASE DE DONNÉES (SÉCURISÉE)
-- Ce script ajoute les tables et colonnes manquantes
-- SANS EFFACER les données existantes (Produits, Users, etc.)
-- =========================================================

-- 1. TABLE: user (Mise à jour additive)
CREATE TABLE IF NOT EXISTS `user` (
    `id` INT AUTO_INCREMENT PRIMARY KEY
) ENGINE=InnoDB;

-- Ajout des colonnes manquantes si elles n'existent pas
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `nom` VARCHAR(100) NOT NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `prenom` VARCHAR(100) NOT NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `username` VARCHAR(100);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `email` VARCHAR(180) NOT NULL UNIQUE;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `password` VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `numero` VARCHAR(30);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `adresse` VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `photo` VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `roles` VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `is_verified` TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `google_id` VARCHAR(255) NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `google_access_token` TEXT NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `latitude` DOUBLE NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `longitude` DOUBLE NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `city` VARCHAR(100) NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `gender` ENUM('male','female','other') NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `blood_type` ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `allergies` TEXT NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `profile_completed` TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `skip_count` INT(11) NOT NULL DEFAULT 0;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `privacy_level` ENUM('public','friends','private') NOT NULL DEFAULT 'public';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `biometric_id` VARCHAR(255) NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `updated_at` DATETIME NULL;

-- 2. TABLE: specialite
CREATE TABLE IF NOT EXISTS `specialite` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(100) NOT NULL,
    `slug` VARCHAR(120) UNIQUE,
    `active` TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- 3. TABLE: medecin
CREATE TABLE IF NOT EXISTS `medecin` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL UNIQUE,
    `specialite_ref_id` INT,
    `cabinet` VARCHAR(255),
    `bio` TEXT,
    `experience_years` INT(3) NULL,
    `consultation_duration` INT(11) NOT NULL DEFAULT 30,
    `is_available_online` TINYINT(1) NOT NULL DEFAULT 0,
    `rating_average` DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    `rating_count` INT(11) NOT NULL DEFAULT 0,
    `languages` VARCHAR(255) NULL,
    CONSTRAINT `fk_medecin_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_medecin_specialite` FOREIGN KEY (`specialite_ref_id`) REFERENCES `specialite`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 4. TABLE: patient
CREATE TABLE IF NOT EXISTS `patient` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL UNIQUE,
    `date_naissance` DATE,
    `groupe_sanguin` VARCHAR(5),
    CONSTRAINT `fk_patient_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. TABLE: admin
CREATE TABLE IF NOT EXISTS `admin` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(100) NULL,
  `prenom` VARCHAR(100) NULL,
  `email` VARCHAR(180) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `roles` VARCHAR(50) NULL DEFAULT '["ROLE_ADMIN"]',
  `is_verified` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_email` (`email`)
) ENGINE=InnoDB;

-- 6. TABLE: produit (Mise à jour additive)
CREATE TABLE IF NOT EXISTS `produit` (
    `id` INT AUTO_INCREMENT PRIMARY KEY
) ENGINE=InnoDB;

ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `name` VARCHAR(150) NOT NULL;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `description` TEXT;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `image_url` VARCHAR(500) NULL;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `image_public_id` VARCHAR(255) NULL;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `sku` VARCHAR(80) UNIQUE;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `quantity` INT NOT NULL DEFAULT 0;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `type` VARCHAR(60);
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `dosage` VARCHAR(60);
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `expiry_date` DATETIME;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `is_active` TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE `produit` ADD COLUMN IF NOT EXISTS `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 7. TABLE: commande
CREATE TABLE IF NOT EXISTS `commande` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `commande_number` VARCHAR(60) NOT NULL UNIQUE,
    `product_id` INT NOT NULL,
    `user_id` INT,
    `quantity` INT NOT NULL DEFAULT 1,
    `total_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `status` VARCHAR(20) NOT NULL DEFAULT 'en_attente',
    `notes` TEXT,
    `commande_date` DATETIME,
    `delivery_date` DATETIME,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `stripe_payment_intent_id` VARCHAR(120),
    CONSTRAINT `fk_commande_produit` FOREIGN KEY (`product_id`) REFERENCES `produit`(`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_commande_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 8. TABLE: rendez_vous
CREATE TABLE IF NOT EXISTS `rendez_vous` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `medecin_id` INT(11) NOT NULL,
  `patient_id` INT(11) NOT NULL,
  `date` DATE NOT NULL,
  `heure` TIME NOT NULL,
  `statut` VARCHAR(50) NOT NULL DEFAULT 'en_attente',
  `motif` VARCHAR(500) NULL,
  `proposed_date` DATE NULL,
  `proposed_heure` TIME NULL,
  `report_pending_patient_response` TINYINT(1) NOT NULL DEFAULT 0,
  `rappel_envoye` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`medecin_id`) REFERENCES `medecin`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`patient_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. TABLE: partner
CREATE TABLE IF NOT EXISTS `partner` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `type_partenaire` VARCHAR(120) NULL,
  `email` VARCHAR(255) NULL,
  `telephone` VARCHAR(50) NULL,
  `adresse` TEXT NULL,
  `statut` VARCHAR(50) NULL DEFAULT 'actif',
  `date_partenariat` DATE NULL,
  `image_name` VARCHAR(500) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- 10. TABLE: collaboration
CREATE TABLE IF NOT EXISTS `collaboration` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `partner_id` INT(11) NOT NULL,
  `user_id` INT(11) NULL,
  `titre` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `statut` VARCHAR(50) NULL DEFAULT 'en_attente',
  `image_name` VARCHAR(500) NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`partner_id`) REFERENCES `partner`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 11. TABLE: forum
CREATE TABLE IF NOT EXISTS `forum` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT,
    `titre` VARCHAR(200),
    `contenu` TEXT,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_forum_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- DONNÉES DE BASE (SANS DOUBLONS)
-- =========================================================

INSERT IGNORE INTO `specialite` (`nom`, `slug`, `active`) VALUES
  ('Médecine Générale', 'medecine-generale', 1),
  ('Cardiologie', 'cardiologie', 1),
  ('Dermatologie', 'dermatologie', 1);
