package com.example.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload recu par POST /orders.
 * Les annotations Jakarta Validation ci-dessous sont lues par le composant
 * Camel "bean-validator" directement dans la route (voir OrderRoutes).
 */
public class OrderRequest {

    @NotBlank(message = "Le nom du client est obligatoire")
    private String customerName;

    @NotBlank(message = "Le produit est obligatoire")
    private String product;

    @NotNull(message = "La quantite est obligatoire")
    @Min(value = 1, message = "La quantite doit etre superieure a 0")
    private Integer quantity;

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
}
