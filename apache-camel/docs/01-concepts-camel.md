# Concepts Apache Camel utilisés dans ce projet

Ce document explique le vocabulaire Camel et pourquoi chaque composant a été choisi
dans `OrderRoutes` (order-service) et `StockConsumerRoute` (stock-service).
Références officielles : https://camel.apache.org/manual/ et
https://camel.apache.org/components/4.8.x/index.html

## 1. Vocabulaire de base

- **CamelContext** : le "conteneur" runtime de Camel. Avec `camel-spring-boot-starter`,
  il est créé et géré automatiquement par Spring (un bean `CamelContext`).
- **Route** : une chaîne de traitements reliant une **source** (`from(...)`) à une ou
  plusieurs **destinations** (`to(...)`), avec des étapes intermédiaires.
- **RouteBuilder** : classe Java où l'on décrit une ou plusieurs routes avec la
  **DSL Java** (`OrderRoutes` et `StockConsumerRoute` en héritent).
- **Endpoint** : une URI Camel (ex: `sql:...`, `kafka:...`, `direct:...`) qui représente
  une source ou une destination concrète. Chaque préfixe (`sql`, `kafka`, `direct`,
  `bean-validator`) correspond à un **Component** différent.
- **Exchange** : l'objet qui circule dans une route. Il contient :
  - un **In Message** (le corps + les headers du message, accessible via `${body}`,
    `${header.xxx}`) ;
  - des **Exchange Properties** (des variables attachées à tout l'échange, pas
    seulement au message courant — utilisées ici via `exchange.setProperty(...)`
    et l'expression `exchangeProperty("...")`) ;
  - éventuellement une exception, en cas d'erreur (`Exchange.EXCEPTION_CAUGHT`).
- **Processor** : interface fonctionnelle `void process(Exchange exchange)`. C'est
  le point d'extension Java le plus simple pour écrire de la logique métier dans
  une route (voir `OrderRoutes.toOrderResponse` ou `StockConsumerRoute.updateStock`).
- **Expression Language "Simple"** : le mini-langage `${body}`, `${body.id}`,
  `${header.xxx}`, `${exception.message}` utilisé dans `.log(...)`, `.setHeader(...)`,
  `.simple(...)`. Pratique pour éviter d'écrire un Processor pour des accès simples.
- **Data Format** : un convertisseur bidirectionnel objet <-> représentation externe.
  Ici `JsonLibrary.Jackson` avec `.marshal()` (objet -> JSON) et `.unmarshal()`
  (JSON -> objet/Map).
- **ProducerTemplate** : l'API utilisée en dehors d'une route (ici dans
  `OrderController`) pour envoyer un message vers une route Camel
  (`producerTemplate.send("direct:createOrder", ...)`). C'est le pont entre le monde
  "Spring MVC classique" et le monde "Camel".

## 2. Les composants utilisés, un par un

### `direct:` — point d'entrée interne

```java
from("direct:createOrder")
```

`direct` est un composant **synchrone et in-memory** : il permet d'appeler une route
Camel depuis du code Java (ici le contrôleur REST) sans passer par le réseau. C'est
l'équivalent d'un appel de méthode, mais qui bénéficie de tout l'outillage Camel
(gestion d'erreurs, logs, etc.). Documentation :
https://camel.apache.org/components/4.8.x/direct-component.html

### `bean-validator:` — validation des données

```java
.to("bean-validator://orderRequestValidation")
```

Ce composant applique les annotations **Jakarta Bean Validation**
(`@NotBlank`, `@Min`, ...) portées par la classe du corps courant de l'Exchange
(ici `OrderRequest`, voir `dto/OrderRequest.java`). S'il trouve une violation, il lève
une `BeanValidationException`, interceptée par le bloc `onException` de la route.
C'est l'alternative "déclarative" à un `if` manuel dans un Processor.
Documentation : https://camel.apache.org/components/4.8.x/bean-validator-component.html

### `sql:` — écriture en base via JDBC

```java
.to("sql:INSERT INTO orders (id, customer_name, product, quantity, status, created_at)"
    + " VALUES (:#id, :#customerName, :#product, :#quantity, :#status, :#createdAt)"
    + "?dataSource=#dataSource")
```

Le composant `camel-sql` exécute la requête SQL fournie. Les paramètres nommés
`:#xxx` sont résolus en cherchant les clés correspondantes dans une **Map** portée par
le corps courant de l'Exchange — c'est pourquoi `OrderRoutes.toSqlParameters` convertit
explicitement le DTO `OrderResponse` en `Map<String, Object>` juste avant l'appel au
composant SQL (le DTO original est conservé à part, dans une **Exchange Property**
`orderResponse`, pour être réutilisé plus loin comme réponse HTTP). `#dataSource`
fait référence au bean Spring `DataSource` auto-configuré par
`spring.datasource.*` (application.yml) — Camel va chercher ce bean dans le contexte
Spring, qui joue le rôle de **registry** Camel.

Choix pédagogique de ce projet : l'**écriture** passe par ce composant SQL brut
(JDBC), tandis que la **lecture** (`GET /orders`) passe par Spring Data JPA
(`OrderRepository`). Cela permet de comparer concrètement les deux approches.
Documentation : https://camel.apache.org/components/4.8.x/sql-component.html

### `jackson` (data format) — transformation JSON

```java
.marshal(orderJson)                                  // objet Java -> JSON (avant Kafka), order-service
.unmarshal().json(JsonLibrary.Jackson, Map.class)    // JSON -> Map, stock-service
```

Dans `order-service`, le `JacksonDataFormat` (`orderJson`) est construit explicitement
dans le constructeur de `OrderRoutes` à partir du bean `ObjectMapper` partagé
(`config/CamelJacksonConfig.java`, qui enregistre le module `JavaTimeModule` pour
sérialiser proprement les `LocalDateTime`). C'est volontaire : la simple écriture
`.marshal().json(JsonLibrary.Jackson)` crée un `ObjectMapper` par défaut **sans**
chercher un bean existant dans le contexte Spring, et donc sans le module date/heure —
piège classique en apprenant Camel avec Spring Boot. Dans `stock-service`, comme on
désérialise vers une simple `Map` (pas de date typée), le raccourci
`unmarshal().json(...)` suffit.
Documentation : https://camel.apache.org/components/4.8.x/dataformats/json-jackson-dataformat.html

### `kafka:` — producteur et consommateur d'événements

Producteur (order-service, après le `marshal().json(...)`) :

```java
.setHeader(KafkaConstants.KEY, simple("${body.id}"))
.to("kafka:{{app.kafka.topic}}?brokers={{app.kafka.brokers}}")
```

Consommateur (stock-service) :

```java
from("kafka:{{app.kafka.topic}}?brokers={{app.kafka.brokers}}&groupId={{app.kafka.group-id}}")
```

`{{app.kafka.topic}}` et `{{app.kafka.brokers}}` sont des **property placeholders**
Camel, résolus automatiquement à partir des propriétés Spring Boot (`application.yml`,
ou variables d'environnement en Docker comme `APP_KAFKA_BROKERS`). La clé Kafka
(`KafkaConstants.KEY`) est définie comme l'id de la commande, ce qui garantit que
tous les événements d'une même commande finiraient sur la même partition.
Documentation : https://camel.apache.org/components/4.8.x/kafka-component.html

### `onException` — gestion centralisée des erreurs

```java
onException(BeanValidationException.class)
    .handled(true)
    ...
onException(Exception.class)
    .handled(true)
    ...
```

Équivalent simplifié d'un **Dead Letter Channel** : au lieu de mettre des
`try/catch` dans chaque étape, on déclare une fois pour toutes ce qui doit se passer
quand une exception précise (ou générique) est levée n'importe où dans la route.
`.handled(true)` signifie "l'erreur est absorbée ici" : l'appelant
(`OrderController`) ne reçoit pas d'exception Java, mais un Exchange avec un header
`OrderResult=VALIDATION_ERROR` (ou `ERROR`) et un corps décrivant l'erreur — à charge
du contrôleur de traduire ça en code HTTP 400/500.
Documentation : https://camel.apache.org/manual/exception-clause.html

## 3. Pourquoi passer par une route plutôt que du code Java classique ?

Tout ce que fait `OrderRoutes` pourrait être écrit en Java "normal" dans un service.
L'intérêt de Camel apparaît quand :

- on veut **standardiser** la façon de gérer les erreurs, les retries, le logging
  sur toutes les intégrations d'une entreprise (même vocabulaire partout) ;
- on veut pouvoir **changer un connecteur sans toucher à la logique** (remplacer
  Kafka par un composant JMS/RabbitMQ ne change qu'une ligne d'URI) ;
- on veut appliquer des **Enterprise Integration Patterns** (routage conditionnel,
  agrégation, split/scatter-gather, idempotence...) sans les réécrire à la main —
  voir [docs/03-exercices.md](03-exercices.md) pour aller plus loin sur ce point.
