package com.poc.hibernate.repository;

import com.poc.hibernate.dto.CompteClientDto;
import com.poc.hibernate.dto.CompteDto;
import com.poc.hibernate.entity.Compte;
import com.poc.hibernate.entity.StatutCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompteRepository extends JpaRepository<Compte, Long> {
    List<Compte> findByStatutCompte(StatutCompte statutCompte);

    @Query("SELECT c FROM Compte c WHERE c.client.id = :clientId")
    List<Compte> findComptesByClientId(@Param("clientId") Long clientId);

    @Query("SELECT c FROM Compte c WHERE c.solde >= :solde")
    List<Compte> findComptesBySoldeMin(@Param("solde") double solde);

    @Query("SELECT DISTINCT c from Compte c JOIN FETCH c.operations ")
    List<Compte> findAllComptesWithOperations();

    /**
     * Calcule des statistiques pour chaque compte.
     *
     * Pour chaque compte, la requête retourne :
     * - l'identifiant du compte
     * - le nombre total d'opérations
     * - la somme des montants des opérations
     *
     * GROUP BY permet de regrouper les opérations
     * par compte afin de calculer les agrégats.
     *
     * Structure du résultat Object[] :
     * - index 0 : Long   -> id du compte
     * - index 1 : Long   -> nombre d'opérations (COUNT)
     * - index 2 : Double -> somme des montants (SUM)
     *
     * Exemple :
     *
     * Compte 1 :
     *   3 opérations (100 + 50 + 200)
     *   => [1, 3, 350.0]
     *
     * Compte 2 :
     *   2 opérations (300 + 150)
     *   => [2, 2, 450.0]
     *
     * @return la liste des statistiques par compte
     */
    @Query("""
            SELECT c.id, COUNT(o), SUM(o.montant)
            FROM Compte c
            JOIN c.operations o
            GROUP BY c.id
            """)
    List<Object[]> findCompteStats();

    @Query("""
            SELECT new com.poc.hibernate.dto.CompteDto(
                c.id,
                c.numeroCompte,
                c.solde,
                c.statutCompte
            )
            FROM Compte c
            """)
    List<CompteDto> findAllCompteDtos();

    /**
     * Récupère la liste des comptes avec les informations
     * du client propriétaire sous forme de DTO.
     *
     * La requête effectue une jointure entre Compte et Client
     * afin de récupérer les données des deux entités.
     *
     * La projection JPQL "new" permet de construire directement
     * des objets CompteClientDto sans charger les entités complètes.
     *
     * Données retournées :
     * - identifiant du compte
     * - solde du compte
     * - nom du client
     * - prénom du client
     *
     * Exemple :
     *
     * Compte 1 | Solde : 1500.00 | Ali Ben Salah
     * Compte 2 | Solde : 2500.00 | Ahmed Trabelsi
     *
     * Cette approche est plus performante lorsque seules
     * quelques informations doivent être affichées dans une liste.
     *
     * @return la liste des comptes avec les informations du client
     */
    @Query("""
            SELECT new com.poc.hibernate.dto.CompteClientDto(
                c.id,
                c.solde,
                cl.nom,
                cl.prenom
            )
            FROM Compte c
            JOIN c.client cl
            """)
    List<CompteClientDto> findAllCompteClientDTOs();
}
