package com.poc.hibernate.repository;

import com.poc.hibernate.dto.ClientDto;
import com.poc.hibernate.dto.ClientCompteCountDto;
import com.poc.hibernate.entity.Client;
import com.poc.hibernate.entity.StatutCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {


    /**
     * Retourne tous les clients ayant le prénom donné.
     */
    List<Client> findByPrenom(String prenom);

    /**
     * Retourne tous les clients ayant le cin donné.
     */
    Optional<Client> findByCin(int cin);

    /**
     * Calcule la moyenne des soldes de tous les comptes
     * appartenant à un client donné.
     *
     * @param clientId identifiant du client
     * @return la moyenne des soldes des comptes du client,
     * ou Optional.empty() si le client ne possède aucun compte
     */
    @Query("""
                SELECT AVG(c.solde)
                FROM Compte c
                WHERE c.client.id = :clientId
            """)
    Optional<Double> calculerMoyenneSoldeParClient(@Param("clientId") Long clientId);

    /**
     * Récupère tous les clients avec leurs comptes associés
     * en une seule requête SQL.
     * <p>
     * JOIN FETCH permet de charger immédiatement la collection
     * des comptes et d'éviter le problème N+1 Select.
     * <p>
     * DISTINCT évite les doublons de clients qui peuvent
     * apparaître lorsqu'un client possède plusieurs comptes.
     *
     * @return la liste des clients avec leurs comptes chargés
     */
    @Query("SELECT DISTINCT cl FROM Client cl JOIN FETCH cl.comptes")
    List<Client> findAllClientsWithComptes();

    /**
     * Récupère tous les clients sous forme de DTO.
     * <p>
     * La projection JPQL "new" permet de construire directement
     * des objets ClientDto sans charger l'entité Client complète.
     * <p>
     * Cette approche améliore les performances lorsque seules
     * certaines informations sont nécessaires.
     * <p>
     * Données retournées :
     * - id
     * - nom
     * - prénom
     *
     * @return la liste des clients sous forme de ClientDto
     */
    @Query("""
            SELECT new com.poc.hibernate.dto.ClientDto(
                c.id,
                c.nom,
                c.prenom
            )
            FROM Client c
            """)
    List<ClientDto> findAllClientDtos();

    /**
     * Récupère la liste des clients avec le nombre de comptes associés.
     * <p>
     * LEFT JOIN permet d'inclure également les clients qui ne possèdent
     * aucun compte.
     * <p>
     * COUNT(cp) calcule le nombre de comptes pour chaque client.
     * <p>
     * GROUP BY regroupe les résultats par client afin d'obtenir
     * un seul résultat par client.
     * <p>
     * La projection JPQL "new" permet de construire directement
     * des objets ClientCompteCountDto.
     * <p>
     * Données retournées :
     * - id du client
     * - nom du client
     * - nombre de comptes
     * <p>
     * Exemple :
     * <p>
     * Ali    -> 3 comptes
     * Ahmed  -> 1 compte
     * Sonia  -> 0 compte
     *
     * @return la liste des clients avec leur nombre de comptes
     */
    @Query("""
            SELECT new com.poc.hibernate.dto.ClientCompteCountDto(
                c.id,
                c.nom,
                COUNT(cp)
            )
            FROM Client c
            LEFT JOIN c.comptes cp
            GROUP BY c.id, c.nom
            """)
    List<ClientCompteCountDto> findAllClientDtos2();


    /**
     * Recherche des clients selon plusieurs critères optionnels.
     * <p>
     * Les critères peuvent être combinés librement :
     * - nom du client
     * - prénom du client
     * - solde minimum d'un compte
     * - statut du compte
     * - ville de l'agence
     * <p>
     * Si un paramètre est null, il n'est pas pris en compte
     * dans le filtrage.
     * <p>
     * La requête utilise :
     * - JOIN entre Client et Compte
     * - JOIN entre Compte et Agence
     * - DISTINCT pour éviter les doublons lorsqu'un client
     * possède plusieurs comptes correspondant aux critères
     * <p>
     * Exemples :
     * <p>
     * searchClients("Ali", null, null, null, null)
     * -> clients dont le nom contient "Ali"
     * <p>
     * searchClients(null, null, 1000.0, ACTIF, null)
     * -> clients ayant au moins un compte actif avec un solde >= 1000
     * <p>
     * searchClients(null, null, null, null, "Tunis")
     * -> clients rattachés à une agence située à Tunis
     *
     * @param nom      nom du client (recherche partielle)
     * @param prenom   prénom du client (recherche partielle)
     * @param soldeMin solde minimum du compte
     * @param statut   statut du compte
     * @param ville    ville de l'agence
     * @return la liste des clients correspondant aux critères
     */
    @Query("""
            SELECT DISTINCT c
            FROM Client c
            LEFT JOIN c.comptes cp
            LEFT JOIN cp.agence a
            WHERE
                (:nom IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%')))
            AND (:prenom IS NULL OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :prenom, '%')))
            AND (:soldeMin IS NULL OR cp.solde >= :soldeMin)
            AND (:statut IS NULL OR cp.statutCompte = :statut)
            AND (:ville IS NULL OR LOWER(a.ville) LIKE LOWER(CONCAT('%', :ville, '%')))
            """)
    List<Client> searchClients(
            @Param("nom") String nom,
            @Param("prenom") String prenom,
            @Param("soldeMin") Double soldeMin,
            @Param("statut") StatutCompte statut,
            @Param("ville") String ville
    );
}
