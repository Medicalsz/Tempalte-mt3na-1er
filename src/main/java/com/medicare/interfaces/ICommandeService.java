package com.medicare.interfaces;

import com.medicare.models.Commande;

import java.util.List;

public interface ICommandeService extends Crud<Commande> {
    Commande getById(int id);
    List<Commande> getByStatus(String status);
    List<Commande> getByProduct(int productId);
    List<Commande> getByUser(int userId);
    List<Commande> search(String keyword);
    void updateStatus(int id, String status);
}
