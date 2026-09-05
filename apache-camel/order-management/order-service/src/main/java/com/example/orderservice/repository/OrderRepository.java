package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA, utilise uniquement pour la lecture (GET /orders, GET /orders/{id}).
 * L'ecriture passe par la route Camel + composant SQL (voir OrderRoutes) afin de
 * decouvrir concretement le composant camel-sql / JDBC.
 */
public interface OrderRepository extends JpaRepository<Order, String> {
}
