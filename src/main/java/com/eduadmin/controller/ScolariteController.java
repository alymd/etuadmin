package com.eduadmin.controller;

import com.eduadmin.dto.*;
import com.eduadmin.model.*;
import com.eduadmin.model.Module;
import com.eduadmin.repository.*;
import com.eduadmin.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/scolarite")
@RequiredArgsConstructor
public class ScolariteController {

    private final EtudiantRepository etudiantRepository;
    private final InscriptionRepository inscriptionRepository;
    private final SemestreRepository semestreRepository;
    private final ModuleRepository moduleRepository;
    private final MatiereRepository matiereRepository;
    private final NoteRepository noteRepository;
    private final ReclamationRepository reclamationRepository;
    private final NotificationRepository notificationRepository;
    private final AnneeUniversitaireRepository anneeUniversitaireRepository;
    private final FiliereRepository filiereRepository;

    private final EvaluationService evaluationService;
    private final DocumentService documentService;
    private final JournalService journalService;
    private final EtudiantService etudiantService;


    @GetMapping("/etudiants")
    public String listeEtudiants(@RequestParam(required = false) String nom,
                                 @RequestParam(required = false) String matricule,
                                 @RequestParam(required = false) Long filiereId,
                                 @RequestParam(required = false) String niveau,
                                 @RequestParam(required = false) Long anneeId,
                                 Authentication auth, Model model) {
        model.addAttribute("etudiants", etudiantService.rechercher(nom, matricule, filiereId, niveau, anneeId));
        model.addAttribute("filieres", filiereRepository.findAll());
        model.addAttribute("annees", anneeUniversitaireRepository.findAll());
        model.addAttribute("nom", nom);
        model.addAttribute("matricule", matricule);
        model.addAttribute("filiereId", filiereId);
        model.addAttribute("niveau", niveau);
        model.addAttribute("anneeId", anneeId);
        model.addAttribute("username", auth.getName());
        return "scolarite/etudiants";
    }


    @GetMapping("/notes")
    public String consultationNotes(@RequestParam(required = false) Long semestreId,
                                    @RequestParam(required = false) Long moduleId,
                                    @RequestParam(required = false) Long matiereId,
                                    Authentication auth, Model model) {
        model.addAttribute("semestres", semestreRepository.findAll());
        model.addAttribute("semestreIdSelected", semestreId);
        model.addAttribute("moduleIdSelected", moduleId);
        model.addAttribute("matiereIdSelected", matiereId);

        model.addAttribute("modules", semestreId != null
                ? moduleRepository.findBySemestreId(semestreId)
                : List.of());

        if (moduleId != null) {
            Module module = moduleRepository.findById(moduleId).orElseThrow();
            model.addAttribute("moduleSelected", module);
            model.addAttribute("matieres", matiereRepository.findByModuleId(moduleId));
        } else {
            model.addAttribute("matieres", List.of());
        }

        if (semestreId != null && matiereId != null) {
            Matiere matiere = matiereRepository.findById(matiereId).orElseThrow();
            model.addAttribute("matiereSelected", matiere);

            List<Inscription> inscriptions = inscriptionRepository.findBySemestreId(semestreId);
            List<Note> notesEtudiants = new ArrayList<>();
            for (Inscription ins : inscriptions) {
                Note note = noteRepository.findByInscriptionIdAndMatiereId(ins.getId(), matiereId)
                        .orElseGet(() -> Note.builder()
                                .inscription(ins)
                                .matiere(matiere)
                                .validee(false)
                                .sessionExamen(SessionExamen.NORMAL)
                                .build());
                notesEtudiants.add(note);
            }
            model.addAttribute("notesEtudiants", notesEtudiants);
        }

        model.addAttribute("username", auth.getName());
        return "scolarite/notes";
    }

    @PostMapping("/notes/enregistrer")
    public String enregistrerNote(@ModelAttribute NoteSaisieDto dto, Authentication auth, RedirectAttributes redirect) {
        try {
            Inscription ins = inscriptionRepository.findById(dto.getInscriptionId()).orElseThrow();
            Matiere mat = matiereRepository.findById(dto.getMatiereId()).orElseThrow();

            Note note = noteRepository.findByInscriptionIdAndMatiereId(dto.getInscriptionId(), dto.getMatiereId())
                    .orElseGet(() -> Note.builder().inscription(ins).matiere(mat).build());

            note.setNoteCC(dto.getNoteCC());
            note.setNoteTP(dto.getNoteTP());
            note.setNoteExamen(dto.getNoteExamen());
            note.setNoteRattrapage(dto.getNoteRattrapage());

            evaluationService.enregistrerNote(note, auth.getName());
            redirect.addFlashAttribute("success", "Note mise a jour pour " + ins.getEtudiant().getMatricule());
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur lors de l'enregistrement de la note : " + e.getMessage());
        }

        Long semestreId = inscriptionRepository.findById(dto.getInscriptionId())
                .map(i -> i.getSemestre().getId()).orElse(null);
        Long moduleId = matiereRepository.findById(dto.getMatiereId())
                .map(m -> m.getModule().getId()).orElse(null);
        return "redirect:/scolarite/notes?semestreId=" + semestreId + "&moduleId=" + moduleId + "&matiereId=" + dto.getMatiereId();
    }

    @GetMapping("/notes/supprimer/{id}")
    public String supprimerNote(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            Note note = noteRepository.findById(id).orElseThrow();
            Long inscriptionId = note.getInscription().getId();
            Long semestreId = note.getInscription().getSemestre().getId();
            Long moduleId = note.getMatiere().getModule().getId();
            Long matiereId = note.getMatiere().getId();

            noteRepository.delete(note);
            evaluationService.calculerResultatsInscription(inscriptionId);
            journalService.log(auth.getName(), "SUPPRESSION_NOTE",
                    "Note supprimee pour " + note.getInscription().getEtudiant().getMatricule()
                            + " / " + note.getMatiere().getCode());
            redirect.addFlashAttribute("success", "Note supprimee avec succes !");
            return "redirect:/scolarite/notes?semestreId=" + semestreId + "&moduleId=" + moduleId + "&matiereId=" + matiereId;
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/scolarite/notes";
        }
    }

    @GetMapping("/notes/export")
    public ResponseEntity<byte[]> exportNotes(Authentication auth) {
        byte[] bytes = documentService.exporterNotesExcel(noteRepository.findAll());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=toutes_les_notes.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }


    @GetMapping("/inscriptions")
    public String pageInscriptions(@RequestParam(required = false) Long anneeId, Authentication auth, Model model) {
        model.addAttribute("etudiants", etudiantRepository.findAll());
        model.addAttribute("semestres", semestreRepository.findAll());
        model.addAttribute("annees", anneeUniversitaireRepository.findAll());
        model.addAttribute("anneeId", anneeId);
        if (anneeId != null) {
            model.addAttribute("inscriptions", inscriptionRepository.findByAnneeUniversitaireId(anneeId));
        } else {
            model.addAttribute("inscriptions", inscriptionRepository.findAll());
        }
        model.addAttribute("username", auth.getName());
        return "scolarite/inscriptions";
    }

    @PostMapping("/inscriptions/sauver")
    public String inscrireEtudiant(@RequestParam Long etudiantId, @RequestParam Long semestreId,
                                   Authentication auth, RedirectAttributes redirect) {
        try {
            Etudiant etudiant = etudiantRepository.findById(etudiantId).orElseThrow();
            Semestre semestre = semestreRepository.findById(semestreId).orElseThrow();
            AnneeUniversitaire annee = semestre.getAnneeUniversitaire();

            Optional<Inscription> existant = inscriptionRepository.findByEtudiantIdAndSemestreId(etudiantId, semestreId);
            if (existant.isPresent()) {
                redirect.addFlashAttribute("error", "L'etudiant est deja inscrit a ce semestre !");
            } else {
                Inscription ins = Inscription.builder()
                        .etudiant(etudiant)
                        .semestre(semestre)
                        .anneeUniversitaire(annee)
                        .valide(false)
                        .build();
                inscriptionRepository.save(ins);
                journalService.log(auth.getName(), "INSCRIPTION_MANUELLE",
                        "Etudiant " + etudiant.getMatricule() + " inscrit au semestre " + semestre.getCode());
                redirect.addFlashAttribute("success", "Etudiant inscrit avec succes !");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/scolarite/inscriptions";
    }

    @GetMapping("/inscriptions/supprimer/{id}")
    public String supprimerInscription(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            Inscription ins = inscriptionRepository.findById(id).orElseThrow();
            noteRepository.deleteAll(noteRepository.findByInscriptionId(id));
            inscriptionRepository.delete(ins);
            journalService.log(auth.getName(), "SUPPRESSION_INSCRIPTION",
                    "Inscription supprimee pour " + ins.getEtudiant().getMatricule() + " / " + ins.getSemestre().getCode());
            redirect.addFlashAttribute("success", "Inscription supprimee avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/scolarite/inscriptions";
    }


    @GetMapping("/reclamations")
    public String listeReclamations(Authentication auth, Model model) {
        model.addAttribute("reclamations", reclamationRepository.findAll());
        model.addAttribute("username", auth.getName());
        return "scolarite/reclamations";
    }

    @PostMapping("/reclamations/traiter/{id}")
    public String traiterReclamation(@PathVariable Long id, @RequestParam StatutReclamation statut,
                                     @RequestParam String commentaireAdmin, Authentication auth, RedirectAttributes redirect) {
        try {
            Reclamation rec = reclamationRepository.findById(id).orElseThrow();
            rec.setStatut(statut);
            rec.setCommentaireAdmin(commentaireAdmin);
            rec.setDateTraitement(LocalDateTime.now());
            reclamationRepository.save(rec);

            Notification notif = Notification.builder()
                    .destinataire(rec.getEtudiant().getUtilisateur())
                    .titre("Traitement de Reclamation")
                    .message("Votre reclamation concernant la matiere " + rec.getMatiere().getNom() +
                            " a ete traitee. Statut : " + statut.name() + ". Commentaire : " + commentaireAdmin)
                    .dateCreation(LocalDateTime.now())
                    .lue(false)
                    .build();
            notificationRepository.save(notif);

            journalService.log(auth.getName(), "TRAITEMENT_RECLAMATION",
                    "Reclamation ID " + id + " traitee avec le statut: " + statut);
            redirect.addFlashAttribute("success", "Reclamation mise a jour !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/scolarite/reclamations";
    }


    @GetMapping("/releves")
    public String pageReleves(@RequestParam(required = false) String niveau,
                              @RequestParam(required = false) Long anneeId,
                              Authentication auth, Model model) {
        List<Inscription> inscriptions = inscriptionRepository.findAll();
        if (anneeId != null) {
            inscriptions = inscriptionRepository.findByAnneeUniversitaireId(anneeId);
        }
        if (niveau != null && !niveau.isBlank()) {
            inscriptions = inscriptions.stream()
                    .filter(i -> niveau.equals(i.getEtudiant().getNiveau()))
                    .toList();
        }
        model.addAttribute("inscriptions", inscriptions);
        model.addAttribute("annees", anneeUniversitaireRepository.findAll());
        model.addAttribute("niveau", niveau);
        model.addAttribute("anneeId", anneeId);
        model.addAttribute("username", auth.getName());
        return "scolarite/releves";
    }

    @GetMapping("/releves/pdf/{inscriptionId}")
    public ResponseEntity<byte[]> telechargerRelevePDF(@PathVariable Long inscriptionId) {
        Inscription ins = inscriptionRepository.findById(inscriptionId).orElseThrow();
        evaluationService.calculerResultatsInscription(inscriptionId);
        byte[] pdfBytes = documentService.genererReleveSemestrielPDF(ins);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=releve_" + ins.getSemestre().getCode() + "_" + ins.getEtudiant().getMatricule() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/releves/imprimer/{inscriptionId}")
    public String imprimerReleve(@PathVariable Long inscriptionId, Model model) {
        Inscription ins = inscriptionRepository.findById(inscriptionId).orElseThrow();

        evaluationService.calculerResultatsInscription(inscriptionId);

        List<Module> modules;
        Etudiant etudiant = ins.getEtudiant();
        if (etudiant.getFiliereActuelle() != null && ins.getSemestre().getCode().substring(0, 2).equals(etudiant.getNiveau())) {
            modules = moduleRepository.findBySemestreIdAndFiliereId(
                    ins.getSemestre().getId(), etudiant.getFiliereActuelle().getId());
        } else {
            modules = moduleRepository.findBySemestreId(ins.getSemestre().getId());
        }

        model.addAttribute("inscription", ins);
        model.addAttribute("modules", modules);
        model.addAttribute("notesMap", buildNotesMapWithPlaceholders(ins, modules));
        model.addAttribute("mention", evaluationService.calculerMention(ins.getMoyenneSemestre()));
        return "releve_print";
    }

    private Map<Long, Note> buildNotesMapWithPlaceholders(Inscription ins, List<Module> modules) {
        Map<Long, Note> notesMap = new HashMap<>();
        for (Module mod : modules) {
            for (Matiere mat : mod.getMatieres()) {
                Optional<Note> noteOpt = noteRepository.findByInscriptionIdAndMatiereId(ins.getId(), mat.getId());

                if (noteOpt.isPresent()) {
                    notesMap.put(mat.getId(), noteOpt.get());
                } else {
                    Note placeholder = Note.builder()
                            .inscription(ins)
                            .matiere(mat)
                            .noteCC(null)
                            .noteExamen(null)
                            .noteTP(null)
                            .noteRattrapage(null)
                            .noteGenerale(null)
                            .validee(false)
                            .build();
                    notesMap.put(mat.getId(), placeholder);
                }
            }
        }
        return notesMap;
    }


    @GetMapping("/statistiques")
    public String pageStatistiques(@RequestParam(required = false) String niveau, Authentication auth, Model model) {
        List<Etudiant> etudiants = niveau != null && !niveau.isBlank()
                ? etudiantRepository.findByNiveau(niveau)
                : etudiantRepository.findAll();

        class RankItem {
            final Etudiant etudiant;
            final double moyenne;
            final int credits;
            final MentionAcademique mention;
            RankItem(Etudiant et, double moy, int cr, MentionAcademique m) {
                this.etudiant = et;
                this.moyenne = moy;
                this.credits = cr;
                this.mention = m;
            }
            public Etudiant getEtudiant() { return etudiant; }
            public double getMoyenne() { return moyenne; }
            public int getCredits() { return credits; }
            public MentionAcademique getMention() { return mention; }
        }

        List<RankItem> rankings = new ArrayList<>();
        for (Etudiant et : etudiants) {
            double moy = evaluationService.calculerMoyenneGenerale(et.getId());
            int cr = evaluationService.calculerTotalCreditsObtenus(et.getId());
            rankings.add(new RankItem(et, moy, cr, evaluationService.calculerMention(moy)));
        }
        rankings.sort((a, b) -> Double.compare(b.moyenne, a.moyenne));

        model.addAttribute("rankings", rankings);
        model.addAttribute("l1Count", etudiantRepository.findByNiveau("L1").size());
        model.addAttribute("l2Count", etudiantRepository.findByNiveau("L2").size());
        model.addAttribute("l3Count", etudiantRepository.findByNiveau("L3").size());
        model.addAttribute("niveau", niveau);
        model.addAttribute("username", auth.getName());
        return "scolarite/statistiques";
    }
}