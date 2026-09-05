package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.routes.OrderRoutes;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final ProducerTemplate producerTemplate;
    private final OrderRepository orderRepository;

    public OrderController(ProducerTemplate producerTemplate, OrderRepository orderRepository) {
        this.producerTemplate = producerTemplate;
        this.orderRepository = orderRepository;
    }

    /**
     * Point d'entree REST : delegue tout le traitement (validation, persistance,
     * publication Kafka) a la route Camel "direct:createOrder" via le ProducerTemplate.
     */
    @PostMapping
    public ResponseEntity<Object> createOrder(@RequestBody OrderRequest request) {
        Exchange result = producerTemplate.send("direct:createOrder",
                exchange -> exchange.getIn().setBody(request));

        String status = result.getMessage().getHeader(OrderRoutes.RESULT_HEADER, String.class);
        Object body = result.getMessage().getBody();

        return switch (status) {
            case "SUCCESS" -> ResponseEntity.status(HttpStatus.CREATED).body(body);
            case "VALIDATION_ERROR" -> ResponseEntity.badRequest().body(body);
            default -> ResponseEntity.internalServerError().body(body);
        };
    }

    /** Lecture seule via Spring Data JPA, en contrepoint de l'ecriture via Camel SQL. */
    @GetMapping
    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
