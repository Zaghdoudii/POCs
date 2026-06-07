package com.poc.hibernate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "carte_bancaire")
@Getter
@Setter
@NoArgsConstructor
public class CarteBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroCarte;
    private LocalDate dateExpiration;
    private double plafond;

    /**
     * Relation One-To-One :
     * Une carte bancaire est associée à un seul compte.
     * Un compte possède une seule carte bancaire.
     *
     * @OneToOne :
     * Définit une relation 1 à 1 entre CarteBancaire et Compte.
     *
     * @JoinColumn(name = "compte_id") :
     * Indique que la table CARTE_BANCAIRE contient la clé étrangère
     * "compte_id" qui référence la clé primaire de la table COMPTE.
     *
     * Structure simplifiée :
     *
     * TABLE COMPTE
     * +----+-------------+
     * | id | numero      |
     * +----+-------------+
     * | 1  | CP001       |
     * +----+-------------+
     *
     * TABLE CARTE_BANCAIRE
     * +----+--------------+-----------+
     * | id | numeroCarte  | compte_id |
     * +----+--------------+-----------+
     * | 10 | 123456789012 |     1     |
     * +----+--------------+-----------+
     *
     * Ainsi, la carte d'identifiant 10 est liée au compte d'identifiant 1.
     */
    @OneToOne
    @JoinColumn(name = "compte_id")
    private Compte compte;
}
