# Ticket API — Spring Boot 4 + Keycloak

POC d'une API REST sécurisée par Keycloak avec trois rôles : **CLIENT**, **AGENT** et **ADMIN**.

---

## Qu'est-ce que Keycloak ?

**Keycloak** est une solution open-source d'**IAM** (Identity and Access Management) développée par Red Hat.
Elle centralise tout ce qui touche à l'identité : authentification, autorisation, gestion des utilisateurs et des sessions.

### Problème qu'il résout

Sans Keycloak, chaque application doit gérer elle-même :
- Le stockage sécurisé des mots de passe
- La gestion des sessions
- La fédération d'identités (LDAP, Google, GitHub…)
- Le Single Sign-On (SSO)

Keycloak externalise tout cela. Les applications ne voient plus que des **tokens JWT**.

### Les protocoles supportés

| Protocole | Rôle |
|-----------|------|
| **OAuth 2.0** | Délégation d'autorisation (qui a le droit de faire quoi) |
| **OpenID Connect (OIDC)** | Couche d'identité au-dessus d'OAuth 2.0 (qui est l'utilisateur) |
| **SAML 2.0** | Alternative entreprise à OIDC |

### Concepts fondamentaux

```
┌─────────────────────────────────────────────────────────────┐
│                         KEYCLOAK                            │
│                                                             │
│  ┌──────────┐   contient   ┌──────────┐   ont des          │
│  │  Realm   │ ──────────▶  │  Clients │ ──────────▶ Rôles  │
│  └──────────┘              └──────────┘                    │
│       │                                                     │
│       │ contient                                            │
│       ▼                                                     │
│  ┌──────────┐   ont des    ┌──────────────┐                 │
│  │  Users   │ ──────────▶  │ Realm Roles  │                 │
│  └──────────┘              └──────────────┘                 │
└─────────────────────────────────────────────────────────────┘
```

| Concept | Description |
|---------|-------------|
| **Realm** | Espace d'isolation (comme un tenant). Un realm = un domaine d'authentification. |
| **Client** | L'application qui délègue l'authentification à Keycloak (`ticket-app` ici). |
| **Realm Role** | Rôle défini au niveau du realm, assigné aux utilisateurs (`CLIENT`, `AGENT`, `ADMIN`). |
| **Client Role** | Rôle défini au niveau du client (application). Plus fin que les realm roles. |
| **JWT** | JSON Web Token — jeton signé que Keycloak émet après authentification. |

---

## Le flux d'authentification (Resource Owner Password Grant)

> Ce flux (Direct Access Grants) est simplifié pour un POC/apprentissage.  
> En production, préférer le **Authorization Code Flow** (avec PKCE).

```
┌──────────┐    1. POST /token          ┌───────────┐
│  Client  │ ──────────────────────────▶│ Keycloak  │
│ (curl /  │   username + password       │  :8081    │
│  .http)  │◀────────────────────────── │           │
└──────────┘    2. access_token (JWT)   └───────────┘
     │
     │  3. GET /api/tickets
     │     Authorization: Bearer <JWT>
     ▼
┌──────────────────┐   4. Valide la signature    ┌───────────┐
│   Spring Boot    │ ──────────────────────────▶ │ Keycloak  │
│     :8080        │   (JWK Set, clé publique)    │  JWKS URI │
│                  │◀────────────────────────────│           │
└──────────────────┘   5. Token valide → traite   └───────────┘
                           la requête
```

### Anatomie d'un JWT Keycloak (décodé)

```json
{
  "exp": 1718272800,
  "iss": "http://localhost:8081/realms/ticket-realm",
  "sub": "uuid-de-l-utilisateur",
  "preferred_username": "agent-user",
  "email": "agent@example.com",
  "realm_access": {
    "roles": [
      "AGENT",
      "offline_access",
      "uma_authorization"
    ]
  },
  "resource_access": {
    "ticket-app": {
      "roles": []
    }
  }
}
```

> **Point clé** : Spring Security cherche les rôles dans le claim `scope` par défaut.  
> Keycloak les place dans `realm_access.roles`. C'est pourquoi on a écrit `KeycloakJwtConverter`.

---

## Objectif du projet

1. Apprendre à protéger une API REST avec Keycloak en tant que **Resource Server** OAuth2.
2. Pratiquer la gestion des **rôles** (RBAC) avec `@PreAuthorize`.
3. Appliquer les **bonnes pratiques Java 25 + Spring Boot 4.x** (records, virtual threads, ProblemDetail…).
4. Écrire des **tests de sécurité** sans serveur Keycloak réel.

---

## Architecture du projet

```
src/
├── main/java/com/poc/keycloak/
│   ├── config/
│   │   ├── SecurityConfig.java          # Filtre JWT + @EnableMethodSecurity
│   │   └── KeycloakJwtConverter.java    # Extrait realm_access.roles → ROLE_XXX
│   ├── controller/
│   │   └── TicketController.java        # @PreAuthorize par méthode
│   ├── service/
│   │   ├── TicketService.java           # Interface
│   │   └── TicketServiceImpl.java       # @Transactional(readOnly = true)
│   ├── repository/
│   │   └── TicketRepository.java        # JpaRepository<Ticket, Long>
│   ├── entity/
│   │   └── Ticket.java                  # Entité JPA (Lombok précis, pas @Data)
│   ├── dto/
│   │   ├── TicketRequest.java           # record Java 25 + Bean Validation
│   │   └── TicketResponse.java          # record Java 25 (immuable)
│   └── exception/
│       ├── TicketNotFoundException.java  # RuntimeException métier
│       └── GlobalExceptionHandler.java   # ProblemDetail RFC 7807
│
├── resources/
│   └── application.yaml                 # issuer-uri, virtual threads, H2
│
keycloak/
│   └── realm-export.json               # Importé automatiquement au démarrage
│
http/
│   └── tickets.http                    # Requêtes de test (VS Code / IntelliJ)
│
docker-compose.yml                      # Keycloak 26
```

---

## Matrice des droits (RBAC)

| Endpoint | Non authentifié | CLIENT | AGENT | ADMIN |
|----------|:--------------:|:------:|:-----:|:-----:|
| `GET /api/tickets` | **401** | ✅ 200 | ✅ 200 | ✅ 200 |
| `GET /api/tickets/{id}` | **401** | ✅ 200 | ✅ 200 | ✅ 200 |
| `POST /api/tickets` | **401** | **403** | ✅ 201 | ✅ 201 |
| `PUT /api/tickets/{id}` | **401** | **403** | ✅ 200 | ✅ 200 |
| `DELETE /api/tickets/{id}` | **401** | **403** | **403** | ✅ 204 |

---

## Ce qu'on a fait dans ce projet

### 1. Entité et couche de données (bonnes pratiques JPA)

- Remplacement de `@Data` (Lombok) par `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(of = "id")` — `@Data` génère un `hashCode` basé sur tous les champs ce qui cause des bugs avec les collections JPA.
- Annotations `@Column(nullable = false)` + Bean Validation (`@NotBlank`, `@Size`) pour une validation en double couche (API + BDD).

### 2. Records Java 25 pour les DTOs

```java
// Avant (classe Lombok)
@Data
public class TicketRequest {
    private String title;
    private String description;
}

// Après (record Java 25 — immuable par nature)
public record TicketRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String description
) {}
```

Les records sont **immuables**, **compacts**, et éliminent tout le boilerplate. Ils sont idéaux pour les DTOs d'entrée/sortie d'une API.

### 3. Service avec transactions explicites

```java
@Transactional(readOnly = true)  // par défaut sur toute la classe
public class TicketServiceImpl implements TicketService {

    @Transactional  // override pour les écritures
    public TicketResponse create(TicketRequest request) { ... }
}
```

`readOnly = true` optimise les lectures (pas de dirty checking, flush désactivé, possibilité de routing vers un replica).

### 4. Gestion d'erreurs avec ProblemDetail (RFC 7807)

Spring Boot 3+ supporte nativement ce standard. Les erreurs sont désormais structurées :

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Ticket not found with id: 99"
}
```

### 5. Virtual Threads (Project Loom — Java 21+)

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Active les threads virtuels pour Tomcat et les tâches `@Async`. Un thread virtuel consomme ~quelques Ko contre ~1 Mo pour un thread OS classique — idéal pour les I/O-bound workloads (appels HTTP, BDD).

### 6. KeycloakJwtConverter — extraction des rôles

```java
// Keycloak place les rôles ici dans le JWT :
// { "realm_access": { "roles": ["AGENT", "offline_access"] } }

private Set<GrantedAuthority> extractRealmRoles(Jwt jwt) {
    var realmAccess = (Map<String, Object>) jwt.getClaim("realm_access");
    var roles = (List<String>) realmAccess.get("roles");
    return roles.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
        .collect(Collectors.toUnmodifiableSet());
}
```

### 7. SecurityConfig — Resource Server stateless

```java
@EnableMethodSecurity  // nécessaire pour que @PreAuthorize fonctionne
public class SecurityConfig {
    http
        .csrf(disable)                  // pas de session → pas de CSRF
        .sessionManagement(STATELESS)   // chaque requête porte son JWT
        .oauth2ResourceServer(jwt → KeycloakJwtConverter)
}
```

### 8. @PreAuthorize par méthode

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")      // seul ADMIN peut supprimer
public void delete(@PathVariable Long id) { ... }
```

Plus flexible que les règles URL dans `SecurityFilterChain` : la logique d'accès est visible directement sur la méthode concernée.

### 9. Tests de sécurité sans Keycloak

```java
// Simule un JWT avec le rôle AGENT — aucun serveur Keycloak requis
mockMvc.perform(post("/api/tickets")
    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
    .andExpect(status().isCreated());

// @MockitoBean JwtDecoder → empêche Spring de contacter Keycloak au démarrage
@MockitoBean
JwtDecoder jwtDecoder;
```

---

## Étapes pour démarrer

### Prérequis

- Java 25
- Maven 3.9+
- Docker + Docker Compose

### 1. Démarrer Keycloak

```bash
docker-compose up -d
```

Keycloak démarre sur `http://localhost:8081` et importe automatiquement `keycloak/realm-export.json` (realm `ticket-realm`, 3 utilisateurs, client `ticket-app`).

Attendre ~30 secondes que Keycloak soit prêt, puis vérifier :

```bash
curl http://localhost:8081/realms/ticket-realm
```

### 2. Démarrer l'application

```bash
./mvnw spring-boot:run
```

L'API est disponible sur `http://localhost:8080`.

### 3. Obtenir un token JWT

```bash
# Token pour agent-user
curl -s -X POST http://localhost:8081/realms/ticket-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=ticket-app&username=agent-user&password=password" \
  | python -m json.tool
```

Copier la valeur du champ `access_token`.

### 4. Appeler l'API avec le token

```bash
TOKEN="<access_token>"

# Liste des tickets (CLIENT, AGENT, ADMIN)
curl http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $TOKEN"

# Créer un ticket (AGENT, ADMIN)
curl -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Problème réseau","description":"Coupure intermittente"}'

# Supprimer (ADMIN uniquement)
curl -X DELETE http://localhost:8080/api/tickets/1 \
  -H "Authorization: Bearer $TOKEN"
```

> Vous pouvez aussi utiliser le fichier `http/tickets.http` avec VS Code REST Client ou IntelliJ.

### 5. Console Keycloak (admin)

```
URL      : http://localhost:8081
Login    : admin
Password : admin
```

Depuis la console, vous pouvez inspecter le realm, décoder les tokens, modifier les rôles des utilisateurs en temps réel.

### 6. Console H2 (base de données)

```
URL  : http://localhost:8080/h2-console
JDBC : jdbc:h2:mem:keycloak_db
User : sa
```

### 7. Lancer les tests

```bash
./mvnw test
```

Les tests ne nécessitent **pas** de Keycloak en fonctionnement.

---

## Décoder un token JWT

Un JWT est en Base64 — vous pouvez le décoder en ligne sur [jwt.io](https://jwt.io) ou en local :

```bash
# Extraire et décoder le payload (la 2ème partie du token)
echo "<access_token>" | cut -d'.' -f2 | base64 -d 2>/dev/null | python -m json.tool
```

Vous verrez les claims Keycloak, notamment `realm_access.roles`.

---

## Dépendances principales

| Dépendance | Rôle |
|-----------|------|
| `spring-boot-starter-web` | API REST (Spring MVC) |
| `spring-boot-starter-data-jpa` | ORM Hibernate + Spring Data |
| `spring-boot-starter-validation` | Bean Validation (`@NotBlank`, `@Valid`) |
| `spring-boot-starter-security` | Filtre de sécurité Spring |
| `spring-boot-starter-oauth2-resource-server` | Validation des JWT Keycloak |
| `spring-security-test` | `jwt()` post-processor pour les tests |
| `h2` | Base de données en mémoire (dev) |
| `lombok` | Réduction du boilerplate sur les entités |
