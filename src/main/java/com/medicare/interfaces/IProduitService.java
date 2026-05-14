package com.medicare.interfaces;

import com.medicare.models.Produit;

import java.util.List;

public interface IProduitService extends Crud<Produit> {
    Produit getById(int id);
    List<Produit> search(String keyword);
    List<Produit> getActive();
    void toggleActive(int id, boolean active);
}
