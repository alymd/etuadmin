package com.eduadmin.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.eduadmin.dto.ReclamationDto;
import com.eduadmin.model.AnneeUniversitaire;
import com.eduadmin.model.ChoixFiliere;
import com.eduadmin.model.Etudiant;
import com.eduadmin.model.Filiere;
import com.eduadmin.model.Inscription;
import com.eduadmin.model.Matiere;
import com.eduadmin.model.MentionAcademique;
import com.eduadmin.model.Module;
import com.eduadmin.model.Note;
import com.eduadmin.model.Notification;
import com.eduadmin.model.Reclamation;
import com.eduadmin.model.Role;
import com.eduadmin.model.StatutReclamation;
import com.eduadmin.repository.AnneeUniversitaireRepository;
import com.eduadmin.repository.ChoixFiliereRepository;
import com.eduadmin.repository.EtudiantRepository;
import com.eduadmin.repository.FiliereRepository;
import com.eduadmin.repository.InscriptionRepository;
import com.eduadmin.repository.MatiereRepository;
import com.eduadmin.repository.ModuleRepository;
import com.eduadmin.repository.NoteRepository;
import com.eduadmin.repository.NotificationRepository;
import com.eduadmin.repository.ReclamationRepository;
import com.eduadmin.repository.UtilisateurRepository;
import com.eduadmin.service.DocumentService;
import com.eduadmin.service.EvaluationService;
import com.eduadmin.service.JournalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/etudiant")
@RequiredArgsConstructor
@Slf4j
public class EtudiantController {

    private final EtudiantRepository etudiantRepository;
    private final InscriptionRepository inscriptionRepository;
    private final NoteRepository noteRepository;
    private final FiliereRepository filiereRepository;
    private final ChoixFiliereRepository choixFiliereRepository;
    private final ReclamationRepository reclamationRepository;
    private final MatiereRepository matiereRepository;
    private final ModuleRepository moduleRepository;
    private final NotificationRepository notificationRepository;
    private final AnneeUniversitaireRepository anneeUniversitaireRepository;
    private final UtilisateurRepository utilisateurRepository;

    private final EvaluationService evaluationService;
    private final DocumentService documentService;
    private final JournalService journalService;



    @GetMapping("/profil")
    public String profil(Authentication auth, Model model) {
        Etudiant et = etudiantRepository.findByUtilisateurUsername(auth.getName()).orElseThrow();
        model.addAttribute("etudiant", et);
        model.addAttribute("username", auth.getName());
        return "etudiant/profil";
    }



    @GetMapping("/notes")
    public String voirNotes(Authentication auth, Model model) {
        Etudiant et = etudiantRepository.findByUtilisateurUsername(auth.getName()).orElseThrow();
        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(et.getId());

        for (Inscription ins : inscriptions) {
            evaluationService.calculerResultatsInscription(ins.getId());
        }

        Map<Inscription, Map<Module, List<Note>>> notesParInscription = new LinkedHashMap<>();

        for (Inscription ins : inscriptions) {
            Map<Module, List<Note>> parModule = new LinkedHashMap<>();

            List<Module> modules;

            if (ins.getSemestre() != null && et.getFiliereActuelle() != null &&
                    ins.getSemestre().getCode().substring(0,2).equals(et.getNiveau())) {
                modules = moduleRepository.findBySemestreIdAndFiliereId(ins.getSemestre().getId(), et.getFiliereActuelle().getId());
            } else {
                // For past semesters (e.g. L1 classes when the student is now L2), look up by Semestre base ID directly
                modules = moduleRepository.findBySemestreId(ins.getSemestre().getId());
            }

            for (Module mod : modules) {
                List<Note> notesDuModule = new ArrayList<>();

                for (Matiere mat : mod.getMatieres()) {
                    Optional<Note> noteOpt = noteRepository.findByInscriptionIdAndMatiereId(ins.getId(), mat.getId());

                    if (noteOpt.isPresent()) {
                        notesDuModule.add(noteOpt.get());
                    } else {
                        // Safe UI placeholder mapping loop step
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
                        notesDuModule.add(placeholder);
                    }
                }
                parModule.put(mod, notesDuModule);
            }
            notesParInscription.put(ins, parModule);
        }

        model.addAttribute("etudiant", et);
        model.addAttribute("notesParInscription", notesParInscription);
        model.addAttribute("moyenneGenerale", evaluationService.calculerMoyenneGenerale(et.getId()));
        model.addAttribute("mentionGenerale", evaluationService.calculerMentionGenerale(et.getId()));
        model.addAttribute("totalCredits", evaluationService.calculerTotalCreditsObtenus(et.getId()));

        Map<Inscription, MentionAcademique> mentionsParInscription = new LinkedHashMap<>();
        for (Inscription ins : inscriptions) {
            mentionsParInscription.put(ins, evaluationService.calculerMention(ins.getMoyenneSemestre()));
        }
        model.addAttribute("mentionsParInscription", mentionsParInscription);
        model.addAttribute("username", auth.getName());

        return "etudiant/notes";
    }

    @GetMapping("/notes/pdf/{inscriptionId}")
    public ResponseEntity<byte[]> telechargerNotesPDF(@PathVariable Long inscriptionId, Authentication auth) {
        Inscription ins = inscriptionRepository.findById(inscriptionId).orElseThrow();

        if (!ins.getEtudiant().getUtilisateur().getUsername().equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdfBytes = documentService.genererReleveSemestrielPDF(ins);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=notes_" + ins.getSemestre().getCode() + "_" + ins.getEtudiant().getMatricule() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/releves/pdf/{inscriptionId}")
    public ResponseEntity<byte[]> telechargerPDF(@PathVariable Long inscriptionId, Authentication auth) {
        Inscription ins = inscriptionRepository.findById(inscriptionId).orElseThrow();

        if (!ins.getEtudiant().getUtilisateur().getUsername().equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdfBytes = documentService.genererReleveSemestrielPDF(ins);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=releve_" + ins.getSemestre().getCode() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }


    @GetMapping("/reclamations")
    public String mesReclamations(Authentication auth, Model model) {
        Etudiant et = etudiantRepository.findByUtilisateurUsername(auth.getName()).orElseThrow();
        List<Reclamation> reclamations = reclamationRepository.findByEtudiantIdOrderByDateCreationDesc(et.getId());

        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(et.getId());
        List<Matiere> matieres = new ArrayList<>();
        for (Inscription ins : inscriptions) {
            List<Note> notes = noteRepository.findByInscriptionId(ins.getId());
            for (Note n : notes) {
                matieres.add(n.getMatiere());
            }
        }

        model.addAttribute("reclamations", reclamations);
        model.addAttribute("matieres", matieres);
        model.addAttribute("reclamationForm", new ReclamationDto());
        model.addAttribute("username", auth.getName());
        return "etudiant/reclamations";
    }

    @PostMapping("/reclamations/sauver")
    public String sauverReclamation(@Valid @ModelAttribute("reclamationForm") ReclamationDto dto,
                                    BindingResult result, Authentication auth, RedirectAttributes redirect) {
        Etudiant et = etudiantRepository.findByUtilisateurUsername(auth.getName()).orElseThrow();

        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", "Veuillez remplir correctement tous les champs de la reclamation");
            return "redirect:/etudiant/reclamations";
        }

        Matiere mat = matiereRepository.findById(dto.getMatiereId()).orElseThrow();

        Reclamation rec = Reclamation.builder()
                .etudiant(et)
                .matiere(mat)
                .noteConcernee(dto.getNoteConcernee())
                .objet(dto.getObjet())
                .description(dto.getDescription())
                .dateCreation(LocalDateTime.now())
                .statut(StatutReclamation.EN_ATTENTE)
                .build();

        MultipartFile file = dto.getPieceJointe();
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path uploadPath = Paths.get("src/main/resources/static/uploads");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                rec.setPieceJointeNom(fileName);
            } catch (IOException e) {
                log.error("Erreur de sauvegarde de piece jointe : {}", e.getMessage());
                redirect.addFlashAttribute("error", "Erreur lors du transfert de la piece jointe.");
                return "redirect:/etudiant/reclamations";
            }
        }

        reclamationRepository.save(rec);

        // Notify all scolarite staff about the new reclamation
        utilisateurRepository.findByRole(Role.ROLE_SCOLARITE).forEach(scolariteUser -> {
            Notification notif = Notification.builder()
                    .destinataire(scolariteUser)
                    .titre("Nouvelle réclamation")
                    .message("L'étudiant " + et.getUtilisateur().getPrenom() + " " + et.getUtilisateur().getNom()
                            + " a déposé une réclamation pour la matière " + mat.getNom() + ".")
                    .dateCreation(LocalDateTime.now())
                    .lue(false)
                    .build();
            notificationRepository.save(notif);
        });

        journalService.log(auth.getName(), "RECLAMATION_DEPOSEE", "Reclamation deposee pour la matiere " + mat.getCode());
        redirect.addFlashAttribute("success", "Votre reclamation a ete deposee avec succes !");

        return "redirect:/etudiant/reclamations";
    }

    @GetMapping("/releves/imprimer/{inscriptionId}")
    public String imprimerReleve(@PathVariable Long inscriptionId, Authentication auth, Model model) {
        Inscription ins = inscriptionRepository.findById(inscriptionId).orElseThrow();
        if (!ins.getEtudiant().getUtilisateur().getUsername().equals(auth.getName())) {
            return "redirect:/etudiant/notes";
        }

        evaluationService.calculerResultatsInscription(inscriptionId);

        List<Module> modules;
        if (ins.getEtudiant().getFiliereActuelle() != null &&
                ins.getSemestre().getCode().substring(0, 2).equals(ins.getEtudiant().getNiveau())) {
            modules = moduleRepository.findBySemestreIdAndFiliereId(
                    ins.getSemestre().getId(), ins.getEtudiant().getFiliereActuelle().getId());
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


    @GetMapping("/choix")
    public String pageChoix(Authentication auth, Model model) {
        Etudiant et = etudiantRepository.findByUtilisateurUsername(auth.getName()).orElseThrow();

        if (!"L1".equals(et.getNiveau())) {
            model.addAttribute("nonL1", true);
            return "etudiant/choix";
        }

        model.addAttribute("nonL1", false);

        AnneeUniversitaire annee = anneeUniversitaireRepository.findByCouranteTrue().orElseThrow();

        List<ChoixFiliere> choixExistants = choixFiliereRepository.findByEtudiantIdAndAnneeUniversitaireIdOrderByOrdrePreferenceAsc(et.getId(), annee.getId());

        List<Filiere> filieresL2 = filiereRepository.findAll().stream()
                .filter(f -> !f.getCode().equals("MPI") && !f.getCode().equals("BG") && !f.getCode().equals("PC"))
                .toList();

        model.addAttribute("filieres", filieresL2);
        model.addAttribute("choixExistants", choixExistants);
        model.addAttribute("username", auth.getName());

        return "etudiant/choix";
    }

    @PostMapping("/choix/sauver")
    public String sauverChoix(@RequestParam Long choix1, @RequestParam Long choix2,
                              @RequestParam Long choix3, @RequestParam Long choix4,
                              Authentication auth, RedirectAttributes redirect) {

        Set<Long> uniques = new HashSet<>(Arrays.asList(choix1, choix2, choix3, choix4));
        if (uniques.size() < 4) {
            redirect.addFlashAttribute("error", "Vous devez choisir 4 filieres distinctes !");
            return "redirect:/etudiant/choix";
        }

        Etudiant et = etudiantRepository.findByUtilisateurUsername(auth.getName()).orElseThrow();
        AnneeUniversitaire annee = anneeUniversitaireRepository.findByCouranteTrue().orElseThrow();

        List<ChoixFiliere> anciens = choixFiliereRepository.findByEtudiantIdAndAnneeUniversitaireIdOrderByOrdrePreferenceAsc(et.getId(), annee.getId());
        choixFiliereRepository.deleteAll(anciens);

        Long[] choixIds = new Long[]{choix1, choix2, choix3, choix4};
        for (int i = 0; i < 4; i++) {
            Filiere f = filiereRepository.findById(choixIds[i]).orElseThrow();
            ChoixFiliere cf = ChoixFiliere.builder()
                    .etudiant(et)
                    .filiere(f)
                    .ordrePreference(i + 1)
                    .anneeUniversitaire(annee)
                    .build();
            choixFiliereRepository.save(cf);
        }

        journalService.log(auth.getName(), "CHOIX_FILIERES_SAUVE", "Formulation de 4 voeux d'orientation enregistree.");
        redirect.addFlashAttribute("success", "Vos voeux d'orientation ont ete enregistres !");

        return "redirect:/etudiant/choix";
    }


    @GetMapping("/notifications/marquer-lue/{id}")
    public String marquerLue(@PathVariable Long id, Authentication auth) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getDestinataire().getUsername().equals(auth.getName())) {
                n.setLue(true);
                notificationRepository.save(n);
            }
        });
        return "redirect:/dashboard";
    }
}