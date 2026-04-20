# ORION  
## Spécifications fonctionnelles – Projet MDD  

**Auteur :** Orlando Espinoza  
**Version :** 0.0.1  

---

## 1. Objet du document

Le document “Spécifications fonctionnelles” liste les fonctionnalités à implémenter pour le projet MDD.  
Ces fonctionnalités sont exprimées du point de vue métier sous la forme d’actions que l’utilisateur peut effectuer sur l’application.

---

## 2. Périmètre

Les spécifications concernent uniquement la version MVP (Minimum Viable Product).  
La version MVP ne prévoit pas de back-office (zone administrateur).

---

## 3. Glossaire

| Terme français | Terme anglais | Description |
|---------------|--------------|------------|
| Utilisateur | User | Personne connectée au réseau social |
| Sujet / Thème | Subject / Topic | Thème de programmation |
| Article | Post | Message lié à un thème |
| Abonnement | Subscription | Suivre un thème |
| Fil | Feed | Articles des abonnements |

---

## 4. Liste des fonctionnalités

### 4.1 Gestion des utilisateurs

- Accéder au formulaire de connexion / inscription depuis la page d’accueil  
- S’inscrire (email, mot de passe, nom d’utilisateur)  
- Se connecter (email ou username + mot de passe)  
- Persistance de session  
- Consulter son profil  
- Modifier son profil  
- Se déconnecter  

---

### 4.2 Gestion des abonnements

- Consulter tous les thèmes  
- S’abonner à un thème  
- Se désabonner  

---

### 4.3 Gestion des articles

- Consulter le fil d’actualité (chronologique)  
- Trier les articles (récent / ancien)  
- Ajouter un article  
- Consulter un article  
- Ajouter un commentaire  

---

## 5. Exigences particulières

### Responsive

- L’application doit être utilisable sur mobile et desktop  

### Mot de passe

Un mot de passe valide doit contenir :

- Minimum 8 caractères  
- 1 chiffre  
- 1 minuscule  
- 1 majuscule  
- 1 caractère spécial  

### Articles

- Auteur et date définis automatiquement  

### Commentaires

- Auteur et date définis automatiquement  
- Pas de sous-commentaires (non récursif)  

### Abonnement

- Le bouton devient “Déjà abonné” après clic  

---

## 6. Règles complémentaires

- Un commentaire appartient à un seul article  
