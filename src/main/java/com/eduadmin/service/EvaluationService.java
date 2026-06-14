package com.eduadmin.service;

import com.eduadmin.model.*;
import com.eduadmin.model.Module;
import com.eduadmin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final NoteRepository noteRepository;
    private final InscriptionRepository inscriptionRepository;
    private final ModuleRepository moduleRepository;
    private final HistoriqueParcoursRepository historiqueParcoursRepository;
    private final JournalService journalService;

    /**
     * Classe interne pour stocker les résultats de validation d'un module
     */
    private static class ModuleValidationResult {
        double moyenneModule;
        boolean presenceNoteEliminatoire;
        boolean hasNoteGreaterThan7;
        boolean valide;
        List<Matiere> matieres;

        ModuleValidationResult(double moyenneModule, boolean presenceNoteEliminatoire, 
                               boolean hasNoteGreaterThan7, boolean valide, List<Matiere> matieres) {
            this.moyenneModule = moyenneModule;
            this.presenceNoteEliminatoire = presenceNoteEliminatoire;
            this.hasNoteGreaterThan7 = hasNoteGreaterThan7;
            this.valide = valide;
            this.matieres = matieres;
        }

        boolean isValide() {
            return valide;
        }
    }

    // Recalcule la note générale et valide la note individuellement
    @Transactional
    public Note enregistrerNote(Note note, String auteur) {
        note.recalculerNoteGenerale();
        Note sauvée = noteRepository.save(note);
        
        // Recalculer le semestre de l'inscription correspondante
        calculerResultatsInscription(sauvée.getInscription().getId());
        
        journalService.log(auteur, "SAISIE_NOTE", 
            "Note enregistree pour l'etudiant " + sauvée.getInscription().getEtudiant().getMatricule() + 
            " dans la matiere " + sauvée.getMatiere().getCode() + 
            " (CC: " + sauvée.getNoteCC() + ", Exam: " + sauvée.getNoteExamen() + 
            ", TP: " + sauvée.getNoteTP() + ", Rattrapage: " + sauvée.getNoteRattrapage() + 
            " -> Generale: " + sauvée.getNoteGenerale() + ")");
        
        return sauvée;
    }

    // Calcul complet des résultats pour une inscription (Semestre)
    @Transactional
    public void calculerResultatsInscription(Long inscriptionId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscription introuvable"));

        Semestre semestre = inscription.getSemestre();
        Etudiant etudiant = inscription.getEtudiant();

        // Récupérer tous les modules du semestre correspondant à la filière de l'étudiant
        List<Module> modules;
        if (etudiant.getFiliereActuelle() != null) {
            modules = moduleRepository.findBySemestreIdAndFiliereId(semestre.getId(), etudiant.getFiliereActuelle().getId());
        } else {
            modules = moduleRepository.findBySemestreId(semestre.getId());
        }
        
        // Première passe: calculer le statut de validation initial de chaque module
        java.util.Map<Module, ModuleValidationResult> moduleResults = new java.util.LinkedHashMap<>();
        
        for (Module module : modules) {
            List<Matiere> matieres = module.getMatieres();
            if (matieres.isEmpty()) continue;

            double sommeNotesCoeff = 0.0;
            double sommeCoeff = 0.0;
            boolean presenceNoteEliminatoire = false; // Note générale < 7.0
            boolean hasNoteGreaterThan7 = false;

            for (Matiere matiere : matieres) {
                // Trouver la note de l'étudiant pour cette matière
                Optional<Note> noteOpt = noteRepository.findByInscriptionIdAndMatiereId(inscription.getId(), matiere.getId());
                double noteGen = 0.0;
                if (noteOpt.isPresent()) {
                    Note note = noteOpt.get();
                    if (note.getNoteGenerale() == null) {
                        note.recalculerNoteGenerale();
                        noteRepository.save(note);
                    }
                    noteGen = note.getNoteGenerale() != null ? note.getNoteGenerale() : 0.0;
                }

                if (noteGen < 7.0) {
                    presenceNoteEliminatoire = true;
                }
                if (noteGen > 7.0) {
                    hasNoteGreaterThan7 = true;
                }

                sommeNotesCoeff += noteGen * matiere.getCoefficient();
                sommeCoeff += matiere.getCoefficient();
            }

            double moyenneModule = sommeCoeff > 0 ? (sommeNotesCoeff / sommeCoeff) : 0.0;
            moyenneModule = Math.round(moyenneModule * 100.0) / 100.0;

            boolean moduleValide = (moyenneModule >= 10.0) && !presenceNoteEliminatoire;
            
            moduleResults.put(module, new ModuleValidationResult(
                moyenneModule, 
                presenceNoteEliminatoire, 
                hasNoteGreaterThan7, 
                moduleValide,
                matieres
            ));
        }
        
        // Deuxième passe: appliquer la règle de compensation si un seul module n'est pas validé
        long invalidModuleCount = moduleResults.values().stream().filter(r -> !r.isValide()).count();
        Module unvalidatedModule = null;
        if (invalidModuleCount == 1) {
            for (java.util.Map.Entry<Module, ModuleValidationResult> entry : moduleResults.entrySet()) {
                if (!entry.getValue().isValide()) {
                    unvalidatedModule = entry.getKey();
                    break;
                }
            }
        }

        // Appliquer la compensation: si moduleAvg >= 9 et au moins une matière > 7, valider le module
        if (unvalidatedModule != null) {
            ModuleValidationResult result = moduleResults.get(unvalidatedModule);
            if (result.moyenneModule >= 9.0 && result.hasNoteGreaterThan7) {
                result.valide = true;
                log.info("Compensation appliquée pour le module {} : moyenne {} >= 9.0 et matiere > 7", 
                    unvalidatedModule.getCode(), result.moyenneModule);
            }
        }

        // Troisième passe: traiter l'octroi des crédits et la validation des matières
        double sommeMoyennesModulesCredits = 0.0;
        int sommeCreditsModules = 0;
        int totalCreditsObtenus = 0;
        boolean semestreValide = true;

        for (java.util.Map.Entry<Module, ModuleValidationResult> entry : moduleResults.entrySet()) {
            Module module = entry.getKey();
            ModuleValidationResult result = entry.getValue();
            
            int creditsModule = module.getCreditsCalculated();
            sommeMoyennesModulesCredits += result.moyenneModule * creditsModule;
            sommeCreditsModules += creditsModule;

            if (result.isValide()) {
                // Si le module est validé, l'étudiant gagne tous ses crédits
                totalCreditsObtenus += creditsModule;
                
                // Mettre à jour la validation individuelle des matières du module pour refléter la compensation
                for (Matiere matiere : result.matieres) {
                    noteRepository.findByInscriptionIdAndMatiereId(inscription.getId(), matiere.getId()).ifPresent(n -> {
                        n.setValidee(true);
                        noteRepository.save(n);
                    });
                }
            } else {
                semestreValide = false;
                // Si le module n'est pas validé, l'étudiant ne gagne que les crédits des matières individuellement >= 10
                for (Matiere matiere : result.matieres) {
                    final double noteGenFinal = noteRepository.findByInscriptionIdAndMatiereId(inscription.getId(), matiere.getId())
                            .map(Note::getNoteGenerale).orElse(0.0);
                    if (noteGenFinal >= 10.0) {
                        totalCreditsObtenus += matiere.getCredits();
                        noteRepository.findByInscriptionIdAndMatiereId(inscription.getId(), matiere.getId()).ifPresent(n -> {
                            n.setValidee(true);
                            noteRepository.save(n);
                        });
                    } else {
                        noteRepository.findByInscriptionIdAndMatiereId(inscription.getId(), matiere.getId()).ifPresent(n -> {
                            n.setValidee(false);
                            noteRepository.save(n);
                        });
                    }
                }
            }
        }

        double moyenneSemestre = sommeCreditsModules > 0 ? (sommeMoyennesModulesCredits / sommeCreditsModules) : 0.0;
        moyenneSemestre = Math.round(moyenneSemestre * 100.0) / 100.0;

        inscription.setMoyenneSemestre(moyenneSemestre);
        inscription.setCreditsObtenus(totalCreditsObtenus);
        
        // Un semestre de 30 crédits est validé si la moyenne est >= 10 et que tous ses modules sont validés
        inscription.setValide(semestreValide && (moyenneSemestre >= 10.0));
        inscriptionRepository.save(inscription);

        // Mettre à jour l'historique annuel après modification
        calculerResultatsAnnuels(etudiant.getId(), inscription.getAnneeUniversitaire().getId());
    }

    // Calcul complet des résultats annuels et mise à jour de l'historique
    @Transactional
    public void calculerResultatsAnnuels(Long etudiantId, Long anneeId) {
        // Trouver toutes les inscriptions de l'étudiant pour cette année
        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(etudiantId).stream()
                .filter(i -> i.getAnneeUniversitaire().getId().equals(anneeId))
                .toList();

        if (inscriptions.isEmpty()) return;

        double sommeMoyennes = 0.0;
        int countSemestres = 0;
        boolean anneeValide = true;

        for (Inscription ins : inscriptions) {
            if (ins.getMoyenneSemestre() != null) {
                sommeMoyennes += ins.getMoyenneSemestre();
                countSemestres++;
            }
            if (!ins.isValide()) {
                anneeValide = false; // L'année n'est validée que si les semestres sont validés
            }
        }

        double moyenneAnnuelle = countSemestres > 0 ? (sommeMoyennes / countSemestres) : 0.0;
        moyenneAnnuelle = Math.round(moyenneAnnuelle * 100.0) / 100.0;

        // Trouver ou créer l'historique de parcours pour cet étudiant et cette année
        HistoriqueParcours historique = historiqueParcoursRepository.findByEtudiantIdAndAnneeUniversitaireId(etudiantId, anneeId)
                .orElseGet(() -> {
                    Inscription ins = inscriptions.getFirst();
                    return HistoriqueParcours.builder()
                            .etudiant(ins.getEtudiant())
                            .anneeUniversitaire(ins.getAnneeUniversitaire())
                            .niveau(ins.getEtudiant().getNiveau())
                            .filiere(ins.getEtudiant().getFiliereActuelle())
                            .build();
                });

        historique.setMoyenneAnnuelle(moyenneAnnuelle);
        historique.setValide(anneeValide && (moyenneAnnuelle >= 10.0));
        historique.setFiliere(inscriptions.getFirst().getEtudiant().getFiliereActuelle());
        historique.setNiveau(inscriptions.getFirst().getEtudiant().getNiveau());
        
        historiqueParcoursRepository.save(historique);
        log.debug("Historique annuel mis a jour pour l'etudiant ID: {} | Moyenne: {} | Valide: {}", etudiantId, moyenneAnnuelle, historique.isValide());
    }

    // Calcul de la moyenne générale cumulative d'un étudiant sur l'ensemble de son parcours
    public double calculerMoyenneGenerale(Long etudiantId) {
        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(etudiantId);
        double somme = 0.0;
        int count = 0;
        for (Inscription ins : inscriptions) {
            if (ins.getMoyenneSemestre() != null) {
                somme += ins.getMoyenneSemestre();
                count++;
            }
        }
        double moyenneGen = count > 0 ? (somme / count) : 0.0;
        return Math.round(moyenneGen * 100.0) / 100.0;
    }

    // Calcul des crédits totaux obtenus sur tout le parcours
    public int calculerTotalCreditsObtenus(Long etudiantId) {
        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(etudiantId);
        return inscriptions.stream()
                .mapToInt(ins -> ins.getCreditsObtenus() != null ? ins.getCreditsObtenus() : 0)
                .sum();
    }

    public MentionAcademique calculerMention(Double moyenne) {
        return MentionAcademique.fromMoyenne(moyenne);
    }

    public MentionAcademique calculerMentionGenerale(Long etudiantId) {
        return calculerMention(calculerMoyenneGenerale(etudiantId));
    }
}
