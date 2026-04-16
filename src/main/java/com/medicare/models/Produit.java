package com.medicare.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Produit {
    private int id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private int quantity;
    private String type;
    private String dosage;
    private LocalDateTime expiryDate;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Produit() {}

    public Produit(int id, String name, String description, String sku,
                   BigDecimal price, int quantity, String type, String dosage,
                   LocalDateTime expiryDate, boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
        this.type = type;
        this.dosage = dosage;
        this.expiryDate = expiryDate;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name + (sku != null ? " (" + sku + ")" : ""); }
}
