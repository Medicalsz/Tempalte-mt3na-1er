USE medicare;

CREATE TABLE IF NOT EXISTS produit (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT NULL,
  image_url TEXT NULL,
  image_public_id VARCHAR(255) NULL,
  sku VARCHAR(100) NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  quantity INT NOT NULL DEFAULT 0,
  type VARCHAR(100) NULL,
  dosage VARCHAR(100) NULL,
  expiry_date DATETIME NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_produit_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS commande (
  id INT AUTO_INCREMENT PRIMARY KEY,
  commande_number VARCHAR(100) NOT NULL,
  product_id INT NOT NULL,
  user_id INT NULL,
  quantity INT NOT NULL,
  total_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  status VARCHAR(50) NOT NULL DEFAULT 'en_attente',
  notes TEXT NULL,
  commande_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delivery_date DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  stripe_payment_intent_id VARCHAR(255) NULL,
  UNIQUE KEY uk_commande_number (commande_number),
  KEY idx_commande_product (product_id),
  KEY idx_commande_user (user_id),
  CONSTRAINT fk_commande_produit
    FOREIGN KEY (product_id) REFERENCES produit(id) ON DELETE CASCADE,
  CONSTRAINT fk_commande_user
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS evaluation (
  id INT AUTO_INCREMENT PRIMARY KEY,
  rendez_vous_id INT NOT NULL,
  patient_id INT NOT NULL,
  medecin_id INT NOT NULL,
  note INT NOT NULL,
  note_ponctualite INT NOT NULL,
  note_ecoute INT NOT NULL,
  note_clarte INT NOT NULL,
  commentaire TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_evaluation_rdv (rendez_vous_id),
  KEY idx_evaluation_patient (patient_id),
  KEY idx_evaluation_medecin (medecin_id),
  CONSTRAINT fk_evaluation_rdv
    FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous(id) ON DELETE CASCADE,
  CONSTRAINT fk_evaluation_patient
    FOREIGN KEY (patient_id) REFERENCES user(id) ON DELETE CASCADE,
  CONSTRAINT fk_evaluation_medecin
    FOREIGN KEY (medecin_id) REFERENCES medecin(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
