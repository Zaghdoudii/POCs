# Pour aller plus loin

Ce projet couvre les bases (route, validation, JDBC, JSON, Kafka, gestion d'erreurs).
Une fois à l'aise, voici des exercices progressifs pour explorer d'autres aspects
d'Apache Camel, du plus simple au plus avancé. Chacun peut se faire indépendamment.

## 1. Content-Based Router — router selon la quantité

Dans `OrderRoutes`, utilisez un `.choice() / .when() / .otherwise()` pour publier
les grosses commandes (`quantity > 100`) sur un topic Kafka différent
(`order-events-large`) plutôt que `order-events`. C'est l'un des Enterprise
Integration Patterns les plus utilisés (routage conditionnel).
→ https://camel.apache.org/components/4.8.x/eips/choice-eip.html

## 2. Idempotent Consumer — éviter les doublons côté stock-service

Si Kafka redistribue un message déjà traité (redémarrage, rebalance de consumer
group...), `stock-service` décrémentera le stock une deuxième fois. Ajoutez un
`.idempotentConsumer(header("orderId"), MemoryIdempotentRepository.memoryIdempotentRepository())`
avant le traitement pour ignorer les messages déjà vus.
→ https://camel.apache.org/components/4.8.x/eips/idempotentConsumer-eip.html

## 3. Redelivery / retry — résilience face aux pannes momentanées

Configurez un `errorHandler` avec redélivrance automatique en cas d'échec de
l'insertion SQL (par exemple si Postgres est temporairement indisponible) :

```java
errorHandler(defaultErrorHandler()
    .maximumRedeliveries(3)
    .redeliveryDelay(1000)
    .retryAttemptedLogLevel(LoggingLevel.WARN));
```

→ https://camel.apache.org/manual/error-handler.html

## 4. REST DSL — remplacer le contrôleur Spring par du Camel pur

Remplacez `OrderController` par la **Rest DSL** de Camel
(`camel-rest-starter` + `camel-platform-http-starter`), qui permet de définir
l'API REST directement dans une route Camel (`rest("/orders").post()...`).
Comparez la lisibilité et les compromis avec l'approche actuelle
(contrôleur Spring + `ProducerTemplate`).
→ https://camel.apache.org/components/4.8.x/rest-component.html

## 5. Tests automatisés avec camel-test-spring-junit5

La dépendance `camel-test-spring-junit5` est déjà présente dans `order-service`.
Écrivez un test qui :
- démarre le contexte Camel/Spring ;
- utilise `AdviceWith` pour remplacer l'endpoint `kafka:...` par un `mock:kafka`
  (pour ne pas dépendre d'un vrai broker Kafka dans les tests) ;
- envoie un `OrderRequest` via `ProducerTemplate` et vérifie le résultat.
→ https://camel.apache.org/manual/advicewith.html

## 6. Observabilité — métriques et monitoring des routes

Ajoutez `camel-micrometer-starter` + `spring-boot-starter-actuator` pour exposer
les métriques par route (nombre de messages, temps de traitement, erreurs) via
`/actuator/metrics`. Pour une observation "live" des routes (graphe, débit,
inspection des messages en transit), essayez **Hawtio**
(`hawtio-springboot` + `actuator`).
→ https://camel.apache.org/components/4.8.x/others/micrometer.html

## 7. YAML DSL — comparer avec le Java DSL

Réécrivez `OrderRoutes` (ou une version simplifiée) en **YAML DSL**
(`camel-yaml-dsl-starter`, fichier `routes.camel.yaml`) pour comparer avec la
version Java actuelle. Utile pour comprendre les cas où Camel est piloté par
configuration plutôt que par code compilé (ex : Camel K / Kubernetes).
→ https://camel.apache.org/manual/camel-yaml-dsl.html

## 8. Aggregator — regrouper plusieurs commandes

Simulez un scénario de facturation groupée : utilisez l'EIP **Aggregator** pour
regrouper toutes les commandes d'un même client reçues sur une fenêtre de temps
avant de publier un seul événement récapitulatif sur Kafka.
→ https://camel.apache.org/components/4.8.x/eips/aggregate-eip.html
