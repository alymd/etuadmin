package com.eduadmin.service;

import com.eduadmin.model.*;
import com.eduadmin.model.Module;
import com.eduadmin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final NoteRepository noteRepository;
    private final InscriptionRepository inscriptionRepository;
    private final ModuleRepository moduleRepository;
    private final HistoriqueParcoursRepository historiqueParcoursRepository;

    public void enregistrerNote(Note note, String name) {
    }

    @Transactional
    public void calculerResultatsInscription(Long inscriptionId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscription introuvable"));

        Semestre semestre = inscription.getSemestre();
        Etudiant etudiant = inscription.getEtudiant();

        List<Module> modules;
        if (etudiant.getFiliereActuelle() != null && semestre.getCode().substring(0, 2).equals(etudiant.getNiveau())) {
            modules = moduleRepository.findBySemestreIdAndFiliereId(semestre.getId(), etudiant.getFiliereActuelle().getId());
        } else {
            modules = moduleRepository.findBySemestreId(semestre.getId());
        }

        double sommeMoyennesModulesCredits = 0.0;
        int totalCreditsSemestre = 0;
        int totalCreditsObtenus = 0;
        boolean semestreValide = true;

        for (Module module : modules) {
            List<Matiere> matieres = module.getMatieres();
            if (matieres.isEmpty()) continue;

            Map<Matiere, Double> notesMap = new HashMap<>();
            double sommeNotesCoeff = 0.0;
            double sommeCoeff = 0.0;

            for (Matiere matiere : matieres) {
                Optional<Note> noteOpt = noteRepository.findByInscriptionIdAndMatiereId(inscription.getId(), matiere.getId());
                double noteGen = 0.0;

                if (noteOpt.isPresent()) {
                    Note n = noteOpt.get();
                    if (n.getNoteGenerale() == null) {
                        n.recalculerNoteGenerale();
                    }
                    noteGen = n.getNoteGenerale() != null ? n.getNoteGenerale() : 0.0;
                }
                notesMap.put(matiere, noteGen);
                sommeNotesCoeff += noteGen * matiere.getCoefficient();
                sommeCoeff += matiere.getCoefficient();
            }

            double moyenneModule = sommeCoeff > 0 ? (sommeNotesCoeff / sommeCoeff) : 0.0;
            moyenneModule = Math.round(moyenneModule * 100.0) / 100.0;

            int creditsModule = module.getCreditsCalculated() > 0 ? module.getCreditsCalculated() : matieres.stream().mapToInt(Matiere::getCredits).sum();
            sommeMoyennesModulesCredits += moyenneModule * creditsModule;
            totalCreditsSemestre += creditsModule;

            Set<Long> matieresValideesIds = new HashSet<>();

            for (Matiere m1 : matieres) {
                double note1 = notesMap.get(m1);

                if (note1 >= 10.0 || moyenneModule >= 10.0) {
                    matieresValideesIds.add(m1.getId());
                    continue;
                }

                if (note1 >= 7.0 && note1 < 10.0) {
                    for (Matiere m2 : matieres) {
                        if (m1.getId().equals(m2.getId())) continue;
                        double note2 = notesMap.get(m2);

                        if (note2 > 10.0) {
                            double pointsNeeded = 10.0 - note1;
                            double pointsAvailable = note2 - 10.0;
                            double relativeSurplus = pointsAvailable * ((double) m2.getCredits() / m1.getCredits());

                            if (relativeSurplus >= pointsNeeded) {
                                matieresValideesIds.add(m1.getId());
                                log.info("Intra-module Compensation: {} validated via surplus from {}", m1.getCode(), m2.getCode());
                                break;
                            }
                        }
                    }
                }
            }

            boolean allMatieresPassed = true;
            for (Matiere matiere : matieres) {
                boolean isValide = matieresValideesIds.contains(matiere.getId());

                if (isValide) {
                    totalCreditsObtenus += matiere.getCredits();
                } else {
                    allMatieresPassed = false;
                }

                Optional<Note> nOpt = noteRepository.findByInscriptionIdAndMatiereId(inscription.getId(), matiere.getId());
                if (nOpt.isPresent()) {
                    Note n = nOpt.get();
                    n.setValidee(isValide);
                    noteRepository.save(n);
                }
            }

            if (!allMatieresPassed) {
                semestreValide = false;
            }
        }

        double moyenneSemestre = totalCreditsSemestre > 0 ? (sommeMoyennesModulesCredits / totalCreditsSemestre) : 0.0;
        moyenneSemestre = Math.round(moyenneSemestre * 100.0) / 100.0;

        inscription.setMoyenneSemestre(moyenneSemestre);
        inscription.setCreditsObtenus(totalCreditsObtenus);
        inscription.setValide(semestreValide || moyenneSemestre >= 10.0);

        inscriptionRepository.save(inscription);

        calculerResultatsAnnuels(etudiant.getId(), inscription.getAnneeUniversitaire().getId());
    }

    @Transactional
    public void calculerResultatsAnnuels(Long etudiantId, Long anneeId) {
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
                anneeValide = false;
            }
        }

        double moyenneAnnuelle = countSemestres > 0 ? (sommeMoyennes / countSemestres) : 0.0;
        moyenneAnnuelle = Math.round(moyenneAnnuelle * 100.0) / 100.0;

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
        historique.setValide(anneeValide || moyenneAnnuelle >= 10.0);
        historique.setFiliere(inscriptions.getFirst().getEtudiant().getFiliereActuelle());
        historique.setNiveau(inscriptions.getFirst().getEtudiant().getNiveau());

        historiqueParcoursRepository.save(historique);
        log.debug("Historique annuel mis a jour pour l'etudiant ID: {} | Moyenne: {} | Valide: {}", etudiantId, moyenneAnnuelle, historique.isValide());
    }

    // --- CLEANED & DEDUPED METRIC PROVIDERS ---

    public double calculerMoyenneGenerale(Long etudiantId) {
        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(etudiantId);
        if (inscriptions.isEmpty()) return 0.0;

        double totalAverages = inscriptions.stream()
                .mapToDouble(i -> i.getMoyenneSemestre() != null ? i.getMoyenneSemestre() : 0.0)
                .sum();
        return Math.round((totalAverages / inscriptions.size()) * 100.0) / 100.0;
    }

    public int calculerTotalCreditsObtenus(Long etudiantId) {
        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(etudiantId);
        return inscriptions.stream()
                .mapToInt(i -> i.getCreditsObtenus() != null ? i.getCreditsObtenus() : 0)
                .sum();
    }

    public MentionAcademique calculerMentionGenerale(Long etudiantId) {
        double moyenne = calculerMoyenneGenerale(etudiantId);
        return calculerMention(moyenne);
    }

    public MentionAcademique calculerMention(Double moyenne) {
        if (moyenne == null || moyenne < 10.0) return MentionAcademique.AJOURNE;
        if (moyenne < 12.0) return MentionAcademique.PASSABLE;
        if (moyenne < 14.0) return MentionAcademique.ASSEZ_BIEN;
        if (moyenne < 16.0) return MentionAcademique.BIEN;
        return MentionAcademique.TRES_BIEN;
    }
}