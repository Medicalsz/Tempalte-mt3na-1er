-- ============================================================
-- Medicare — Safe Migration Script
-- Only ALTERs existing user/medecin tables (keeps your data)
-- Creates missing tables if they don't exist
-- Safe to run multiple times
-- ============================================================

USE medicare;

-- ==================== 1. ALTER TABLE: user ====================
-- Remove old columns that are no longer needed

ALTER TABLE user DROP COLUMN IF EXISTS email_privacy;
ALTER TABLE user DROP COLUMN IF EXISTS phone_privacy;
ALTER TABLE user DROP COLUMN IF EXISTS address_privacy;
ALTER TABLE user DROP COLUMN IF EXISTS is_private;
ALTER TABLE user DROP COLUMN IF EXISTS online_duration;

-- Fix roles type (was LONGTEXT, should be VARCHAR)
ALTER TABLE user MODIFY COLUMN roles VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER';

-- Allow NULL password (for Google Auth users)
ALTER TABLE user MODIFY COLUMN password VARCHAR(255) NULL;

-- Add all new columns
ALTER TABLE user ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS google_access_token TEXT NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS city VARCHAR(100) NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS gender ENUM('male','female','other') NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS blood_type ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-') NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS allergies TEXT NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS profile_completed TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE user ADD COLUMN IF NOT EXISTS skip_count INT(11) NOT NULL DEFAULT 0;
ALTER TABLE user ADD COLUMN IF NOT EXISTS privacy_level ENUM('public','friends','private') NOT NULL DEFAULT 'public';
ALTER TABLE user ADD COLUMN IF NOT EXISTS biometric_id VARCHAR(255) NULL;
ALTER TABLE user ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;


-- ==================== 2. ALTER TABLE: medecin ====================
-- Remove old columns

ALTER TABLE medecin DROP COLUMN IF EXISTS email_privacy;
ALTER TABLE medecin DROP COLUMN IF EXISTS phone_privacy;
ALTER TABLE medecin DROP COLUMN IF EXISTS address_privacy;
ALTER TABLE medecin DROP COLUMN IF EXISTS online_duration;

-- Allow NULL password (for Google Auth users)
ALTER TABLE medecin MODIFY COLUMN password VARCHAR(255) NULL;

-- Add user_id (CRITICAL — links medecin to user table)
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS user_id INT(11) NULL AFTER id;

-- Add specialite_ref_id (links to specialite table)
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS specialite_ref_id INT(11) NULL;

-- Add all new columns
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) NULL;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS google_access_token TEXT NULL;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS experience_years INT(3) NULL;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS consultation_duration INT(11) NOT NULL DEFAULT 30;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS is_available_online TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS rating_average DECIMAL(3,2) NOT NULL DEFAULT 0.00;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS rating_count INT(11) NOT NULL DEFAULT 0;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS disponibilite TEXT NULL;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS languages VARCHAR(255) NULL;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS gender ENUM('male','female') NULL;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS privacy_level ENUM('public','friends','private') NOT NULL DEFAULT 'public';
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS profile_completed TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE medecin ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;


-- ==================== 3. CREATE missing tables ====================

-- 3a. TABLE: admin
CREATE TABLE IF NOT EXISTS admin (
  id            INT(11)       NOT NULL AUTO_INCREMENT,
  nom           VARCHAR(100)  NULL,
  prenom        VARCHAR(100)  NULL,
  email         VARCHAR(180)  NOT NULL,
  password      VARCHAR(255)  NOT NULL,
  roles         VARCHAR(50)   NULL DEFAULT '[\"ROLE_ADMIN\"]',
  is_verified   TINYINT(1)    NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3b. TABLE: specialite
CREATE TABLE IF NOT EXISTS specialite (
  id      INT(11)       NOT NULL AUTO_INCREMENT,
  nom     VARCHAR(255)  NOT NULL,
  slug    VARCHAR(255)  NULL,
  active  TINYINT(1)    NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3c. TABLE: disponibilite
CREATE TABLE IF NOT EXISTS disponibilite (
  id                INT(11)       NOT NULL AUTO_INCREMENT,
  medecin_id        INT(11)       NOT NULL,
  jour_semaine      VARCHAR(20)   NOT NULL,
  ferme             TINYINT(1)    NOT NULL DEFAULT 0,
  matin_debut       TIME          NULL,
  matin_fin         TIME          NULL,
  pause_debut       TIME          NULL,
  pause_fin         TIME          NULL,
  apres_midi_debut  TIME          NULL,
  apres_midi_fin    TIME          NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3d. TABLE: rendez_vous
CREATE TABLE IF NOT EXISTS rendez_vous (
  id                                INT(11)       NOT NULL AUTO_INCREMENT,
  medecin_id                        INT(11)       NOT NULL,
  patient_id                        INT(11)       NOT NULL,
  date                              DATE          NOT NULL,
  heure                             TIME          NOT NULL,
  statut                            VARCHAR(50)   NOT NULL DEFAULT 'en_attente',
  motif                             VARCHAR(500)  NULL,
  motif_annulation                  VARCHAR(500)  NULL,
  hidden_by_patient                 TINYINT(1)    NOT NULL DEFAULT 0,
  hidden_by_medecin                 TINYINT(1)    NOT NULL DEFAULT 0,
  proposed_date                     DATE          NULL,
  proposed_heure                    TIME          NULL,
  report_pending_patient_response   TINYINT(1)    NOT NULL DEFAULT 0,
  rappel_envoye                     TINYINT(1)    NOT NULL DEFAULT 0,
  created_at                        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                        DATETIME      NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE,
  FOREIGN KEY (patient_id) REFERENCES user(id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3e. TABLE: demande_medecin
CREATE TABLE IF NOT EXISTS demande_medecin (
  id                INT(11)       NOT NULL AUTO_INCREMENT,
  user_id           INT(11)       NOT NULL,
  certificat_pdf    VARCHAR(255)  NULL,
  cin_recto         VARCHAR(255)  NULL,
  cin_verso         VARCHAR(255)  NULL,
  statut            VARCHAR(50)   NOT NULL DEFAULT 'en_attente',
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3f. TABLE: medicament
CREATE TABLE IF NOT EXISTS medicament (
  id              INT(11)       NOT NULL AUTO_INCREMENT,
  user_id         INT(11)       NOT NULL,
  nom             VARCHAR(255)  NOT NULL,
  dosage          VARCHAR(100)  NULL,
  frequence       VARCHAR(100)  NULL,
  heure_prise     VARCHAR(100)  NULL,
  date_debut      DATE          NULL,
  date_fin        DATE          NULL,
  stock_actuel    INT(11)       NOT NULL DEFAULT 0,
  stock_minimum   INT(11)       NOT NULL DEFAULT 5,
  est_actif       TINYINT(1)    NOT NULL DEFAULT 1,
  notes           TEXT          NULL,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3g. TABLE: avis
CREATE TABLE IF NOT EXISTS avis (
  id              INT(11)       NOT NULL AUTO_INCREMENT,
  user_id         INT(11)       NOT NULL,
  medecin_id      INT(11)       NOT NULL,
  note            TINYINT(1)    NOT NULL,
  commentaire     TEXT          NULL,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id)    REFERENCES user(id)    ON DELETE CASCADE,
  FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3h. TABLE: notification
CREATE TABLE IF NOT EXISTS notification (
  id              INT(11)       NOT NULL AUTO_INCREMENT,
  user_id         INT(11)       NULL,
  medecin_id      INT(11)       NULL,
  type            ENUM('appointment','medication','system','reminder') NOT NULL,
  titre           VARCHAR(255)  NOT NULL,
  message         TEXT          NOT NULL,
  is_read         TINYINT(1)    NOT NULL DEFAULT 0,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 3i. TABLE: user_block
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

-- ==================== 4. SAMPLE DATA ====================

INSERT IGNORE INTO specialite (nom, slug, active) VALUES
  ('Médecine Générale',    'medecine-generale',    1),
  ('Cardiologie',          'cardiologie',          1),
  ('Dermatologie',         'dermatologie',         1),
  ('Ophtalmologie',        'ophtalmologie',        1),
  ('Pédiatrie',            'pediatrie',            1),
  ('Gynécologie',          'gynecologie',          1),
  ('Orthopédie',           'orthopedie',           1),
  ('Neurologie',           'neurologie',           1),
  ('ORL',                  'orl',                  1),
  ('Dentiste',             'dentiste',             1);

-- ============================================================
-- DONE! Verify with:
--   SHOW TABLES;
--   DESCRIBE user;
--   DESCRIBE medecin;
-- ============================================================
