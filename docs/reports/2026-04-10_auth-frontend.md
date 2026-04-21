# Rapport technique - Authentification frontend

Date : 2026-04-10  
Auteur : Codex

---

## 1. Contexte

Le projet MDD avait besoin d'une interface frontend Angular pour :
- permettre l'inscription et la connexion des utilisateurs ;
- consommer les endpoints d'authentification exposes par le backend Spring Boot ;
- proposer une navigation simple entre l'accueil public, les formulaires d'authentification et une page utilisateur connecte.

Le perimetre couvert ici concerne exclusivement le frontend Angular de l'authentification.

---

## 2. Probleme technique

Il fallait resoudre plusieurs points :
- connecter proprement le front Angular au backend JWT ;
- eviter un composant monolithique difficile a faire evoluer ;
- rendre les ecrans reutilisables et coherents avec la maquette ;
- gerer l'etat de session sans dupliquer la logique entre les pages ;
- conserver un rendu responsive correct sur mobile et desktop.

Les points de vigilance principaux etaient :
- la validation des formulaires ;
- la redirection apres connexion ;
- la lisibilite de l'architecture frontend ;
- la maintenabilite des styles et composants UI.

---

## 3. Solutions envisagees

| Solution | Avantages | Inconvenients | Choix |
|----------|----------|--------------|------|
| Un seul composant Angular pour tout gerer (landing, login, register, home, boutons, logo, logique de session) | Mise en place rapide, peu de fichiers | Composant trop charge, styles peu reutilisables, duplication potentielle, maintenance plus difficile | ❌ |
| Separation en composants UI reutilisables + service d'authentification partage + page home dediee | Responsabilites mieux separees, code plus maintenable, composants reutilisables, meilleure evolution future | Plus de fichiers a structurer, refactor initial un peu plus long | ✅ |

---

## 4. Solution retenue

La solution retenue repose sur trois axes :

- un `AuthService` partage pour centraliser l'etat de session et les appels `login`, `register`, `logout` et `me` ;
- des composants UI reutilisables pour les elements repetes (`brand-logo`, `ui-button`, `topbar`) ;
- des pages distinctes pour les usages differents :
  - page publique d'entree ;
  - pages de connexion et d'inscription ;
  - page `home` pour l'utilisateur connecte.

Le service conserve l'utilisateur courant et gere la restauration de session depuis le token stocke localement. Les composants d'interface restent legers et se concentrent sur l'affichage et les interactions utilisateur.

---

## 5. Justification du choix

Le choix d'un service partage et d'un petit socle de composants UI a ete retenu car il repond mieux aux besoins de maintenabilite du projet.

Une approche basee sur un seul gros composant aurait pu fonctionner a court terme, mais elle aurait rapidement accumule plusieurs responsabilites : gestion des formulaires, navigation, session utilisateur, logo, boutons, topbar, et page connectee. Cette concentration augmente le risque de regressions et rend le code plus difficile a relire.

Au contraire, la solution retenue isole :
- la logique metier de session dans un service dedie ;
- la presentation recurrente dans des composants reutilisables ;
- les ecrans dans des pages ou composants de feature.

Ce choix est plus coherent avec Angular et avec l'organisation par features demandee dans le projet. Il facilite aussi l'ajout futur d'un interceptor HTTP, d'une navbar complete, ou de nouvelles pages authentifiees sans reintroduire de duplication.

---

## 6. Impact sur l'architecture

Fichiers impactes ou ajoutes :

- `front/src/app/core/auth/auth.service.ts`
- `front/src/app/shared/ui/brand-logo.component.ts`
- `front/src/app/shared/ui/ui-button.component.ts`
- `front/src/app/shared/ui/topbar.component.ts`
- `front/src/app/features/auth/auth-page.component.ts`
- `front/src/app/features/auth/auth-page.component.html`
- `front/src/app/features/auth/auth-page.component.scss`
- `front/src/app/features/home/home-page.component.ts`
- `front/src/app/app.routes.ts`

Couches concernees :
- couche presentation Angular ;
- couche de service frontend pour l'authentification ;
- routage applicatif.

Modification structurelle principale :
- passage d'un ecran d'authentification concentre a une structure plus modulaire `core / shared / features`.

---

## 7. Bonnes pratiques appliquees

- Separation des responsabilites entre service, composants UI et pages.
- Organisation du frontend par features, conforme aux orientations du projet.
- Mutualisation des elements repetes pour limiter la duplication.
- Validation reactive avec `ReactiveFormsModule`.
- Redirections explicites selon l'etat de session.
- Conservation d'une interface simple sans ajouter de dependances UI externes inutiles.

---

## 8. Securite

Risques identifies :
- exposition de routes connectees sans verification de session ;
- incoherence entre l'etat visuel du formulaire et l'etat reel de la session ;
- conservation d'un token invalide dans le navigateur.

Protections mises en place :
- verification de la session via l'endpoint `/auth/me` ;
- suppression de la session locale si la verification echoue ;
- redirection vers l'accueil si l'utilisateur n'est pas authentifie pour acceder a `/home` ;
- validation frontend avant envoi des formulaires.

Le frontend ne remplace pas les controles backend, mais il limite les erreurs d'usage et evite des etats incoherents cote interface.

---

## 9. Points d'amelioration

- Ajouter un `HttpInterceptor` Angular pour injecter automatiquement le token JWT sur les futures requetes protegees.
- Introduire un vrai guard de route Angular pour les pages authentifiees.
- Aligner completement le login avec un backend acceptant reellement email ou nom d'utilisateur.
- Extraire encore davantage les styles globaux si d'autres ecrans reprennent les memes patterns.
- Ajouter des tests unitaires Angular sur le service d'authentification et les redirections.

---

## 10. Sources

- Angular Style Guide : utile pour justifier le decoupage entre composants, services et responsabilites frontend. La recommandation de separer clairement les roles a guide le choix d'un `AuthService` et de composants UI reutilisables.
- Reactive Forms in Angular : utile pour comparer une gestion template-driven et une gestion reactive des formulaires. Cette source soutient le choix des formulaires reactifs pour mieux controler validation, etat et evolution des champs.
- OWASP Authentication Cheat Sheet : utile pour rappeler les bonnes pratiques de gestion de session, de validation et de separation entre controles frontend et backend. La source a servi de cadre de vigilance sur les flux de connexion et de deconnexion.
