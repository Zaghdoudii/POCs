# Order Management — Projet d'apprentissage Apache Camel

Projet minimal mais complet pour **apprendre Apache Camel** en pratique : une API de
gestion de commandes ou toute la logique d'orchestration (validation, persistance,
transformation, messaging) passe par des **routes Camel**, pas par du code métier
dispersé dans des services.

> Le but n'est pas d'avoir un système de gestion de commandes "production-ready",
> mais de manipuler concrètement les briques Camel les plus utilisées en entreprise.
> Voir [docs/01-concepts-camel.md](docs/01-concepts-camel.md) pour la théorie associée
> à chaque bout de code, et [docs/03-exercices.md](docs/03-exercices.md) pour continuer
> à progresser une fois ce projet pris en main.

## Architecture

```
                POST /orders
                     │
                     ▼
             ┌───────────────┐
             │ OrderController│  (Spring MVC, expose l'API REST)
             └───────┬───────┘
                     │ ProducerTemplate.send("direct:createOrder")
                     ▼
             ┌────────────────────────────┐
             │      Route Camel           │
             │  direct:createOrder        │
             │                            │
             │ 1. bean-validator (valide) │
             │ 2. process (DTO -> id)     │
             │ 3. sql (INSERT JDBC) ──────┼────► PostgreSQL
             │ 4. jackson (marshal JSON)  │
             │ 5. kafka (publish) ────────┼────► Kafka topic "order-events"
             └────────────────────────────┘
                                                      │
                                                      ▼
                                          ┌───────────────────────┐
                                          │     stock-service      │
                                          │  route Camel Kafka     │
                                          │  consumer + jackson    │
                                          │  (unmarshal) + stock   │
                                          │  en mémoire            │
                                          └───────────────────────┘
```

## Stack technique

- **Spring Boot 3.3** (Java 21)
- **Apache Camel 4.8** (`camel-spring-boot-starter`)
- **PostgreSQL 16** — persistance des commandes
- **Kafka (KRaft, image Bitnami)** — événements de commande
- **Docker Compose** — orchestration de l'ensemble
- **Kafka UI** (optionnel) — pour observer visuellement les messages Kafka

## Structure du projet

```
order-management/
├── docker-compose.yml
├── README.md
├── docs/
│   ├── 01-concepts-camel.md   # Vocabulaire Camel + explication de chaque composant utilisé
│   ├── 02-guide-demarrage.md  # Étapes détaillées pour lancer et tester le projet
│   └── 03-exercices.md        # Pour aller plus loin une fois le projet pris en main
├── order-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/example/orderservice/
│       │   ├── OrderApplication.java
│       │   ├── controller/OrderController.java   # POST/GET /orders
│       │   ├── entity/Order.java                 # entité JPA (lecture seule)
│       │   ├── entity/OrderStatus.java
│       │   ├── repository/OrderRepository.java   # Spring Data JPA (lecture)
│       │   ├── routes/OrderRoutes.java           # LA route Camel principale
│       │   ├── dto/OrderRequest.java             # payload entrant + annotations validation
│       │   ├── dto/OrderResponse.java            # objet manipulé dans la route
│       │   └── config/CamelJacksonConfig.java    # ObjectMapper partagé Spring/Camel
│       └── resources/application.yml
└── stock-service/
    ├── pom.xml
    ├── Dockerfile
    └── src/main/
        ├── java/com/example/stockservice/
        │   ├── StockApplication.java
        │   ├── routes/StockConsumerRoute.java    # consumer Kafka Camel
        │   ├── service/StockService.java         # stock en mémoire
        │   └── controller/StockController.java   # GET /stock (vérification)
        └── resources/application.yml
```

## Démarrage rapide

Prérequis : Docker + Docker Compose (le build Java se fait dans les conteneurs,
inutile d'installer Java/Maven en local).

```bash
cd order-management
docker compose up --build
```

Attendre que les 5 conteneurs soient démarrés (Postgres et Kafka ont un healthcheck,
`order-service` et `stock-service` attendent qu'ils soient prêts avant de démarrer).

Services exposés :

| Service      | URL                          | Usage                                   |
|--------------|-------------------------------|------------------------------------------|
| order-service| http://localhost:8080/orders  | API REST commandes                       |
| stock-service| http://localhost:8081/stock   | Consultation du stock (effet de Kafka)   |
| Kafka UI     | http://localhost:8090          | Observer les topics/messages Kafka       |
| PostgreSQL   | localhost:5432 (camel/camel)   | Base `orders_db`, table `orders`         |

## Tester le flux complet

```bash
# 1. Créer une commande valide
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Alice","product":"Clavier","quantity":3}'

# 2. Créer une commande invalide (déclenche le composant bean-validator)
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"","product":"Souris","quantity":0}'

# 3. Vérifier la persistance (lecture via Spring Data JPA)
curl http://localhost:8080/orders

# 4. Vérifier que stock-service a bien consommé l'événement Kafka
curl http://localhost:8081/stock
```

Un guide pas à pas plus détaillé (avec vérification directe dans PostgreSQL et Kafka)
se trouve dans [docs/02-guide-demarrage.md](docs/02-guide-demarrage.md).

## Concepts Apache Camel abordés

| Concept Camel          | Où le voir dans le code                              |
|------------------------|-------------------------------------------------------|
| RouteBuilder / DSL Java| `OrderRoutes`, `StockConsumerRoute`                    |
| Exchange / Message     | processeurs (`this::toOrderResponse`, `updateStock`)   |
| `direct:` component    | point d'entrée interne de la route commande            |
| `bean-validator:`      | validation Jakarta Validation dans la route             |
| `sql:` (JDBC)          | insertion des commandes en base PostgreSQL              |
| `jackson` dataformat   | `marshal()/unmarshal()` Objet ↔ JSON                    |
| `kafka:` component     | producteur (order-service) et consommateur (stock-service)|
| `onException`          | gestion centralisée des erreurs (mini Dead Letter Channel)|
| Property placeholders  | `{{app.kafka.topic}}`, `{{app.kafka.brokers}}`          |
| ProducerTemplate       | pont entre le contrôleur REST et la route Camel          |

Le détail pédagogique de chaque ligne se trouve dans
[docs/01-concepts-camel.md](docs/01-concepts-camel.md).

## Arrêter / nettoyer

```bash
docker compose down        # arrête les conteneurs
docker compose down -v     # + supprime le volume PostgreSQL (repart de zéro)
```
