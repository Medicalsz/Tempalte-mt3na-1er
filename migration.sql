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

CREATE TABLE IF NOT EXISTS partner (
  id                INT(11) NOT NULL AUTO_INCREMENT,
  name              VARCHAR(255) NOT NULL,
  type_partenaire   VARCHAR(120) NULL,
  email             VARCHAR(255) NULL,
  telephone         VARCHAR(50) NULL,
  adresse           TEXT NULL,
  statut            VARCHAR(50) NULL DEFAULT 'actif',
  date_partenariat  DATE NULL,
  image_name        VARCHAR(500) NULL,
  updated_at        DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collaboration (
  id            INT(11) NOT NULL AUTO_INCREMENT,
  partner_id    INT(11) NOT NULL,
  user_id       INT(11) NULL,
  date_debut    DATE NULL,
  date_fin      DATE NULL,
  titre         VARCHAR(255) NOT NULL,
  description   TEXT NULL,
  statut        VARCHAR(50) NULL DEFAULT 'en_attente',
  image_name    VARCHAR(500) NULL,
  updated_at    DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS partner_rating (
  id          INT(11) NOT NULL AUTO_INCREMENT,
  partner_id  INT(11) NOT NULL,
  author_id   INT(11) NULL,
  rating      INT(11) NOT NULL,
  comment     TEXT NULL,
  sentiment   VARCHAR(50) NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
  FOREIGN KEY (author_id) REFERENCES user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS badges (
  id           VARCHAR(80) NOT NULL,
  name         VARCHAR(120) NOT NULL,
  description  TEXT NULL,
  icon_path    VARCHAR(500) NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS partner_badges (
  partner_id  INT(11) NOT NULL,
  badge_id    VARCHAR(80) NOT NULL,
  PRIMARY KEY (partner_id, badge_id),
  FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
  FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE
);

INSERT IGNORE INTO badges (id, name, description, icon_path) VALUES
('reliable', 'Reliable', 'Partenaire fiable', '/icons/badges/reliable.png'),
('superstar', 'Superstar', 'Partenaire exceptionnel', '/icons/badges/superstar.png'),
('top_rated', 'Top rated', 'Tres bien note', '/icons/badges/top_rated.png'),
('veteran', 'Veteran', 'Partenaire experimente', '/icons/badges/veteran.png');

-- ============================================================
-- Forum Module (from mohamed/forum branch)
-- ============================================================

CREATE TABLE IF NOT EXISTS forum_topic (
    id INT AUTO_INCREMENT PRIMARY KEY,
    author_id INT NOT NULL,
    reported_by_id INT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    video_url VARCHAR(500) NULL,
    summary TEXT NULL,
    tags VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    is_reported TINYINT(1) NOT NULL DEFAULT 0,
    is_hidden TINYINT(1) NOT NULL DEFAULT 0,
    reported_reason VARCHAR(500) NULL,
    reported_at DATETIME NULL,
    FOREIGN KEY (author_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_forum_topic_author (author_id),
    INDEX idx_forum_topic_created (created_at)
);

CREATE TABLE IF NOT EXISTS forum_comment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    author_id INT NOT NULL,
    topic_id INT NOT NULL,
    parent_id INT NULL,
    reported_by_id INT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_reported TINYINT(1) NOT NULL DEFAULT 0,
    is_hidden TINYINT(1) NOT NULL DEFAULT 0,
    reported_reason VARCHAR(500) NULL,
    reported_at DATETIME NULL,
    FOREIGN KEY (author_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (topic_id) REFERENCES forum_topic(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES forum_comment(id) ON DELETE SET NULL,
    INDEX idx_forum_comment_topic (topic_id),
    INDEX idx_forum_comment_author (author_id)
);

CREATE TABLE IF NOT EXISTS forum_comment_reaction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_reaction (comment_id, user_id, type),
    FOREIGN KEY (comment_id) REFERENCES forum_comment(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

-- ============================================================
-- DONE! Verify with:
--   DESCRIBE user;
--   DESCRIBE medecin;
--   SHOW TABLES;
-- ============================================================
