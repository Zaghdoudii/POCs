# Guide de démarrage détaillé

## 1. Prérequis

- Docker + Docker Compose (le build Java se fait dans les conteneurs).
- Un client HTTP (`curl`, ou Postman/Insomnia).
- (Optionnel) `psql` en local si vous préférez vous connecter à PostgreSQL
  depuis votre machine plutôt que via `docker exec`.

## 2. Lancer l'ensemble de la stack

```bash
cd order-management
docker compose up --build
```

Premier lancement : le build Maven des deux services + le téléchargement des images
Postgres/Kafka/Kafka-UI peut prendre quelques minutes. Les lancements suivants sont
beaucoup plus rapides (couches Docker + dépendances Maven en cache).

Dans les logs, attendez de voir :

- `order-postgres` : `database system is ready to accept connections`
- `order-kafka` : le conteneur passe "healthy" (`docker compose ps`)
- `order-service` : `Started OrderApplication` + les logs Camel
  `Route: create-order-route started`
- `stock-service` : `Started StockApplication` + `Route: stock-consumer-route started`

Pour vérifier l'état de tous les conteneurs :

```bash
docker compose ps
```

## 3. Créer une commande (chemin nominal)

```bash
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Alice","product":"Clavier","quantity":3}'
```

Réponse attendue : `201 Created` avec le corps JSON de la commande créée
(`id`, `customerName`, `product`, `quantity`, `status: PENDING`, `createdAt`).

Dans les logs de `order-service`, vous devriez voir la trace de chaque étape de la
route (`Nouvelle commande recue`, `Commande validee`, `Commande enregistree en base`,
`Publication sur Kafka`, `Commande ... traitee avec succes`).

Dans les logs de `stock-service`, quelques instants après, vous devriez voir
`Evenement de commande recu` puis `Stock mis a jour pour le produit Clavier`.

## 4. Tester la validation (chemin d'erreur)

```bash
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"","product":"Souris","quantity":0}'
```

Réponse attendue : `400 Bad Request` avec un corps du type
`{"error":"Donnees de commande invalides","details":"..."}`. C'est le composant
`bean-validator` qui a rejeté le message avant même d'atteindre la base ou Kafka
(voir [docs/01-concepts-camel.md](01-concepts-camel.md)).

## 5. Vérifier la persistance

Via l'API (Spring Data JPA) :

```bash
curl http://localhost:8080/orders
curl http://localhost:8080/orders/<id-recupere-plus-haut>
```

Directement en base (pour voir que l'insertion a bien été faite par le composant
Camel SQL, indépendamment de l'API) :

```bash
docker exec -it order-postgres psql -U camel -d orders_db -c "SELECT * FROM orders;"
```

## 6. Vérifier Kafka

Via l'interface web Kafka UI : http://localhost:8090 → Topics → `order-events` →
Messages. Vous devriez voir le JSON de la commande publiée, avec en clé l'id de
la commande.

Ou en ligne de commande :

```bash
docker exec -it order-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning
```

## 7. Vérifier l'effet côté stock-service

```bash
curl http://localhost:8081/stock
```

Le produit commandé plus haut (`Clavier`) doit avoir une quantité diminuée par
rapport à sa valeur initiale (200 - 3 = 197).

## 8. Recompiler après une modification de code

```bash
docker compose up --build order-service   # ou stock-service
```

## 9. Arrêter / nettoyer

```bash
docker compose down        # arrête les conteneurs, garde les données Postgres
docker compose down -v     # + supprime le volume Postgres (repart de zéro)
```

## 10. Dépannage courant

| Symptôme                                             | Cause probable / solution |
|-------------------------------------------------------|----------------------------|
| `order-service` redémarre en boucle au tout premier lancement | Kafka/Postgres pas encore prêts ; `depends_on` avec `condition: service_healthy` devrait l'éviter, sinon relancer `docker compose up` |
| `Port is already allocated`                            | Un service local utilise déjà le port 5432/9092/8080/8081/8090 ; modifiez le mapping dans `docker-compose.yml` |
| `400` avec un message de validation alors que les données semblent correctes | Vérifiez le JSON envoyé : `quantity` doit être un nombre ≥ 1, `customerName`/`product` non vides |
| Le stock ne bouge pas après une commande               | Vérifiez les logs de `stock-service` et que le topic `order-events` contient bien le message (étape 6) |
