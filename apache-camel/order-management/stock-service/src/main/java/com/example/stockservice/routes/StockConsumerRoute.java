package com.example.stockservice.routes;

import com.example.stockservice.service.StockService;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consommateur Kafka Camel : ecoute le topic "order-events" publie par
 * order-service et decremente le stock du produit commande.
 *
 * Composants Camel mis en oeuvre :
 *  - kafka (consumer)  : reception des evenements depuis le topic order-events
 *  - jackson (unmarshal): desserialisation du JSON recu en Map
 *  - onException        : isole une erreur de traitement d'un message sans planter la route
 */
@Component
public class StockConsumerRoute extends RouteBuilder {

    private final StockService stockService;

    public StockConsumerRoute(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void configure() {

        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Erreur lors du traitement de l'evenement Kafka : ${exception.message}");

        from("kafka:{{app.kafka.topic}}?brokers={{app.kafka.brokers}}&groupId={{app.kafka.group-id}}")
                .routeId("stock-consumer-route")
                .log("Evenement de commande recu : ${body}")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(this::updateStock)
                .log("Stock mis a jour pour le produit ${body[product]}");
    }

    @SuppressWarnings("unchecked")
    private void updateStock(Exchange exchange) {
        Map<String, Object> order = exchange.getIn().getBody(Map.class);
        String product = (String) order.get("product");
        int quantity = (Integer) order.get("quantity");

        int remaining = stockService.decrement(product, quantity);
        exchange.setProperty("remainingStock", remaining);
    }
}
