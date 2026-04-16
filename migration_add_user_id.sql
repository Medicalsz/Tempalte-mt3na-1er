-- Run this once to add user_id to an existing commande table.
USE medicare;

ALTER TABLE commande
    ADD COLUMN user_id INT NULL AFTER product_id,
    ADD CONSTRAINT fk_commande_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL;
