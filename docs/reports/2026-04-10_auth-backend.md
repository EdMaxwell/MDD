# Rapport technique - Authentification backend JWT

Date : 2026-04-10  
Auteur : Codex

---

## 1. Contexte

Le backend du projet MDD ne contenait encore aucune brique d'authentification alors que le projet impose un backend Spring Boot sécurisé et organisé en couches.

Le besoin est d'ajouter :
- une inscription utilisateur ;
- une connexion utilisateur ;
- une sécurisation stateless des routes backend ;
- un socle réutilisable pour les futures routes protégées.

Le périmètre de cette intervention est limité à l'authentification backend et à sa documentation technique.

---

## 2. Problème technique

Le backend devait résoudre plusieurs points :
- identifier un utilisateur de manière fiable sur une API REST ;
- sécuriser les mots de passe et empêcher l'accès aux routes privées sans authentification ;
- rester cohérent avec une architecture `Controller / Service / Repository` ;
- préparer le projet à une intégration frontend Angular, avec un mécanisme simple à propager via interceptor.

Points de vigilance :
- ne pas stocker de mot de passe en clair ;
- ne pas garder d'état serveur inutile pour une API REST ;
- éviter une solution trop complexe pour un backend encore jeune ;
- prévoir une réponse d'erreur exploitable côté front.

---

## 3. Solutions envisagées

| Solution | Avantages | Inconvénients | Choix |
|----------|----------|--------------|------|
| Sessions serveur + cookie | Implémentation classique, révocation serveur simple | Moins naturel pour une API REST séparée, gestion d'état côté serveur, intégration front SPA plus contraignante | ❌ |
| JWT stateless avec Spring Security | Adapté à une SPA Angular, pas de session serveur, simple à transmettre via header `Authorization`, bonne base pour des routes REST protégées | Nécessite une bonne gestion du secret, révocation plus limitée sans mécanisme additionnel, vigilance sur la durée de vie du token | ✅ |

---

## 4. Solution retenue

La solution retenue est une authentification stateless basée sur JWT.

Concrètement :
- l'utilisateur s'inscrit via `POST /api/auth/register` ;
- le backend crée un utilisateur avec mot de passe chiffré en BCrypt ;
- l'utilisateur se connecte via `POST /api/auth/login` ;
- le backend renvoie un token JWT signé contenant l'identité de l'utilisateur ;
- les requêtes suivantes envoient ce token dans `Authorization: Bearer ...` ;
- un filtre Spring Security valide le token et alimente le contexte de sécurité ;
- la route `GET /api/auth/me` permet de récupérer l'utilisateur courant.

---

## 5. Justification du choix

Le choix du JWT est cohérent avec la nature REST du projet et avec un frontend Angular séparé. Une SPA transporte plus facilement un bearer token qu'une session fortement couplée au serveur. Cela simplifie aussi l'usage futur d'un interceptor Angular pour attacher automatiquement le token.

La session serveur aurait été viable, mais elle introduit une dépendance plus forte au stockage d'état côté backend et complique davantage la scalabilité horizontale. À ce stade du projet, le JWT offre un meilleur compromis entre simplicité d'intégration, lisibilité de l'architecture et alignement avec les usages modernes d'API REST.

Le choix a été volontairement gardé sobre :
- pas de refresh token pour éviter d'introduire prématurément un cycle plus complexe ;
- pas de rôles métier avancés tant que le besoin n'existe pas ;
- un seul type d'utilisateur applicatif pour limiter la complexité initiale.

---

## 6. Impact sur l'architecture

Fichiers et couches impactés :
- `Controller` : ajout du contrôleur d'authentification ;
- `Service` : ajout du service d'authentification et du chargement des utilisateurs ;
- `Repository` : ajout du repository JPA pour les utilisateurs ;
- `Domain` : ajout de l'entité `User` ;
- `Security` : ajout de la configuration Spring Security, du service JWT et du filtre d'authentification ;
- `Configuration` : enrichissement de `application.yaml` ;
- `Tests` : ajout de tests unitaires du service ;
- `Documentation` : création de ce rapport dans `docs/reports/`.

Modification structurelle principale :
- le backend passe d'un squelette vide à un premier socle applicatif sécurisé, prêt à protéger les futures ressources métier.

---

## 7. Bonnes pratiques appliquées

- Respect de l'architecture en couches `Controller / Service / Repository`.
- Encodage des mots de passe avec BCrypt.
- Validation des entrées via `jakarta.validation`.
- Sécurité stateless alignée avec une API REST.
- Centralisation des erreurs via un `RestControllerAdvice`.
- Externalisation des paramètres sensibles via variables d'environnement (`JWT_SECRET`, base de données).

---

## 8. Sécurité

Risques principaux :
- fuite ou faiblesse de la clé de signature JWT ;
- vol de token côté client ;
- tentative d'accès à des routes protégées sans authentification ;
- réutilisation d'un email déjà inscrit.

Protections mises en place :
- mot de passe encodé en BCrypt ;
- signature HMAC du JWT ;
- expiration des tokens configurable ;
- filtre JWT ne donnant accès aux routes privées qu'après validation ;
- contraintes d'unicité sur l'email ;
- réponses d'erreur explicites pour validation, conflit et authentification.

---

## 9. Points d'amélioration

- Ajouter un mécanisme de refresh token si le front en a besoin.
- Prévoir une stratégie de révocation ou blacklist si la sécurité métier l'exige.
- Ajouter des rôles et permissions plus fins si plusieurs profils apparaissent.
- Compléter par des tests d'intégration HTTP et de sécurité.
- Remplacer la valeur par défaut de `JWT_SECRET` par une vraie clé d'environnement dans tous les environnements non locaux.

---

## 10. Sources

- OWASP, *JSON Web Token for Java Cheat Sheet*. Intérêt : synthèse des risques concrets liés au JWT et des protections recommandées, utile pour cadrer la durée de vie du token, le secret de signature et les précautions d'usage. [https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- Auth0, *Cookies vs Tokens: Defining the Difference*. Intérêt : comparaison argumentée entre approche session/cookie et approche token pour des architectures web/API, utilisée ici pour justifier le choix d'une API stateless. [https://auth0.com/blog/cookies-vs-tokens-definitive-guide/](https://auth0.com/blog/cookies-vs-tokens-definitive-guide/)
- Toptal Engineering, *Spring Security Tutorial*. Intérêt : retour d'expérience explicatif sur l'intégration de JWT avec Spring Security dans une API REST, utile pour confronter l'implémentation pratique au besoin du projet. [https://www.toptal.com/spring/spring-security-tutorial](https://www.toptal.com/spring/spring-security-tutorial)
