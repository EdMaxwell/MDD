# Backend MDD

Ce dossier contient l'API REST du projet MDD. Le backend gere l'authentification, les utilisateurs, les themes, les abonnements, le feed d'articles, la creation d'articles, le detail d'article et les commentaires.

L'objectif de ce README est double :

- permettre de lancer le backend localement ;
- donner les points de code importants a presenter, notamment l'integration avec le frontend Angular.

## Stack technique

| Element | Choix |
|---------|-------|
| Langage | Java 21 |
| Framework | Spring Boot 4.0.5 |
| API HTTP | Spring WebMVC |
| Securite | Spring Security, JWT signe, refresh token opaque |
| Persistence | Spring Data JPA, Hibernate |
| Migrations | Liquibase SQL |
| Base locale | PostgreSQL 17 via Docker Compose |
| Tests | JUnit, Spring Boot Test, H2 en mode PostgreSQL |
| Tokens JWT | `io.jsonwebtoken:jjwt` |

## Lancer le backend

### 1. Prerequis

- Java 21.
- Docker Desktop ou Docker Engine.
- Maven installe, ou le wrapper Maven inclus dans `back/`.

### 2. Demarrer PostgreSQL

Depuis la racine du depot :

```powershell
docker compose up -d postgres
```

Le backend utilise par defaut les memes valeurs que `docker-compose.yml` :

| Variable | Valeur par defaut |
|----------|-------------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/mdd` |
| `DB_USERNAME` | `mdd` |
| `DB_PASSWORD` | `mdd_password` |

### 3. Demarrer Spring Boot

Depuis `back/` :

```powershell
.\mvnw.cmd spring-boot:run -Pdev
```

Equivalent avec Maven global :

```powershell
mvn spring-boot:run -Pdev
```

L'API demarre sur `http://localhost:8080/api`.

Le profil `dev` charge `src/main/resources/application-dev.yaml`, qui execute le seed `src/main/resources/db/seed/test-data.sql` apres les migrations Liquibase.

### 4. Verifier

```powershell
.\mvnw.cmd test
```

## Variables de configuration

Les valeurs suivantes peuvent etre surchargees par variables d'environnement :

| Variable | Role |
|----------|------|
| `DB_URL` | URL JDBC PostgreSQL |
| `DB_USERNAME` | Utilisateur de base de donnees |
| `DB_PASSWORD` | Mot de passe de base de donnees |
| `JWT_SECRET` | Secret HMAC Base64 utilise pour signer les access tokens |
| `JWT_EXPIRATION` | Duree de vie de l'access token en millisecondes |
| `JWT_REFRESH_EXPIRATION` | Duree de vie du refresh token en millisecondes |
| `JWT_REFRESH_COOKIE_NAME` | Nom du cookie HttpOnly |
| `JWT_REFRESH_COOKIE_SECURE` | Active l'attribut `Secure` du cookie |
| `JWT_REFRESH_COOKIE_SAME_SITE` | Valeur `SameSite` du cookie |
| `AUTH_DEBUG_LOG_LEVEL` | Niveau de log des classes auth/JWT |

En local HTTP, `JWT_REFRESH_COOKIE_SECURE=false` est attendu. En HTTPS, il faut le passer a `true`.

## Donnees de developpement

Le profil `dev` cree des themes, des articles et trois comptes :

| Email | Mot de passe |
|-------|--------------|
| `alice@mdd.local` | `password` |
| `bob@mdd.local` | `password` |
| `charlie@mdd.local` | `password` |

Les scripts utilisent `ON CONFLICT DO NOTHING`, ce qui permet de relancer le backend sans dupliquer les donnees deja presentes.

## Architecture du code

```text
src/main/java/com/mdd
|-- auth/       # inscription, login, refresh token, utilisateur principal
|-- common/     # enveloppes communes et gestion globale des erreurs
|-- config/     # beans techniques comme Clock et Liquibase
|-- post/       # articles, feed et commentaires
|-- security/   # JWT, filtre de securite, CORS, password encoder
|-- topic/      # themes et abonnements
`-- user/       # profil utilisateur
```

Le backend suit une architecture en couches :

- `controller` expose les routes REST.
- `service` porte les regles metier et les transactions.
- `repository` isole les requetes JPA.
- `domain` contient les entites JPA.
- `dto` definit les contrats d'entree/sortie de l'API.
- `exception` donne des erreurs metier dediees.

Les entites JPA ne sont pas retournees directement a l'API. Les controllers renvoient des DTO pour garder un contrat stable avec le frontend.

## Routes API principales

Toutes les routes metier sont prefixees par `/api`.

| Methode | Route | Role | Auth |
|---------|-------|------|------|
| `POST` | `/auth/register` | Inscrire un utilisateur et ouvrir une session | Non |
| `POST` | `/auth/login` | Se connecter avec email ou username | Non |
| `POST` | `/auth/refresh` | Remplacer un access token expire | Cookie refresh |
| `POST` | `/auth/logout` | Revoquer le refresh token courant | Cookie refresh |
| `GET` | `/auth/me` | Lire l'utilisateur authentifie | Oui |
| `GET` | `/users/me` | Lire le profil et les abonnements | Oui |
| `PUT` | `/users/me` | Modifier profil et mot de passe | Oui |
| `GET` | `/topics?page=&size=` | Lister les themes avec etat d'abonnement | Oui |
| `POST` | `/topics/{topicId}/subscription` | S'abonner a un theme | Oui |
| `DELETE` | `/topics/{topicId}/subscription` | Se desabonner d'un theme | Oui |
| `GET` | `/posts/feed?sort=&page=&size=` | Lire le feed des themes suivis | Oui |
| `POST` | `/posts` | Creer un article | Oui |
| `GET` | `/posts/{postId}` | Lire le detail d'un article | Oui |
| `POST` | `/posts/{postId}/comments` | Ajouter un commentaire | Oui |

## Integration avec le frontend

Le frontend Angular appelle `http://localhost:8080/api`, configure dans `front/src/environments/environment.ts`.

Les points d'integration essentiels sont :

- CORS : `SecurityConfig` autorise `http://localhost:4200` et `allowCredentials=true`.
- Access token : renvoye dans le JSON par `/auth/login`, `/auth/register` et `/auth/refresh`.
- Refresh token : pose par `AuthController` dans un cookie `HttpOnly`, chemin `/api/auth`.
- Requetes protegees : le frontend envoie `Authorization: Bearer <token>`.
- Refresh automatique : si l'access token expire, le frontend appelle `/auth/refresh` avec le cookie.
- Pagination : les endpoints `/topics` et `/posts/feed` renvoient `PageResponse<T>`, consomme par Angular.

### Flux de session

```text
Angular AuthService
  -> POST /api/auth/login
Backend AuthController
  -> AuthService verifie les credentials
  -> JwtService cree l'access token
  -> RefreshTokenService cree un refresh token stocke hashe
  -> AuthController pose le cookie HttpOnly
Angular AuthService
  -> stocke l'access token
  -> ajoute Authorization sur les requetes suivantes
```

Ce flux separe le token court lisible par le frontend et le refresh token long protege du JavaScript.

## Points cles du code a presenter

### Securite et session

- `security/SecurityConfig.java` : configure le mode stateless, les routes publiques, CORS, BCrypt et le filtre JWT.
- `security/JwtAuthenticationFilter.java` : lit l'en-tete Bearer, valide le JWT et installe l'utilisateur dans le contexte Spring Security.
- `security/JwtService.java` : signe et valide les access tokens avec un `Clock` injectable.
- `auth/controller/AuthController.java` : expose login/register/refresh/logout et deplace le refresh token dans un cookie HttpOnly.
- `auth/service/RefreshTokenService.java` : stocke uniquement le hash du refresh token, fait la rotation et detecte la reutilisation suspecte.

### Domaine metier

- `topic/service/TopicSubscriptionService.java` : liste les themes avec l'etat d'abonnement, puis gere abonnement/desabonnement de facon idempotente.
- `post/service/PostFeedService.java` : construit le feed a partir des themes suivis et applique pagination + tri.
- `post/repository/PostRepository.java` : contient la requete qui selectionne les articles des themes suivis, avec `@EntityGraph` pour charger auteur et theme.
- `post/service/PostService.java` : cree les articles, charge le detail et ajoute les commentaires. L'auteur et la date viennent toujours du backend.
- `user/service/UserProfileService.java` : lit et met a jour le profil avec validation d'unicite email.

### Contrats API et erreurs

- `common/PageResponse.java` : contrat public de pagination partage par feed et themes.
- `common/GlobalExceptionHandler.java` : convertit validation, 401/403, 404, 409 et erreurs inattendues en enveloppe stable.
- `post/dto/*`, `topic/dto/*`, `user/dto/*`, `auth/dto/*` : DTO utilises par le frontend.

### Base de donnees

- `src/main/resources/db/changelog/db.changelog-master.sql` : schema Liquibase PostgreSQL.
- Tables principales : `users`, `topics`, `user_topic_subscriptions`, `posts`, `comments`, `refresh_tokens`.
- Les contraintes d'unicite protegent emails, usernames, topics et doublons d'abonnement.

## Validation et securite

Le backend applique les validations importantes cote serveur :

- inscription : email valide, nom entre 2 et 100 caracteres, mot de passe fort ;
- profil : email valide, nom valide, mot de passe fort si fourni ;
- article : theme obligatoire, titre obligatoire et limite a 180 caracteres, contenu obligatoire ;
- commentaire : contenu obligatoire.

Les protections principales sont :

- hash BCrypt des mots de passe ;
- access token JWT signe et expire ;
- refresh token opaque stocke hashe en base ;
- rotation du refresh token a chaque refresh ;
- cookie `HttpOnly` pour eviter l'exposition JavaScript ;
- absence de `userId` dans les payloads de creation article/commentaire ;
- erreurs controlees via `GlobalExceptionHandler`.

## Tests

Les tests unitaires et de contexte sont dans `src/test/java/com/mdd`.

Ils couvrent notamment :

- inscription, login et refresh token ;
- creation/detail/commentaires d'articles ;
- feed pagine ;
- abonnements aux themes ;
- profil utilisateur.

Commande :

```powershell
.\mvnw.cmd test
```

## Depannage

| Probleme | Verification |
|----------|--------------|
| Le backend ne demarre pas | Verifier que PostgreSQL tourne avec `docker compose ps`. |
| Erreur de connexion BDD | Verifier `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. |
| Le frontend recoit une erreur CORS | Verifier que le frontend tourne sur `http://localhost:4200`. |
| Refresh impossible en local | Verifier `JWT_REFRESH_COOKIE_SECURE=false` en HTTP. |
| Feed vide | Verifier que l'utilisateur suit au moins un theme et que le profil `dev` a charge le seed. |
