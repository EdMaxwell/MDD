# Rapport technique - Base PostgreSQL avec Docker

Date : 2026-04-10  
Auteur : Codex

---

## 1. Contexte

Le projet MDD dispose maintenant d'un backend Spring Boot utilisant PostgreSQL, mais aucun environnement de base de données local standardisé n'était fourni.

Le besoin est de permettre à tout développeur de démarrer rapidement une base PostgreSQL cohérente avec le backend, sans installation locale spécifique.

---

## 2. Problème technique

Il fallait fournir :
- une base PostgreSQL locale simple à lancer ;
- une configuration cohérente avec les variables déjà utilisées par le backend ;
- une solution persistante mais légère ;
- un démarrage reproductible pour l'équipe.

Contraintes :
- rester simple ;
- ne pas introduire d'orchestration inutile ;
- conserver la compatibilité avec une exécution du backend hors Docker.

---

## 3. Solutions envisagées

| Solution | Avantages | Inconvénients | Choix |
|----------|----------|--------------|------|
| Installation PostgreSQL locale manuelle | Pas de couche Docker supplémentaire | Configuration hétérogène selon les postes, onboarding plus long, plus d'erreurs d'environnement | ❌ |
| PostgreSQL via Docker Compose | Standardisé, rapide à lancer, persistance simple via volume, aligné avec les variables du backend | Nécessite Docker Desktop, ajoute un fichier d'orchestration à maintenir | ✅ |

---

## 4. Solution retenue

La solution retenue est un service PostgreSQL unique défini dans `docker-compose.yml`.

Caractéristiques :
- image `postgres:17-alpine` pour rester légère ;
- volume Docker nommé pour conserver les données ;
- variables d'environnement centralisées via `.env` ;
- port exposé configurable ;
- healthcheck avec `pg_isready`.

---

## 5. Justification du choix

Docker Compose apporte ici le meilleur compromis entre simplicité d'usage et reproductibilité. Le projet n'a pas besoin d'une stack multi-services complexe à ce stade, donc un seul service Compose suffit largement.

Cette approche est également cohérente avec la configuration Spring déjà mise en place : le backend lit `DB_URL`, `DB_USERNAME` et `DB_PASSWORD`, ce qui permet de brancher la base Docker sans couplage fort à l'infrastructure.

L'installation manuelle a été écartée car elle augmente les écarts entre environnements développeurs et ralentit l'onboarding. Pour un projet pédagogique ou collaboratif, cet écart est généralement plus coûteux que l'ajout d'un fichier Compose.

---

## 6. Impact sur l'architecture

Fichiers impactés :
- ajout de `docker-compose.yml` à la racine ;
- ajout de `.env.example` pour documenter les variables ;
- mise à jour de `.gitignore` pour éviter de versionner `.env` ;
- ajout de ce rapport dans `docs/reports/`.

Couches concernées :
- infrastructure locale ;
- configuration applicative ;
- documentation projet.

---

## 7. Bonnes pratiques appliquées

- Externalisation des secrets et paramètres via variables d'environnement.
- Persistance des données via volume nommé Docker.
- Utilisation d'un healthcheck pour mieux diagnostiquer la disponibilité du service.
- Séparation claire entre configuration versionnée (`.env.example`) et configuration locale réelle (`.env`).

---

## 8. Sécurité

Risques :
- versionner accidentellement un vrai mot de passe ;
- exposer la base sur un port conflictuel ou non souhaité ;
- utiliser des identifiants faibles au-delà du développement local.

Protections mises en place :
- `.env` ignoré par Git ;
- fichier `.env.example` sans secret réel ;
- configuration explicitement orientée développement local ;
- possibilité de surcharger les identifiants et le port.

---

## 9. Points d'amélioration

- Ajouter un service backend Dockerisé si l'équipe veut un lancement full stack via Compose.
- Prévoir un script d'initialisation SQL si le projet a besoin de données de seed.
- Ajouter des profils Compose si des besoins différents apparaissent entre dev et test.

---

## 10. Sources

- Docker Docs, *Docker Compose getting started*. Intérêt : base pratique sur la structuration d'un fichier Compose simple et maintenable. [https://docs.docker.com/compose/gettingstarted/](https://docs.docker.com/compose/gettingstarted/)
- PostgreSQL Docker Official Image. Intérêt : documentation d'usage des variables d'environnement officielles et du stockage persistant du conteneur PostgreSQL. [https://hub.docker.com/_/postgres](https://hub.docker.com/_/postgres)
- Baeldung, *Dockerize PostgreSQL*. Intérêt : retour d'expérience explicatif sur la mise en place d'une base PostgreSQL locale avec Docker dans un contexte de développement. [https://www.baeldung.com/ops/postgresql-docker-setup](https://www.baeldung.com/ops/postgresql-docker-setup)
