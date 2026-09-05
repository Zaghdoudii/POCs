package com.example.orderservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Un seul ObjectMapper partage par Spring MVC (reponses REST) et par le
 * dataformat camel-jackson utilise dans OrderRoutes (marshal().json(...)).
 * Camel recherche automatiquement un bean ObjectMapper dans le registry
 * Spring : pas besoin de le referencer explicitement dans la route.
 */
@Configuration
public class CamelJacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
