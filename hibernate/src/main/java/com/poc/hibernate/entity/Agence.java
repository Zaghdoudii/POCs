package com.poc.hibernate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agence")
@Getter
@Setter
@NoArgsConstructor
public class Agence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String nom;
    private String ville;
    private String adresse;

    /**
     * Relation One-To-Many :
     * Une agence peut gérer plusieurs comptes.
     * Chaque compte appartient à une seule agence.
     *
     * mappedBy = "agence" :
     * La relation est pilotée par l'attribut "agence"
     * présent dans l'entité Compte.
     * La clé étrangère est donc stockée dans la table COMPTE.
     *
     * cascade = CascadeType.ALL :
     * Toutes les opérations effectuées sur l'agence
     * (persist, merge, remove, refresh, detach)
     * sont automatiquement propagées aux comptes associés.
     *
     * Exemple :
     * - Sauvegarde d'une agence => sauvegarde des comptes.
     * - Suppression d'une agence => suppression des comptes.
     *
     * orphanRemoval = true :
     * Si un compte est retiré de la liste "comptes",
     * il sera automatiquement supprimé de la base de données.
     *
     * Exemple :
     * agence.getComptes().remove(compte);
     * => le compte sera supprimé en base après la transaction.
     */
    @OneToMany(mappedBy = "agence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Compte> comptes = new ArrayList<>();
}
