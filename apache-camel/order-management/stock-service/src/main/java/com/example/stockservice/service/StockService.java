package com.example.stockservice.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stock en memoire (volontairement simple, aucune base de donnees ici) :
 * suffisant pour observer concretement l'effet de la consommation Kafka.
 */
@Service
public class StockService {

    private final Map<String, Integer> stockByProduct = new ConcurrentHashMap<>(Map.of(
            "Ordinateur portable", 50,
            "Clavier", 200,
            "Souris", 300
    ));

    public int decrement(String product, int quantity) {
        return stockByProduct.compute(product,
                (name, current) -> Math.max(0, (current == null ? 0 : current) - quantity));
    }

    public Map<String, Integer> getAll() {
        return Map.copyOf(stockByProduct);
    }
}
