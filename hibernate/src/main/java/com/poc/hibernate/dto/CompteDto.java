package com.poc.hibernate.dto;

import com.poc.hibernate.entity.StatutCompte;

public record CompteDto(
        Long id,
        String numeroCompte,
        double solde,
        StatutCompte statut
) {
}
