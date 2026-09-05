package com.example.orderservice.dto;

import java.time.LocalDateTime;

/**
 * Objet manipule par la route Camel entre l'insertion en base (camel-sql) et
 * la publication Kafka (les proprietes de cette classe sont utilisees comme
 * parametres nommes ":#xxx" dans la requete SQL, cf. OrderRoutes).
 */
public class OrderResponse {

    private String id;
    private String customerName;
    private String product;
    private Integer quantity;
    private String status;
    private LocalDateTime createdAt;

    public OrderResponse() {
    }

    public OrderResponse(String id, String customerName, String product, Integer quantity,
                          String status, LocalDateTime createdAt) {
        this.id = id;
        this.customerName = customerName;
        this.product = product;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
