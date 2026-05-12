# MDD

MDD est une application web de reseau social pour developpeurs. Le MVP permet a un utilisateur de s'inscrire, se connecter, suivre des themes, consulter un fil d'articles, publier un article, lire le detail d'un article, commenter et gerer son profil.

Le depot contient deux applications :

- `back/` : API REST Spring Boot securisee par JWT.
- `front/` : application Angular standalone avec SSR.

La documentation detaillee de chaque partie est disponible ici :

- Backend : [back/README.md](back/README.md)
- Frontend : [front/README.md](front/README.md)

## Stack globale

| Couche | Technologies |
|--------|--------------|
| Frontend | Angular 21, TypeScript, RxJS, PrimeNG, SCSS, SSR |
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Liquibase |
| Base de donnees | PostgreSQL 17 via Docker Compose |
| Authentification | Access token JWT + refresh token opaque en cookie HttpOnly |
| Tests | Maven/JUnit cote backend, Angular/Vitest cote frontend |

## Structure du projet

```text
.
|-- back/                  # API REST Spring Boot
|-- front/                 # Application Angular
|-- docs/                  # Specifications, schema et aide locale
|-- docker-compose.yml     # PostgreSQL et pgAdmin
|-- .env.example           # Variables locales de reference
`-- README.md              # Vue globale du projet
```

## Lancer le projet en local

### 1. Prerequis

- Java 21.
- Node.js et npm compatibles avec Angular 21.
- Docker Desktop ou Docker Engine.
- Maven installe globalement, ou les wrappers Maven fournis dans `back/`.

### 2. Preparer l'environnement

Depuis la racine du projet :

```powershell
Copy-Item .env.example .env
docker compose up -d postgres
```

Optionnel : demarrer aussi pgAdmin.

```powershell
docker compose up -d postgres pgadmin
```

PostgreSQL ecoute par defaut sur `localhost:5432`, avec :

- base : `mdd`
- utilisateur : `mdd`
- mot de passe : `mdd_password`

### 3. Lancer le backend

Depuis `back/` :

```powershell
.\mvnw.cmd spring-boot:run -Pdev
```

Equivalent si Maven est installe :

```powershell
mvn spring-boot:run -Pdev
```

L'API est disponible sur `http://localhost:8080/api`.

Le profil `dev` applique le schema Liquibase puis charge le seed de developpement.

### 4. Lancer le frontend

Dans un second terminal, depuis `front/` :

```powershell
npm install
npm start
```

L'application est disponible sur `http://localhost:4200`.

### 5. Comptes de developpement

Avec le profil backend `dev`, les comptes suivants sont charges :

| Email | Mot de passe |
|-------|--------------|
| `alice@mdd.local` | `password` |
| `bob@mdd.local` | `password` |
| `charlie@mdd.local` | `password` |

## Integration front-end / back-end

Le frontend appelle l'API via `front/src/environments/environment.ts` :

```ts
apiUrl: 'http://localhost:8080/api'
```

Le backend expose toutes les routes metier sous `/api` et autorise l'origine Angular locale dans `SecurityConfig` :

```text
http://localhost:4200
```

Le flux d'authentification est le point central de l'integration :

1. Le frontend envoie `POST /api/auth/login` ou `POST /api/auth/register`.
2. Le backend renvoie un access token JWT dans le JSON et pose un refresh token dans un cookie `HttpOnly`.
3. Le frontend stocke l'access token dans `localStorage` et l'envoie dans l'en-tete `Authorization: Bearer ...`.
4. Le backend valide le JWT avec `JwtAuthenticationFilter`.
5. Si une requete protegee retourne `401` ou `403`, `AuthService` tente `POST /api/auth/refresh` avec `withCredentials`.
6. Le backend fait une rotation du refresh token, renvoie un nouvel access token et remplace le cookie.

Les listes paginees utilisent le meme contrat JSON entre back et front :

```json
{
  "content": [],
  "page": 0,
  "size": 6,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## Points cles du code a presenter

- Authentification : `back/src/main/java/com/mdd/auth/controller/AuthController.java`, `back/src/main/java/com/mdd/auth/service/AuthService.java`, `back/src/main/java/com/mdd/auth/service/RefreshTokenService.java`.
- Securite HTTP : `back/src/main/java/com/mdd/security/SecurityConfig.java`, `back/src/main/java/com/mdd/security/JwtAuthenticationFilter.java`, `back/src/main/java/com/mdd/security/JwtService.java`.
- Feed d'articles : `back/src/main/java/com/mdd/post/service/PostFeedService.java` et `back/src/main/java/com/mdd/post/repository/PostRepository.java`.
- Creation/detail/commentaires : `back/src/main/java/com/mdd/post/service/PostService.java`.
- Contrat de pagination : `back/src/main/java/com/mdd/common/PageResponse.java` et `front/src/app/shared/pagination/page-response.ts`.
- Session frontend : `front/src/app/core/auth/auth.service.ts`.
- Routes et pages Angular : `front/src/app/app.routes.ts`, `front/src/app/features/home/home-page.component.ts`, `front/src/app/features/articles/`.
- Themes et abonnements : `back/src/main/java/com/mdd/topic/service/TopicSubscriptionService.java` et `front/src/app/features/topics/`.

## Commandes utiles

Backend :

```powershell
cd back
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run -Pdev
```

Frontend :

```powershell
cd front
npm run build
npm test
npm start
```

Docker :

```powershell
docker compose up -d postgres
docker compose ps
docker compose down
```

## URLs locales

| Service | URL |
|---------|-----|
| Frontend Angular | `http://localhost:4200` |
| API backend | `http://localhost:8080/api` |
| PostgreSQL | `localhost:5432` |
| pgAdmin optionnel | `http://localhost:5050` |

## Documentation projet

- Specifications MVP : [docs/specifications_mdd.md](docs/specifications_mdd.md)
- Schema BDD et consignes JWT : [docs/schema_bdd_mdd_consignes_jwt.md](docs/schema_bdd_mdd_consignes_jwt.md)
- Developpement local : [docs/developpement-local.md](docs/developpement-local.md)
- Decisions techniques : [docs/technical_decisions_guidelines_projet_mdd.md](docs/technical_decisions_guidelines_projet_mdd.md)
