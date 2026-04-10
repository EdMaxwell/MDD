# Technical Decisions Guidelines — Projet MDD

## Objectif

Ce document définit les règles que doivent suivre les agents (IA ou développeurs) lorsqu’ils implémentent une nouvelle fonctionnalité dans le projet.

Chaque décision technique doit être :
- justifiée ;
- documentée ;
- cohérente avec l’architecture existante.

---

## 1. Règle principale

⚠️ Toute modification significative du projet **doit** produire un rapport dans le dossier suivant :

```text
docs/reports/
```

Aucune implémentation importante ne doit être faite sans justification.

---

## 2. Structure attendue des rapports

Chaque intervention doit créer un fichier au format suivant :

```text
docs/reports/YYYY-MM-DD_nom-de-la-feature.md
```

### Exemples

```text
docs/reports/2026-04-09_auth-backend.md
docs/reports/2026-04-10_user-management.md
```

---

## 3. Template obligatoire

Chaque rapport **doit** respecter le template ci-dessous.

```md
# Rapport technique — [Nom de la feature]

Date : [YYYY-MM-DD]  
Auteur : [Agent / IA / Nom]

---

## 1. Contexte

Décrire brièvement :
- le besoin ;
- la fonctionnalité à implémenter ;
- le périmètre.

---

## 2. Problème technique

Expliquer :
- ce qui doit être résolu ;
- les contraintes techniques ;
- les points de vigilance.

---

## 3. Solutions envisagées

Présenter **au moins 2 solutions**.

| Solution | Avantages | Inconvénients | Choix |
|----------|----------|--------------|------|
| Solution A | ... | ... | ❌ |
| Solution B | ... | ... | ✅ |

---

## 4. Solution retenue

Décrire clairement :
- la solution choisie ;
- comment elle fonctionne ;
- pourquoi elle a été retenue.

---

## 5. Justification du choix

⚠️ **Obligatoire**

La justification doit :
- expliquer le raisonnement ;
- comparer avec les alternatives ;
- être argumentée ;
- éviter les justifications vagues comme « plus simple » sans explication.

---

## 6. Impact sur l’architecture

Décrire :
- les fichiers impactés ;
- les couches concernées ;
- les modifications structurelles.

---

## 7. Bonnes pratiques appliquées

Lister :
- les design patterns utilisés ;
- les principes SOLID si pertinents ;
- les conventions respectées.

---

## 8. Sécurité (si concerné)

Expliquer :
- les risques ;
- les protections mises en place.

---

## 9. Points d’amélioration

Lister :
- ce qui pourrait être amélioré plus tard ;
- ce qui est laissé volontairement simple ;
- les limites connues.

---

## 10. Sources

⚠️ **Important**

Les sources doivent être :
- comparatives ou explicatives ;
- utiles à la compréhension du choix ;
- pas uniquement de la documentation brute.

### Exemples acceptés
- articles comparatifs ;
- guides de bonnes pratiques ;
- recommandations de sécurité ;
- retours d’expérience techniques argumentés.

### Exemples refusés
- documentation officielle seule sans analyse ;
- source non vérifiée ou non pertinente ;
- lien sans explication de son intérêt.
```

---

## 4. Règles de décision technique

### 4.1 Nombre de choix

⚠️ **Important**

Pour chaque problématique :
- un seul choix final doit être retenu ;
- pas de « on verra plus tard » pour une décision structurante ;
- pas de multi-solutions concurrentes en production sans justification claire.

### 4.2 Comparatif obligatoire

Chaque décision doit inclure :
- au moins 2 options ;
- les avantages et inconvénients de chaque option ;
- la justification du rejet des options non retenues.

### 4.3 Cohérence du projet

Les choix doivent respecter les orientations du projet :
- Angular pour le front ;
- Spring Boot pour le back ;
- architecture en couches ;
- API REST.

---

## 5. Règles spécifiques au projet

### 5.1 Backend

Le backend doit respecter les choix suivants :
- Java 21 ;
- Spring Boot ;
- Spring Security ;
- architecture `Controller / Service / Repository`.

### 5.2 Frontend

Le frontend doit respecter les choix suivants :
- Angular ;
- organisation par features ;
- utilisation d’interceptors lorsque pertinent.

---

## 6. Ce qui est interdit

- ❌ Ajouter une nouvelle technologie sans justification.
- ❌ Modifier l’architecture sans rapport dédié.
- ❌ Introduire de la complexité inutile.
- ❌ Copier du code sans compréhension.

---

## 7. Philosophie du projet

Le projet privilégie :
- la clarté ;
- la maintenabilité ;
- la cohérence.

Plutôt que :
- la complexité ;
- l’optimisation prématurée ;
- les effets de mode.

---

## 8. Exemple de mission

### Mission

Implémenter l’authentification backend.

### Obligations

- créer `docs/reports/XXXX_auth-backend.md` ;
- comparer JWT vs session ;
- justifier le choix retenu ;
- expliquer les impacts sur l’architecture.

---

## 9. Usage attendu par les agents

Tout agent intervenant sur le projet doit considérer ce document comme une règle de travail.

Avant toute implémentation significative, il doit :
1. identifier le problème technique ;
2. comparer plusieurs approches ;
3. choisir une solution cohérente avec l’existant ;
4. produire un rapport dans `docs/reports/` ;
5. implémenter la solution retenue en restant aligné avec ce rapport.

Le rapport technique n’est pas optionnel : il fait partie intégrante du livrable.

