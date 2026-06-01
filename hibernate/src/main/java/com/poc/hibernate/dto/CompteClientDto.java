package com.poc.hibernate.dto;

public record CompteClientDto(
        Long compteId,
        double solde,
        String nomClient,
        String prenomClient
) {}
