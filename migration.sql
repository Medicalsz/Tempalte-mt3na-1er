-- ============================================================
-- Medicare — Combined SQL Migration Script
-- Run this ENTIRELY in phpMyAdmin → SQL Tab
-- ============================================================

-- ==================== USER TABLE ====================

-- Remove old privacy columns (ignore errors if they don't exist)
ALTER TABLE user
  DROP COLUMN IF EXISTS email_privacy,
  DROP COLUMN IF EXISTS phone_privacy,
  DROP COLUMN IF EXISTS address_privacy,
  DROP COLUMN IF EXISTS adresse_privacy,
  DROP COLUMN IF EXISTS adress_privacy,
  DROP COLUMN IF EXISTS is_private,
  DROP COLUMN IF EXISTS online_duration;

-- Change roles to simple VARCHAR
ALTER TABLE user MODIFY COLUMN roles VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER';

-- Allow NULL password for Google users
ALTER TABLE user MODIFY COLUMN password VARCHAR(255) NULL;

-- Add new columns to user table
ALTER TABLE user
  ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS google_access_token TEXT NULL,
  ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS city VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS gender ENUM('male','female','other') NULL,
  ADD COLUMN IF NOT EXISTS blood_type ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') NULL,
  ADD COLUMN IF NOT EXISTS allergies TEXT NULL,
  ADD COLUMN IF NOT EXISTS profile_completed TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS skip_count INT(11) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS privacy_level ENUM('public','friends','private') NOT NULL DEFAULT 'public',
  ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

-- ==================== MEDECIN TABLE ====================

-- Remove old columns
ALTER TABLE medecin
  DROP COLUMN IF EXISTS email_privacy,
  DROP COLUMN IF EXISTS phone_privacy,
  DROP COLUMN IF EXISTS address_privacy,
  DROP COLUMN IF EXISTS adresse_privacy,
  DROP COLUMN IF EXISTS online_duration;

-- Allow NULL password for Google users
ALTER TABLE medecin MODIFY COLUMN password VARCHAR(255) NULL;

-- Add new columns to medecin table
ALTER TABLE medecin
  ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS google_access_token TEXT NULL,
  ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS experience_years INT(3) NULL,
  ADD COLUMN IF NOT EXISTS consultation_duration INT(11) NOT NULL DEFAULT 30,
  ADD COLUMN IF NOT EXISTS is_available_online TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS rating_average DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN IF NOT EXISTS rating_count INT(11) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS disponibilite TEXT NULL,
  ADD COLUMN IF NOT EXISTS languages VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS gender ENUM('male','female') NULL,
  ADD COLUMN IF NOT EXISTS privacy_level ENUM('public','friends','private') NOT NULL DEFAULT 'public',
  ADD COLUMN IF NOT EXISTS profile_completed TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

-- ==================== RENDEZ_VOUS TABLE SYNC ====================
-- Add columns used by the RDV module (safe to run multiple times)
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS motif VARCHAR(500) NULL;
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS motif_annulation VARCHAR(500) NULL;
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS rappel_envoye TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS hidden_by_patient TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS hidden_by_medecin TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS proposed_date DATE NULL;
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS proposed_heure TIME NULL;
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS report_pending_patient_response TINYINT(1) NOT NULL DEFAULT 0;


-- ==================== NEW TABLES ====================

CREATE TABLE IF NOT EXISTS user_block (
  id         INT(11)  NOT NULL AUTO_INCREMENT,
  blocker_id INT(11)  NOT NULL,
  blocked_id INT(11)  NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_block (blocker_id, blocked_id),
  FOREIGN KEY (blocker_id) REFERENCES user(id) ON DELETE CASCADE,
  FOREIGN KEY (blocked_id) REFERENCES user(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS medicament (
  id              INT(11) NOT NULL AUTO_INCREMENT,
  user_id         INT(11) NOT NULL,
  nom             VARCHAR(255) NOT NULL,
  dosage          VARCHAR(100) NULL,
  frequence       VARCHAR(100) NULL,
  heure_prise     VARCHAR(100) NULL,
  date_debut      DATE NULL,
  date_fin        DATE NULL,
  stock_actuel    INT(11) NOT NULL DEFAULT 0,
  stock_minimum   INT(11) NOT NULL DEFAULT 5,
  est_actif       TINYINT(1) NOT NULL DEFAULT 1,
  notes           TEXT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS avis (
  id              INT(11) NOT NULL AUTO_INCREMENT,
  user_id         INT(11) NOT NULL,
  medecin_id      INT(11) NOT NULL,
  note            TINYINT(1) NOT NULL,
  commentaire     TEXT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id)    REFERENCES user(id)    ON DELETE CASCADE,
  FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification (
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

-- ============================================================
-- DONE! Verify with:
--   DESCRIBE user;
--   DESCRIBE medecin;
--   SHOW TABLES;
-- ============================================================
