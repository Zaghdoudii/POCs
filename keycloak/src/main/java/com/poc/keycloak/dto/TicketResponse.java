package com.poc.keycloak.dto;

import com.poc.keycloak.entity.Ticket;

public record TicketResponse(Long id, String title, String description) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription());
    }
}
