# EduAdmin — Système d'Information Universitaire

---

## Auteurs

| Matricule | Nom complet | Filière |
|-----------|-------------|---------|
| **C28916** | Aly Mohamed Aly | DA2I |
| **C27888** | Mouhamed Lehbib Beyrouk | MIAGE |

---

## Présentation du projet

**EduAdmin** est une application web de gestion académique et administrative destinée aux établissements universitaires. Elle permet à trois types d'utilisateurs — Administrateur, Scolarité et Étudiant — d'interagir avec les données pédagogiques et administratives au sein d'une interface unique, sécurisée et entièrement en français.

Le projet a été développé avec **Java 21** et **Spring Boot 3.3**, en suivant une architecture MVC multicouche et les principes SOLID.

---

## Fonctionnalités principales

### Espace Administrateur
- Gestion complète des étudiants (création, modification, suppression)
- Gestion des filières, modules et matières
- Gestion des comptes utilisateurs et des rôles
- Lancement de l'affectation automatique des filières (passage L1 → L2)
- Consultation du journal d'activité global
- Publication d'annonces

### Espace Scolarité
- Saisie et validation des notes (session normale & rattrapage)
- Calcul automatique des moyennes avec règles de compensation
- Calcul et attribution des crédits ECTS
- Génération de relevés de notes au format **PDF** (OpenPDF) et **Excel** (Apache POI)
- Import en masse des étudiants et des notes via fichier Excel
- Traitement des réclamations étudiantes

### Espace Étudiant
- Consultation du tableau de bord personnel et des notes par semestre
- Téléchargement du relevé de notes
- Dépôt de réclamations avec pièce jointe (max 5 Mo)
- Consultation des annonces et notifications

---

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Langage | Java 21 |
| Framework principal | Spring Boot 3.3.0 |
| Persistance | Spring Data JPA / Hibernate |
| Base de données | MySQL 8 |
| Sécurité | Spring Security 6 (BCrypt, RBAC) |
| Vue | Thymeleaf + Bootstrap 5 |
| Génération PDF | OpenPDF 1.3.40 |
| Génération Excel | Apache POI 5.2.5 |
| Build | Maven |
| Utilitaires | Lombok |

---

## Architecture du projet

Le projet suit une architecture **MVC multicouche** classique :

```
com.eduadmin
├── config/          → Configuration Spring Security & DataSeeder
├── controller/      → Contrôleurs MVC (Admin, Scolarité, Étudiant, Dashboard)
├── dto/             → Objets de transfert de données (formulaires, saisies)
├── exception/       → Gestion globale des erreurs (@ControllerAdvice)
├── model/           → Entités JPA (20 entités)
├── repository/      → Interfaces JpaRepository
└── service/         → Logique métier (Évaluation, Orientation, Documents, Import/Export, Journal)
```

Les templates Thymeleaf sont organisés par rôle sous `src/main/resources/templates/` (dossiers `admin/`, `scolarite/`, `etudiant/`).

---

## Modèle de données

Le schéma relationnel repose sur **20 entités JPA** principales, dont :

- `Utilisateur`, `Etudiant`, `Filiere`, `AnneeUniversitaire`
- `Semestre`, `Module`, `Matiere`
- `Inscription`, `Note`, `ReleveNote`
- `HistoriqueParcours`, `ChoixFiliere`, `AffectationFiliere`, `FormuleOrientation`
- `Annonce`, `Notification`, `Reclamation`, `JournalActivite`

Les tables sont créées et mises à jour automatiquement par Hibernate (`ddl-auto=update`). Un `DataSeeder` peuple la base avec des données de démonstration au premier démarrage.

---

## Règles métier académiques

### Calcul de la note générale d'une matière
- **Avec TP** : `(noteTP + 2×noteCC + 3×noteExamen) / 6`
- **Sans TP** : `(2×noteCC + 3×noteExamen) / 5`
- En session de **rattrapage**, la note d'examen est remplacée par la note de rattrapage si celle-ci est supérieure.

### Validation et compensation
- Une matière est validée si `noteGenerale ≥ 10/20`.
- La **compensation** au sein d'un module est possible si `noteGenerale ≥ 7/20` ET `moyenneModule ≥ 10/20`.
- Toute note `< 7/20` est **éliminatoire** et bloque la compensation du module.
- Les crédits ECTS sont accordés module par module ou matière par matière selon la validation.

### Affectation automatique des filières (L1 → L2)
1. Chaque étudiant de L1 formule 4 vœux ordonnés.
2. Le système calcule une **moyenne d'orientation** selon une formule propre à chaque filière (ex. MIAGE, DA2I), stockée dynamiquement en base.
3. Les étudiants sont classés par ordre décroissant de cette moyenne et affectés à leur meilleur vœu disponible dans la limite des capacités.

---

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **MySQL 8** en cours d'exécution localement
- Un IDE Java (IntelliJ IDEA recommandé)

---

## Installation et démarrage

### 1. Cloner le dépôt

```bash
git clone <url-du-depot>
cd eduadmin-test
```

### 2. Configurer la base de données

Ouvrir `src/main/resources/application.properties` et adapter les identifiants MySQL :

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/etuadmin-test?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

La base de données `etuadmin-test` sera créée automatiquement si elle n'existe pas.

### 3. Compiler et lancer

```bash
mvn clean spring-boot:run
```

L'application démarre sur [http://localhost:8080](http://localhost:8080).

Au premier démarrage, le `DataSeeder` injecte automatiquement des données de démonstration (utilisateurs, filières, étudiants, notes, etc.).

---

## Comptes de démonstration

| Rôle | Identifiant | Mot de passe |
|------|-------------|--------------|
| Administrateur | `admin` | `admin` |
| Scolarité | `scolarite` | `scolarite` |
| Étudiant | `C28916` | `C28916` |

> Les mots de passe étudiants correspondent à leur matricule et sont hachés en BCrypt.

---

## Sécurité

L'accès est contrôlé par Spring Security avec un système **RBAC (Role-Based Access Control)** à trois niveaux :

- `/admin/**` → `ROLE_ADMIN` uniquement
- `/scolarite/**` → `ROLE_ADMIN` ou `ROLE_SCOLARITE`
- `/etudiant/**` → `ROLE_ETUDIANT` uniquement

Après authentification, chaque utilisateur est redirigé vers son tableau de bord dédié.

---

## Tests

```bash
mvn clean test
```

Les tests couvrent la logique de calcul des notes, les règles de compensation et les cas limites de l'affectation des filières.

---

## Structure des fichiers sources

```
src/
├── main/
│   ├── java/com/eduadmin/
│   │   ├── EduadminApplication.java
│   │   ├── config/
│   │   │   ├── DataSeeder.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── AdminController.java
│   │   │   ├── DashboardController.java
│   │   │   ├── EtudiantController.java
│   │   │   └── ScolariteController.java
│   │   ├── dto/
│   │   │   ├── EtudiantFormDto.java
│   │   │   ├── NoteSaisieDto.java
│   │   │   └── ReclamationDto.java
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── model/           → 20 entités JPA
│   │   ├── repository/      → 18 repositories
│   │   └── service/
│   │       ├── DocumentService.java
│   │       ├── EtudiantService.java
│   │       ├── EvaluationService.java
│   │       ├── ImportExportService.java
│   │       ├── JournalService.java
│   │       ├── OrientationService.java
│   │       └── UtilisateurService.java
│   └── resources/
│       ├── application.properties
│       ├── static/css/style.css
│       └── templates/
│           ├── admin/
│           ├── scolarite/
│           ├── etudiant/
│           ├── dashboard_admin.html
│           ├── dashboard_etudiant.html
│           ├── dashboard_scolarite.html
│           ├── login.html
│           └── releve_print.html
└── test/
    └── java/com/eduadmin/
```


