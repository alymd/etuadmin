# Plan d'Implémentation - EduAdmin (Système d'Information Universitaire)

Ce projet est une solution complète de gestion académique et administrative pour une université, développée en Java Spring Boot et entièrement en français.

## Conception de l'Architecture
Nous allons utiliser une architecture multicouche standard et propre (Clean Architecture / SOLID) :
1. **Entités (Entities)** : Définition des tables de la base de données relationnelle MySQL avec JPA/Hibernate.
2. **Repositories** : Interfaces héritant de `JpaRepository` pour l'accès aux données.
3. **DTOs** : Objets de transfert de données pour découpler la couche présentation de la couche persistante.
4. **Services** : Logique métier (calculs de moyennes, règles de compensation, algorithme d'affectation automatique, génération PDF/Excel, journalisation).
5. **Controllers** : Contrôleurs Thymeleaf MVC pour la gestion de l'affichage et de la navigation.
6. **Security** : Configuration de Spring Security pour le contrôle d'accès basé sur les rôles (ADMIN, SCOLARITE, ETUDIANT).
7. **Exceptions** : Gestion globale des erreurs avec `@ControllerAdvice`.

---

## Entités Obligatoires & Relations

Voici les entités JPA à créer et leurs relations :
1. **Utilisateur** : `id`, `username` (matricule), `password` (BCrypt), `nom`, `prenom`, `email`, `role`, `actif`.
2. **Role** : Enum ou entité (`ADMIN`, `SCOLARITE`, `ETUDIANT`).
3. **Etudiant** : Lié à `Utilisateur` (OneToOne), `matricule` (commençant par 'C'), `dateNaissance`, `telephone`, `adresse`, `filiereActuelle` (ManyToOne, nullable), `niveau` (L1, L2, L3).
4. **Filiere** : `id`, `code` (ex: MIAGE, DA2I), `nom`, `capaciteMax`.
5. **AnneeUniversitaire** : `id`, `libelle` (ex: 2025-2026), `courante` (boolean).
6. **Semestre** : `id`, `code` (S1 à S6), `dateDebut`, `dateFin`, `anneeUniversitaire` (ManyToOne).
7. **Module** : `id`, `code`, `nom`, `semestre` (ManyToOne), `creditsCalculated` (somme des crédits des matières).
8. **Matiere** : `id`, `code`, `nom`, `coefficient`, `credits`, `presenceTP` (boolean), `description`, `module` (ManyToOne).
9. **Note** : `id`, `inscription` (ManyToOne), `matiere` (ManyToOne), `noteCC` (nullable), `noteTP` (nullable), `noteExamen` (nullable), `noteRattrapage` (nullable), `noteGenerale` (calculée), `sessionExamen` (Enum NORMAL/RATTRAPAGE), `validee` (boolean).
10. **Inscription** : `id`, `etudiant` (ManyToOne), `semestre` (ManyToOne), `anneeUniversitaire` (ManyToOne), `moyenneSemestre` (Calculée), `creditsObtenus` (Calculés), `valide` (boolean).
11. **HistoriqueParcours** : `id`, `etudiant` (ManyToOne), `anneeUniversitaire` (ManyToOne), `niveau` (L1, L2, L3), `filiere` (ManyToOne, nullable), `moyenneAnnuelle`, `valide` (boolean).
12. **ChoixFiliere** : `id`, `etudiant` (ManyToOne), `filiere` (ManyToOne), `ordrePreference` (1 à 4), `anneeUniversitaire` (ManyToOne).
13. **AffectationFiliere** : `id`, `etudiant` (ManyToOne), `filiereAffectee` (ManyToOne), `moyenneOrientation`, `anneeUniversitaire` (ManyToOne), `dateAffectation`.
14. **FormuleOrientation** : `id`, `filiere` (OneToOne), `formuleSql` (ou expression Java parsée, ex: pour MIAGE).
15. **ReleveNote** : `id`, `inscription` (ManyToOne), `dateGeneration`, `type` (SEMESTRIEL, ANNUEL, COMPLET), `pdfUrl`.
16. **SessionExamen** : Enum (`NORMAL`, `RATTRAPAGE`).
17. **Annonce** : `id`, `titre`, `contenu`, `datePublication`, `auteur` (ManyToOne Utilisateur).
18. **Notification** : `id`, `destinataire` (ManyToOne Utilisateur), `titre`, `message`, `dateCreation`, `lue` (boolean).
19. **Reclamation** : `id`, `etudiant` (ManyToOne), `matiere` (ManyToOne), `noteConcernee` (double), `objet`, `description`, `pieceJointeNom` (String, nullable), `dateCreation`, `dateTraitement` (nullable), `commentaireAdmin` (nullable), `statut` (EN_ATTENTE, EN_COURS, ACCEPTEE, REJETEE).
20. **JournalActivite** : `id`, `utilisateur` (String), `dateAction`, `action` (String), `details` (String).

---

## Règles Métier & Logique Académique

1. **Génération de Compte Étudiant** :
   - À la création d'un étudiant, un `Utilisateur` est automatiquement généré.
   - `username` = `matricule` (généré avec le préfixe 'C' et 5 chiffres aléatoires ou incrémentés, ex: C28916).
   - `password` = `matricule` (haché par BCrypt).

2. **Calcul de la Note Générale d'une Matière** :
   - Si `presenceTP == true` : `noteGenerale = (noteTP + 2 * noteCC + 3 * noteExamen) / 6.0`
   - Si `presenceTP == false` : `noteGenerale = (2 * noteCC + 3 * noteExamen) / 5.0`
   - En cas de session de **Rattrapage** : si `noteRattrapage != null` et `noteRattrapage > noteExamen`, la `noteGenerale` est recalculée en remplaçant la note d'examen par la note de rattrapage (si cela améliore le résultat).

3. **Validation & Compensation** :
   - **Moyenne du Module** : Moyenne pondérée des matières du module :
     `MoyenneModule = (Somme(NoteGenerale_i * Coeff_i)) / Somme(Coeff_i)`
   - **Règle de Compensation** :
     - Une matière est validée directement si sa `noteGenerale >= 10.0`.
     - Si une matière a une `noteGenerale < 10.0`, elle peut être validée par compensation au sein du module si et seulement si :
       1. La `noteGenerale` de la matière est `>= 7.0/20`.
       2. La `MoyenneModule` est `>= 10.0/20`.
     - Si la note générale d'une matière est `< 7.0/20`, elle est non validée (éliminatoire) et empêche la validation automatique du module, même si la moyenne du module est `>= 10.0/20`.
   - **Crédits ECTS** :
     - Si le module est validé (moyenne `>= 10.0` et pas de matière `< 7.0`), l'étudiant obtient l'ensemble des crédits du module.
     - Si le module n'est pas validé, l'étudiant obtient uniquement les crédits des matières individuellement validées (note `>= 10.0`). Les crédits manquants sont conservés pour être validés ultérieurement.

4. **Affectation Automatique des Filières (L1 -> L2)** :
   - À la fin de L1, l'étudiant formule 4 choix ordonnés de filières.
   - Le système calcule la moyenne d'orientation pour chaque étudiant selon la formule de la base de données.
   - Les étudiants sont classés par ordre décroissant de cette moyenne.
   - Le système parcourt la liste classée et affecte chaque étudiant à son choix préféré disponible (dans la limite de la `capaciteMax` de la filière).
   - Si tous les choix d'un étudiant sont saturés, il est mis sur une liste d'attente ou affecté par défaut.
   - L'administrateur peut relancer cette affectation à tout moment.

5. **Formules d'Orientation** :
   - Exemple : pour MIAGE : `(MO1S1 + MO1S2 + 2*MO2S1 + 2*MO2S2 + MGS1 + MGS2)/8`
   - Ces formules seront stockées dans `FormuleOrientation` sous forme d'expressions littérales et évaluées dynamiquement via Spring Expression Language (SpEL) ou un interpréteur d'expressions.

---

## User Review Required

> [!IMPORTANT]
> **Base de Données MySQL** : L'application nécessite MySQL. Nous allons configurer la base de données dans `application.properties`. Avez-vous une base de données MySQL locale active ? Nous fournirons une configuration avec création de table automatique (`update`) et un seeder de données complet au démarrage si la base est vide.
>
> **Spring Boot 4.1 / Java 21** : Étant donné que Spring Boot 4 n'est pas encore publié, nous utiliserons Spring Boot 3.3.x (dernière version stable majeure) avec Java 21/25 (compatible avec le JDK 25 local).

---

## Proposed Changes

Nous allons créer la structure complète dans le répertoire de travail `c:\Users\pc\Desktop\eduadmin-test`.

### 1. Fichier de Configuration de Construction
- `[NEW]` [pom.xml](file:///c:/Users/pc/Desktop/eduadmin-test/pom.xml) : Configuration avec Spring Boot Starter Data JPA, Security, Thymeleaf, Validation, Web, Lombok, MySQL Connector, Apache POI (Excel), et OpenPDF (PDF).

### 2. Configuration & Propriétés
- `[NEW]` [application.properties](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/resources/application.properties) : Configuration de la source de données MySQL (`eduadmin_db`), Hibernate, Spring Security, et limites de taille de fichier pour les réclamations.

### 3. Modèles / Entités (JPA)
- `[NEW]` [Utilisateur.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/model/Utilisateur.java) et les autres entités énumérées dans la section Entités.

### 4. Répertoires (Repositories)
- `[NEW]` [UtilisateurRepository.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/repository/UtilisateurRepository.java) etc.

### 5. Services Métier
- `[NEW]` [EvaluationService.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/service/EvaluationService.java) : Calcul des notes, moyennes et crédits avec compensation.
- `[NEW]` [OrientationService.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/service/OrientationService.java) : Calcul des moyennes d'orientation et affectation automatique.
- `[NEW]` [DocumentService.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/service/DocumentService.java) : Génération de relevés PDF (OpenPDF) et exports Excel (Apache POI).
- `[NEW]` [JournalService.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/service/JournalService.java) : Journalisation des actions critiques.
- `[NEW]` [ImportExportService.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/service/ImportExportService.java) : Import Excel des étudiants et des notes.

### 6. Sécurité & Données de Démo
- `[NEW]` [SecurityConfig.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/config/SecurityConfig.java) : Configuration de la sécurité avec redirections basées sur les rôles.
- `[NEW]` [DataSeeder.java](file:///c:/Users/pc/Desktop/eduadmin-test/src/main/java/com/eduadmin/config/DataSeeder.java) : Générateur de données fictives complet au démarrage.

### 7. Contrôleurs (MVC) & Templates Thymeleaf
- `[NEW]` Contrôleurs pour l'administration générale, les notes, l'orientation, les réclamations, les annonces et le tableau de bord de chaque profil.
- `[NEW]` Templates Thymeleaf sous `src/main/resources/templates/` avec sidebar responsive en français, design premium Bootstrap 5, verre dépoli (glassmorphism) et animations.

---

## Verification Plan

### Tests Automatisés
- Commande Maven : `mvn clean test` pour vérifier le bon comportement des formules et des calculs de compensation.
- Démarrage et compilation de l'application : `mvn spring-boot:run`

### Manuel Verification
- Accès au serveur local à `http://localhost:8080`
- Test de connexion avec différents comptes (ADMIN, SCOLARITE, ETUDIANT).
- Vérification du processus d'affectation automatique.
- Téléchargement d'un relevé de notes au format PDF et Excel.
- Dépôt d'une réclamation avec pièce jointe par un étudiant, traitement par l'administration, et notification de l'étudiant.
