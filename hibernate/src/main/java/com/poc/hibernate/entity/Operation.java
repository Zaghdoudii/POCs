package com.poc.hibernate.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private double montant;
    private LocalDateTime dateOperation;
    private String description;

    /**
     * Stocke l'énumération sous forme de texte (ACTIF, BLOQUE, FERME)
     * plutôt que sous forme numérique (0, 1, 2).
     * Plus lisible et plus sûr en cas d'évolution de l'enum.
     */
    @Enumerated(EnumType.STRING)
    private TypeOperation type;

    /**
     * Relation Many-To-One :
     * Plusieurs opérations peuvent être associées au même compte.
     * Chaque opération appartient obligatoirement à un seul compte.
     *
     * Exemple :
     *
     * Compte C001
     * ├── Dépôt 100€
     * ├── Retrait 50€
     * ├── Virement 200€
     * └── Paiement 30€
     *
     * Les 4 opérations référencent le même compte.
     *
     * @ManyToOne :
     * Indique que plusieurs enregistrements de la table OPERATION
     * peuvent être liés à un même enregistrement de la table COMPTE.
     *
     * @JoinColumn(name = "compte_id") :
     * Crée la colonne "compte_id" dans la table OPERATION.
     * Cette colonne contient l'identifiant du compte auquel
     * l'opération est rattachée.
     *
     * Structure simplifiée :
     *
     * TABLE COMPTE
     * +----+-------------+
     * | id | numero      |
     * +----+-------------+
     * | 1  | C001        |
     * +----+-------------+
     *
     * TABLE OPERATION
     * +----+-----------+-----------+
     * | id | montant   | compte_id |
     * +----+-----------+-----------+
     * | 10 | 100.00    |     1     |
     * | 11 |  50.00    |     1     |
     * | 12 | 200.00    |     1     |
     * +----+-----------+-----------+
     *
     * fetch = FetchType.LAZY :
     * Le compte n'est pas chargé immédiatement lorsque
     * l'opération est récupérée depuis la base.
     *
     * Hibernate chargera le compte uniquement lorsque
     * l'on accédera à :
     *
     * operation.getCompte()
     *
     * Cela améliore les performances lorsque les informations
     * du compte ne sont pas nécessaires.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id")
    private Compte compte;
}
