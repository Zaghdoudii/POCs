package com.example.orderservice.routes;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.validator.BeanValidationException;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.spi.DataFormat;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Route Camel centrale du projet : recoit une commande, la valide, l'enregistre
 * en base via le composant SQL, la transforme en JSON et la publie sur Kafka.
 *
 * Composants Camel mis en oeuvre ici (voir docs/01-concepts-camel.md pour le detail) :
 *  - direct            : point d'entree interne, appele depuis OrderController via ProducerTemplate
 *  - bean-validator     : validation des annotations Jakarta Validation portees par OrderRequest
 *  - sql (JDBC)         : insertion en base PostgreSQL avec parametres nommes
 *  - jackson (marshal)  : transformation de l'objet Java en JSON
 *  - kafka              : publication de l'evenement sur le topic "order-events"
 *  - onException        : gestion centralisee des erreurs (equivalent d'un Dead Letter Channel)
 */
@Component
public class OrderRoutes extends RouteBuilder {

    public static final String RESULT_HEADER = "OrderResult";

    // DataFormat construit explicitement avec l'ObjectMapper partage (config/CamelJacksonConfig)
    // afin de garantir la prise en compte du module JavaTimeModule pour LocalDateTime.
    private final DataFormat orderJson;

    public OrderRoutes(ObjectMapper objectMapper) {
        this.orderJson = new JacksonDataFormat(objectMapper, OrderResponse.class);
    }

    @Override
    public void configure() {

        // --- Gestion des erreurs de validation ---
        onException(BeanValidationException.class)
                .handled(true)
                .log(LoggingLevel.WARN, "Validation echouee : ${exception.message}")
                .setHeader(RESULT_HEADER, constant("VALIDATION_ERROR"))
                .process(exchange -> {
                    BeanValidationException e =
                            exchange.getProperty(Exchange.EXCEPTION_CAUGHT, BeanValidationException.class);
                    exchange.getMessage().setBody(Map.of(
                            "error", "Donnees de commande invalides",
                            "details", e.getMessage()
                    ));
                });

        // --- Gestion des erreurs techniques (base, Kafka, etc.) ---
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Erreur inattendue dans la route : ${exception.message}")
                .setHeader(RESULT_HEADER, constant("ERROR"))
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    exchange.getMessage().setBody(Map.of(
                            "error", "Erreur interne lors du traitement de la commande",
                            "details", e.getMessage()
                    ));
                });

        from("direct:createOrder")
                .routeId("create-order-route")
                .log("Nouvelle commande recue : ${body}")

                // 1. Validation (composant camel-bean-validator, annotations de OrderRequest)
                .to("bean-validator://orderRequestValidation")

                // 2. Transformation DTO entrant -> DTO metier (id genere, statut initial)
                //    Le DTO est aussi garde de cote (property d'Exchange) pour etre reutilise
                //    plus loin comme reponse HTTP, une fois le corps transforme par les etapes suivantes.
                .process(this::toOrderResponse)
                .log("Commande validee, id genere : ${exchangeProperty.orderResponse.id}")

                // 3. Enregistrement en base via le composant Camel SQL/JDBC.
                //    Le composant sql resout les parametres nommes ":#xxx" en cherchant les cles
                //    correspondantes dans une Map (d'ou la conversion explicite ci-dessous).
                .process(this::toSqlParameters)
                .to("sql:INSERT INTO orders (id, customer_name, product, quantity, status, created_at)"
                        + " VALUES (:#id, :#customerName, :#product, :#quantity, :#status, :#createdAt)"
                        + "?dataSource=#dataSource")
                .log("Commande enregistree en base : ${exchangeProperty.orderResponse.id}")

                // 4. On restaure le DTO metier comme corps courant, puis on prepare la cle Kafka
                .setBody(exchangeProperty("orderResponse"))
                .setHeader(KafkaConstants.KEY, simple("${body.id}"))

                // 5. Transformation en JSON (composant camel-jackson, ObjectMapper partage)
                .marshal(orderJson)
                .log("Publication sur Kafka : ${body}")

                // 6. Publication de l'evenement sur Kafka
                .to("kafka:{{app.kafka.topic}}?brokers={{app.kafka.brokers}}")

                // 7. Restauration du DTO (et non le JSON Kafka) comme reponse HTTP
                .setBody(exchangeProperty("orderResponse"))
                .setHeader(RESULT_HEADER, constant("SUCCESS"))
                .log("Commande ${body.id} traitee avec succes");
    }

    private void toOrderResponse(Exchange exchange) {
        OrderRequest request = exchange.getIn().getBody(OrderRequest.class);
        OrderResponse response = new OrderResponse(
                UUID.randomUUID().toString(),
                request.getCustomerName(),
                request.getProduct(),
                request.getQuantity(),
                OrderStatus.PENDING.name(),
                LocalDateTime.now()
        );
        exchange.setProperty("orderResponse", response);
        exchange.getIn().setBody(response);
    }

    private void toSqlParameters(Exchange exchange) {
        OrderResponse response = exchange.getIn().getBody(OrderResponse.class);
        Map<String, Object> params = new HashMap<>();
        params.put("id", response.getId());
        params.put("customerName", response.getCustomerName());
        params.put("product", response.getProduct());
        params.put("quantity", response.getQuantity());
        params.put("status", response.getStatus());
        params.put("createdAt", response.getCreatedAt());
        exchange.getIn().setBody(params);
    }
}
