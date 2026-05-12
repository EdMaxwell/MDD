# Frontend MDD

Ce dossier contient l'application Angular du projet MDD. Elle fournit les ecrans d'authentification, le feed d'articles, les themes, la creation et le detail d'articles, les commentaires et le profil utilisateur.

L'objectif de ce README est double :

- permettre de lancer le frontend localement ;
- expliquer les points de code importants, en particulier l'integration avec le backend Spring Boot.

## Stack technique

| Element | Choix |
|---------|-------|
| Framework | Angular 21 |
| Langage | TypeScript |
| Composants | Standalone components |
| Etat UI | Signals Angular |
| Formulaires | Reactive Forms |
| HTTP | `HttpClient` + RxJS |
| UI | PrimeNG, PrimeIcons, theme Aura |
| Styles | SCSS |
| Rendu | SSR Angular avec hydration |
| Tests | Angular unit test builder, Vitest |

## Lancer le frontend

### 1. Prerequis

- Node.js et npm compatibles avec Angular 21.
- Backend Spring Boot demarre sur `http://localhost:8080`.

### 2. Installer les dependances

Depuis `front/` :

```powershell
npm install
```

### 3. Demarrer le serveur Angular

```powershell
npm start
```

L'application est disponible sur `http://localhost:4200`.

### 4. Construire l'application

```powershell
npm run build
```

La sortie est generee dans `dist/mdd-front`.

### 5. Lancer la version SSR construite

Apres un build :

```powershell
npm run serve:ssr:mdd-front
```

### 6. Lancer les tests

```powershell
npm test
```

## Configuration d'environnement

Les fichiers d'environnement sont :

- `src/environments/environment.ts`
- `src/environments/environment.prod.ts`

La valeur importante pour l'integration backend est :

```ts
apiUrl: 'http://localhost:8080/api'
```

`authDebugEnabled` active des logs de diagnostic dans `AuthService` en environnement de developpement.

## Architecture du code

```text
src/app
|-- core/auth/                # session, tokens, helpers HTTP authentifies
|-- features/auth/            # landing, login, inscription
|-- features/home/            # feed d'articles
|-- features/articles/        # cartes, creation, detail, commentaires
|-- features/topics/          # catalogue de themes et abonnements
|-- features/profile/         # profil et desabonnements
|-- shared/pagination/        # contrat de pagination partage
`-- shared/ui/                # topbar, boutons, logo, grille paginee
```

L'application utilise des composants standalone. Les pages de feature orchestrent les appels API et les etats de chargement, tandis que les composants partages gerent l'affichage reutilisable.

## Routes Angular

Les routes sont declarees dans `src/app/app.routes.ts`.

| Route | Composant | Role |
|-------|-----------|------|
| `/` | `AuthPageComponent` | Page d'accueil non connectee |
| `/login` | `AuthPageComponent` | Connexion |
| `/register` | `AuthPageComponent` | Inscription |
| `/home` | `HomePageComponent` | Feed des articles des themes suivis |
| `/articles/new` | `ArticleCreatePageComponent` | Creation d'article |
| `/articles/:id` | `ArticleDetailPageComponent` | Detail et commentaires |
| `/topics` | `TopicsPageComponent` | Liste des themes et abonnement |
| `/profile` | `ProfilePageComponent` | Profil et abonnements |

`src/app/app.routes.server.ts` configure le rendu SSR. La route dynamique `/articles/:id` est rendue cote serveur a la demande, car les identifiants d'articles ne sont pas connus au build.

## Integration avec le backend

Le frontend parle avec l'API Spring Boot via les services Angular.

### Session et securite

`src/app/core/auth/auth.service.ts` centralise la session :

- `login()` appelle `POST /api/auth/login`.
- `register()` appelle `POST /api/auth/register`.
- `logout()` appelle `POST /api/auth/logout`.
- `init()` restaure la session au demarrage.
- `requestWithRefresh()` relance une requete apres refresh si le backend retourne `401` ou `403`.

Le backend renvoie un access token JWT dans le JSON. Le frontend le stocke dans `localStorage` sous la cle `mdd.token`.

Le refresh token n'est pas lu par Angular. Il est stocke par le navigateur dans un cookie `HttpOnly` pose par le backend. Les appels qui doivent envoyer ce cookie utilisent `withCredentials: true`.

### Appels metier

| Service frontend | Methode | Endpoint backend |
|------------------|---------|------------------|
| `AuthService` | `login` | `POST /api/auth/login` |
| `AuthService` | `register` | `POST /api/auth/register` |
| `AuthService` | `loadProfile` | `GET /api/users/me` |
| `AuthService` | `updateProfile` | `PUT /api/users/me` |
| `ArticleFeedService` | `loadFeed` | `GET /api/posts/feed?sort=&page=&size=` |
| `ArticleFeedService` | `createArticle` | `POST /api/posts` |
| `ArticleFeedService` | `loadArticle` | `GET /api/posts/{id}` |
| `ArticleFeedService` | `addComment` | `POST /api/posts/{id}/comments` |
| `TopicSubscriptionService` | `loadTopics` | `GET /api/topics?page=&size=` |
| `TopicSubscriptionService` | `subscribe` | `POST /api/topics/{id}/subscription` |
| `TopicSubscriptionService` | `unsubscribe` | `DELETE /api/topics/{id}/subscription` |

### Pagination

Le backend renvoie un objet pagine. Le frontend le represente avec `PageResponse<T>` dans `src/app/shared/pagination/page-response.ts`.

`normalizePageResponse()` accepte aussi une ancienne reponse en tableau simple. Cela garde l'interface robuste pendant les transitions de backend local.

## Points cles du code a presenter

### Authentification

- `core/auth/auth.service.ts` : point central de l'integration. Il gere `localStorage`, `withCredentials`, l'en-tete Bearer, la restauration de session et le refresh automatique.
- `features/auth/auth-page.component.ts` : une seule page gere landing, login et register selon les donnees de route. Les validateurs changent selon l'ecran.

### Feed et articles

- `features/home/home-page.component.ts` : charge le feed, gere tri asc/desc, pagination et conservation de l'etat dans les query params.
- `features/articles/services/article-feed.service.ts` : contrat TypeScript des articles et appels vers `/posts`.
- `features/articles/pages/article-create/article-create-page.component.ts` : charge les themes puis cree un article avec `topicId`, `title`, `content`.
- `features/articles/pages/article-detail/article-detail-page.component.ts` : lit le detail, affiche les commentaires et ajoute un commentaire localement apres succes API.
- `features/articles/components/article-card/` : composant de presentation reutilise par le feed.

### Themes et profil

- `features/topics/topic-subscription.service.ts` : appels au catalogue et aux abonnements.
- `features/topics/components/topics-page/` : pagination des themes et etat des boutons pendant les appels.
- `features/profile/profile-page.component.ts` : edition du profil, changement optionnel de mot de passe et desabonnement depuis le profil.

### UI partagee

- `shared/ui/topbar/` : navigation principale et etat connecte.
- `shared/ui/ui-button/` : bouton coherent dans l'application.
- `shared/ui/paginated-card-grid/` : grille responsive avec pagination PrimeNG.
- `shared/pagination/page-response.ts` : contrat commun avec le backend.

## Parcours utilisateur couverts

1. Accueil non connecte.
2. Inscription avec mot de passe fort.
3. Connexion avec email ou username.
4. Restauration de session au rechargement.
5. Consultation du feed selon les themes suivis.
6. Tri du feed par date recente ou ancienne.
7. Creation d'un article.
8. Lecture du detail d'article.
9. Ajout d'un commentaire.
10. Consultation des themes et abonnement.
11. Consultation et modification du profil.
12. Desabonnement depuis le profil.
13. Deconnexion.

## Comportements importants

- Les pages protegees appellent `authService.init()` puis redirigent vers `/` si aucune session n'est restauree.
- Les erreurs reseau affichent un message indiquant de verifier le backend sur le port `8080`.
- Le retour depuis un detail d'article restaure la page, la taille de page et le tri du feed.
- Les boutons d'abonnement/desabonnement evitent les doubles clics avec des ensembles d'identifiants en cours de mise a jour.
- Les formulaires ne remplacent pas la validation backend : ils ameliorent seulement l'experience utilisateur.

## Comptes de developpement

Si le backend tourne avec le profil `dev`, les comptes suivants sont disponibles :

| Email | Mot de passe |
|-------|--------------|
| `alice@mdd.local` | `password` |
| `bob@mdd.local` | `password` |
| `charlie@mdd.local` | `password` |

## Depannage

| Probleme | Verification |
|----------|--------------|
| Page blanche ou erreurs de bundles | Relancer `npm install`, puis `npm start`. |
| Message "backend inaccessible" | Verifier que Spring Boot tourne sur `http://localhost:8080`. |
| Session non restauree | Verifier que le backend pose bien le cookie refresh et que l'appel utilise `withCredentials`. |
| Erreur CORS | Verifier que l'URL frontend est `http://localhost:4200`. |
| Feed vide | Verifier que l'utilisateur est abonne a au moins un theme. |
