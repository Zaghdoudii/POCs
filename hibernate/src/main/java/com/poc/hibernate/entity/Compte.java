package com.poc.hibernate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compte")
@Getter
@Setter
@NoArgsConstructor
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroCompte;
    private double solde;
    private LocalDateTime dateOuverture;

    @Enumerated(EnumType.STRING)
    private StatutCompte statutCompte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;

    @OneToMany(mappedBy = "compte", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Operation> operations = new ArrayList<>();

    // fetch = LAZY : par défaut @OneToOne est EAGER (jointure à chaque chargement de Compte)
    // ATTENTION cascade côté mappedBy : pour que la FK compte_id soit correctement
    // remplie en base lors d'un persist, il faut toujours synchroniser les deux côtés :
    //   compte.setCarteBancaire(carte);
    //   carte.setCompte(compte);   ← sinon compte_id = NULL en base
    @OneToOne(mappedBy = "compte", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CarteBancaire carteBancaire;
}
