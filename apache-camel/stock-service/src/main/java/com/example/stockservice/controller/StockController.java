package com.example.stockservice.controller;

import com.example.stockservice.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de verification uniquement : permet de constater visuellement
 * l'effet de la consommation Kafka (curl http://localhost:8081/stock).
 */
@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stock")
    public Map<String, Integer> getStock() {
        return stockService.getAll();
    }
}
