-- =====================================================
-- Migration : ajout des colonnes image (Cloudinary)
-- + nouveau statut paiement pour la commande (Stripe).
-- =====================================================

ALTER TABLE produit
    ADD COLUMN image_url VARCHAR(500) NULL AFTER description,
    ADD COLUMN image_public_id VARCHAR(255) NULL AFTER image_url;

-- The status column already exists; we just rely on a new value 'en_attente_paiement'.
-- If the column is an ENUM, extend it; otherwise nothing to do.
-- Example for ENUM:
-- ALTER TABLE commande
--     MODIFY status ENUM('en_attente_paiement','en_attente','confirmee','livree','annulee')
--     NOT NULL DEFAULT 'en_attente';
