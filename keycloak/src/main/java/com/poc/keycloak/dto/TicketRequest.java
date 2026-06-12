package com.poc.keycloak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description
) {}
