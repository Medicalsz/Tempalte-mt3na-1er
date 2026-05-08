package com.medicare.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Commande {
    private int id;
    private String commandeNumber;
    private int productId;
    private String productName; // joined field for display
    private int userId;
    private String userFullName; // joined field for display
    private int quantity;
    private BigDecimal totalPrice;
    private String status;
    private String notes;
    private LocalDateTime commandeDate;
    private LocalDateTime deliveryDate;
    private LocalDateTime createdAt;
    private String stripePaymentIntentId;

    public Commande() {}

    public Commande(int id, String commandeNumber, int productId, int quantity,
                    BigDecimal totalPrice, String status, String notes,
                    LocalDateTime commandeDate, LocalDateTime deliveryDate,
                    LocalDateTime createdAt, String stripePaymentIntentId) {
        this.id = id;
        this.commandeNumber = commandeNumber;
        this.productId = productId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.notes = notes;
        this.commandeDate = commandeDate;
        this.deliveryDate = deliveryDate;
        this.createdAt = createdAt;
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCommandeNumber() { return commandeNumber; }
    public void setCommandeNumber(String commandeNumber) { this.commandeNumber = commandeNumber; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCommandeDate() { return commandeDate; }
    public void setCommandeDate(LocalDateTime commandeDate) { this.commandeDate = commandeDate; }

    public LocalDateTime getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDateTime deliveryDate) { this.deliveryDate = deliveryDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String id) { this.stripePaymentIntentId = id; }

    @Override
    public String toString() {
        return commandeNumber != null ? commandeNumber : ("Commande #" + id);
    }
}
