package com.eduadmin.service;

import com.eduadmin.model.*;
import com.eduadmin.model.Module;
import com.eduadmin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrientationService {

    private final EtudiantRepository etudiantRepository;
    private final FiliereRepository filiereRepository;
    private final ChoixFiliereRepository choixFiliereRepository;
    private final AffectationFiliereRepository affectationFiliereRepository;
    private final FormuleOrientationRepository formuleOrientationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final ModuleRepository moduleRepository;
    private final NoteRepository noteRepository;
    private final NotificationRepository notificationRepository;
    private final JournalService journalService;
    private final AnneeUniversitaireRepository anneeUniversitaireRepository;

    // Calcule la moyenne d'un module spécifique pour un étudiant et un semestre donnés
    public double getMoyenneModule(Etudiant etudiant, String semestreCode, int moduleIndex) {
        // Trouver l'inscription du semestre
        Optional<Inscription> insOpt = inscriptionRepository.findByEtudiantIdAndSemestreCodeAndAnneeUniversitaireCouranteTrue(etudiant.getId(), semestreCode);
        if (insOpt.isEmpty()) return 0.0;

        Inscription ins = insOpt.get();
        List<Module> modules;
        if (etudiant.getFiliereActuelle() != null) {
            modules = moduleRepository.findBySemestreIdAndFiliereId(ins.getSemestre().getId(), etudiant.getFiliereActuelle().getId());
        } else {
            modules = moduleRepository.findBySemestreId(ins.getSemestre().getId());
        }
        if (modules.size() <= moduleIndex) return 0.0;

        Module module = modules.get(moduleIndex);
        List<Matiere> matieres = module.getMatieres();
        if (matieres.isEmpty()) return 0.0;

        double sommeNotesCoeff = 0.0;
        double sommeCoeff = 0.0;

        for (Matiere matiere : matieres) {
            Optional<Note> noteOpt = noteRepository.findByInscriptionIdAndMatiereId(ins.getId(), matiere.getId());
            double noteGen = noteOpt.map(n -> n.getNoteGenerale() != null ? n.getNoteGenerale() : 0.0).orElse(0.0);
            sommeNotesCoeff += noteGen * matiere.getCoefficient();
            sommeCoeff += matiere.getCoefficient();
        }

        return sommeCoeff > 0 ? (sommeNotesCoeff / sommeCoeff) : 0.0;
    }

    // Calcule la moyenne d'orientation pour un étudiant et une filière cible
    public double calculerMoyenneOrientation(Etudiant etudiant, Filiere filiere) {
        Optional<FormuleOrientation> formuleOpt = formuleOrientationRepository.findByFiliereId(filiere.getId());
        String formuleStr = formuleOpt.map(FormuleOrientation::getFormule).orElse("(MO1S1 + MO1S2 + 2*MO2S1 + 2*MO2S2 + MGS1 + MGS2) / 8");

        // Calculer les variables de la formule
        double mo1s1 = getMoyenneModule(etudiant, "S1", 0);
        double mo1s2 = getMoyenneModule(etudiant, "S2", 0);
        double mo2s1 = getMoyenneModule(etudiant, "S1", 1);
        double mo2s2 = getMoyenneModule(etudiant, "S2", 1);

        Optional<Inscription> s1Opt = inscriptionRepository.findByEtudiantIdAndSemestreCodeAndAnneeUniversitaireCouranteTrue(etudiant.getId(), "S1");
        Optional<Inscription> s2Opt = inscriptionRepository.findByEtudiantIdAndSemestreCodeAndAnneeUniversitaireCouranteTrue(etudiant.getId(), "S2");

        double mgs1 = s1Opt.map(i -> i.getMoyenneSemestre() != null ? i.getMoyenneSemestre() : 0.0).orElse(0.0);
        double mgs2 = s2Opt.map(i -> i.getMoyenneSemestre() != null ? i.getMoyenneSemestre() : 0.0).orElse(0.0);

        // Nettoyage de l'expression pour SpEL
        String expr = formuleStr
                .replace("MO1S1", String.valueOf(mo1s1))
                .replace("MO1S2", String.valueOf(mo1s2))
                .replace("MO2S1", String.valueOf(mo2s1))
                .replace("MO2S2", String.valueOf(mo2s2))
                .replace("MGS1", String.valueOf(mgs1))
                .replace("MGS2", String.valueOf(mgs2));

        try {
            ExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(expr);
            Double resultat = expression.getValue(Double.class);
            return resultat != null ? Math.round(resultat * 100.0) / 100.0 : 0.0;
        } catch (Exception e) {
            log.error("Erreur de parsing de la formule d'orientation : {} | Formule: {}", e.getMessage(), formuleStr);
            // Formule de secours en cas d'erreur
            return Math.round(((mo1s1 + mo1s2 + 2 * mo2s1 + 2 * mo2s2 + mgs1 + mgs2) / 8.0) * 100.0) / 100.0;
        }
    }

    // Exécute l'algorithme d'affectation automatique
    @Transactional
    public void affecterEtudiantsAutomatiquement(String executeur) {
        AnneeUniversitaire anneeCourante = anneeUniversitaireRepository.findByCouranteTrue()
                .orElseThrow(() -> new IllegalStateException("Aucune annee universitaire courante definie"));

        // 1. Récupérer tous les étudiants de L1
        List<Etudiant> etudiantsL1 = etudiantRepository.findByNiveau("L1");
        if (etudiantsL1.isEmpty()) {
            log.warn("Aucun etudiant en L1 pour l'affectation automatique.");
            return;
        }

        // Nettoyer les anciennes affectations de cette année
        List<AffectationFiliere> anciennes = affectationFiliereRepository.findByAnneeUniversitaireId(anneeCourante.getId());
        affectationFiliereRepository.deleteAll(anciennes);

        // 2. Préparer les données pour le classement
        // Pour classer, on calcule la moyenne générale d'orientation (formule générique ou moyenne simple)
        // afin de donner un classement unique des étudiants.
        class EtudiantScore {
            final Etudiant etudiant;
            final double scoreGénéral;
            EtudiantScore(Etudiant etudiant, double scoreGénéral) {
                this.etudiant = etudiant;
                this.scoreGénéral = scoreGénéral;
            }
        }

        List<EtudiantScore> classés = new ArrayList<>();
        for (Etudiant et : etudiantsL1) {
            // Utiliser une formule par défaut pour classer (moyenne simple de S1 et S2)
            double score = calculerMoyenneOrientation(et, Filiere.builder().id(0L).build()); // formule par défaut
            classés.add(new EtudiantScore(et, score));
        }

        // Trier par score décroissant
        classés.sort((a, b) -> Double.compare(b.scoreGénéral, a.scoreGénéral));

        // 3. Charger les capacités des filières
        List<Filiere> filieres = filiereRepository.findAll();
        Map<Long, Integer> capacitesRestantes = new HashMap<>();
        for (Filiere f : filieres) {
            capacitesRestantes.put(f.getId(), f.getCapaciteMax());
        }

        // 4. Affectation pas à pas
        for (EtudiantScore es : classés) {
            Etudiant et = es.etudiant;
            // Récupérer ses choix formulés
            List<ChoixFiliere> choix = choixFiliereRepository.findByEtudiantIdAndAnneeUniversitaireIdOrderByOrdrePreferenceAsc(et.getId(), anneeCourante.getId());
            boolean affecte = false;

            for (ChoixFiliere ch : choix) {
                Filiere filiereChoisie = ch.getFiliere();
                int placesDispos = capacitesRestantes.getOrDefault(filiereChoisie.getId(), 0);

                if (placesDispos > 0) {
                    // Affecter l'étudiant
                    AffectationFiliere aff = AffectationFiliere.builder()
                            .etudiant(et)
                            .filiereAffectee(filiereChoisie)
                            .moyenneOrientation(es.scoreGénéral)
                            .anneeUniversitaire(anneeCourante)
                            .dateAffectation(LocalDate.now())
                            .build();

                    affectationFiliereRepository.save(aff);

                    // Mettre à jour son dossier étudiant pour l'inscrire en L2 dans cette filière
                    et.setFiliereActuelle(filiereChoisie);
                    et.setNiveau("L2"); // Promotion automatique en L2
                    etudiantRepository.save(et);

                    // Réduire la capacité restante
                    capacitesRestantes.put(filiereChoisie.getId(), placesDispos - 1);

                    // Envoyer une notification automatique
                    Notification notif = Notification.builder()
                            .destinataire(et.getUtilisateur())
                            .titre("Affectation de Filiere")
                            .message("Felicitations ! Vous avez ete affecte a la filiere " + filiereChoisie.getCode() + " en L2 pour l'annee " + anneeCourante.getLibelle() + ".")
                            .dateCreation(LocalDateTime.now())
                            .lue(false)
                            .build();
                    notificationRepository.save(notif);

                    affecte = true;
                    break;
                }
            }

            if (!affecte) {
                // Pas de place dans les vœux, affecter à une filière avec de la place par défaut
                for (Filiere f : filieres) {
                    int places = capacitesRestantes.getOrDefault(f.getId(), 0);
                    if (places > 0 && !f.getCode().equals("MPI") && !f.getCode().equals("BG") && !f.getCode().equals("PC")) {
                        AffectationFiliere aff = AffectationFiliere.builder()
                                .etudiant(et)
                                .filiereAffectee(f)
                                .moyenneOrientation(es.scoreGénéral)
                                .anneeUniversitaire(anneeCourante)
                                .dateAffectation(LocalDate.now())
                                .build();
                        affectationFiliereRepository.save(aff);

                        et.setFiliereActuelle(f);
                        et.setNiveau("L2");
                        etudiantRepository.save(et);

                        capacitesRestantes.put(f.getId(), places - 1);

                        Notification notif = Notification.builder()
                                .destinataire(et.getUtilisateur())
                                .titre("Affectation de Filiere (Choix par defaut)")
                                .message("Vos voeux etant satures, vous avez ete affecte par defaut a la filiere " + f.getCode() + ".")
                                .dateCreation(LocalDateTime.now())
                                .lue(false)
                                .build();
                        notificationRepository.save(notif);

                        break;
                    }
                }
            }
        }

        journalService.log(executeur, "AFFECTATION_AUTOMATIQUE", 
            "Affectation automatique des filieres executee pour " + classés.size() + " etudiants de L1");
    }
}
