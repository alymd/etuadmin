package com.eduadmin.controller;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import com.eduadmin.dto.EtudiantFormDto;
import com.eduadmin.model.AnneeUniversitaire;
import com.eduadmin.model.Etudiant;
import com.eduadmin.model.Filiere;
import com.eduadmin.model.FormuleOrientation;
import com.eduadmin.model.Inscription;
import com.eduadmin.model.Matiere;
import com.eduadmin.model.Module;
import com.eduadmin.model.Role;
import com.eduadmin.model.Semestre;
import com.eduadmin.model.Utilisateur;
import com.eduadmin.repository.AffectationFiliereRepository;
import com.eduadmin.repository.AnneeUniversitaireRepository;
import com.eduadmin.repository.EtudiantRepository;
import com.eduadmin.repository.FiliereRepository;
import com.eduadmin.repository.FormuleOrientationRepository;
import com.eduadmin.repository.InscriptionRepository;
import com.eduadmin.repository.MatiereRepository;
import com.eduadmin.repository.ModuleRepository;
import com.eduadmin.repository.NoteRepository;
import com.eduadmin.repository.ReclamationRepository;
import com.eduadmin.repository.SemestreRepository;
import com.eduadmin.repository.UtilisateurRepository;
import com.eduadmin.service.DocumentService;
import com.eduadmin.service.EtudiantService;
import com.eduadmin.service.ImportExportService;
import com.eduadmin.service.JournalService;
import com.eduadmin.service.OrientationService;
import com.eduadmin.service.UtilisateurService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UtilisateurRepository utilisateurRepository;
    private final EtudiantRepository etudiantRepository;
    private final FiliereRepository filiereRepository;
    private final ModuleRepository moduleRepository;
    private final MatiereRepository matiereRepository;
    private final NoteRepository noteRepository;
    private final ReclamationRepository reclamationRepository;
    private final FormuleOrientationRepository formuleOrientationRepository;
    private final AnneeUniversitaireRepository anneeUniversitaireRepository;
    private final SemestreRepository semestreRepository;
    private final InscriptionRepository inscriptionRepository;
    private final AffectationFiliereRepository affectationFiliereRepository;
    
    private final UtilisateurService utilisateurService;
    private final EtudiantService etudiantService;
    private final OrientationService orientationService;
    private final ImportExportService importExportService;
    private final DocumentService documentService;
    private final JournalService journalService;
    private final PasswordEncoder passwordEncoder;

    // --- GESTION UTILISATEURS ---

    @GetMapping("/utilisateurs")
    public String listeUtilisateurs(Model model) {
        model.addAttribute("utilisateurs", utilisateurRepository.findAll());
        model.addAttribute("nouveauUser", new Utilisateur());
        model.addAttribute("roles", Role.values());
        return "admin/utilisateurs";
    }

    @PostMapping("/utilisateurs/sauver")
    public String sauverUtilisateur(@ModelAttribute("nouveauUser") Utilisateur user, 
                                    Authentication auth, RedirectAttributes redirect) {
        try {
            if (user.getId() == null) {
                utilisateurService.enregisterUtilisateur(user, passwordEncoder);
                journalService.log(auth.getName(), "CREATION_UTILISATEUR", "Nouvel utilisateur cree: " + user.getUsername());
            } else {
                Utilisateur existant = utilisateurRepository.findById(user.getId()).orElseThrow();
                existant.setNom(user.getNom());
                existant.setPrenom(user.getPrenom());
                existant.setEmail(user.getEmail());
                existant.setRole(user.getRole());
                existant.setActif(user.isActif());
                utilisateurRepository.save(existant);
                journalService.log(auth.getName(), "MODIFICATION_UTILISATEUR", "Utilisateur modifie: " + user.getUsername());
            }
            redirect.addFlashAttribute("success", "Utilisateur enregistre avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }

    @GetMapping("/utilisateurs/toggle/{id}")
    public String toggleActif(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        Utilisateur user = utilisateurRepository.findById(id).orElseThrow();
        user.setActif(!user.isActif());
        utilisateurRepository.save(user);
        journalService.log(auth.getName(), "TOGGLE_UTILISATEUR", 
            "Utilisateur " + user.getUsername() + " mis a " + (user.isActif() ? "ACTIF" : "INACTIF"));
        redirect.addFlashAttribute("success", "Statut utilisateur mis a jour !");
        return "redirect:/admin/utilisateurs";
    }

    // --- GESTION ETUDIANTS ---

    @GetMapping("/etudiants")
    public String listeEtudiants(@RequestParam(required = false) String nom,
                                 @RequestParam(required = false) String matricule,
                                 @RequestParam(required = false) Long filiereId,
                                 @RequestParam(required = false) String niveau,
                                 @RequestParam(required = false) Long anneeId,
                                 Model model) {
        model.addAttribute("etudiants", etudiantService.rechercher(nom, matricule, filiereId, niveau, anneeId));
        model.addAttribute("filieres", filiereRepository.findAll());
        model.addAttribute("annees", anneeUniversitaireRepository.findAll());
        model.addAttribute("nom", nom);
        model.addAttribute("matricule", matricule);
        model.addAttribute("filiereId", filiereId);
        model.addAttribute("niveau", niveau);
        model.addAttribute("anneeId", anneeId);
        return "admin/etudiants";
    }

    @GetMapping("/etudiants/supprimer/{id}")
    public String supprimerEtudiant(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            Etudiant et = etudiantRepository.findById(id).orElseThrow();
            etudiantService.supprimerEtudiant(id);
            journalService.log(auth.getName(), "SUPPRESSION_ETUDIANT", "Etudiant supprime: " + et.getMatricule());
            redirect.addFlashAttribute("success", "Etudiant supprime avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/etudiants";
    }

    @GetMapping("/etudiants/nouveau")
    public String nouveauEtudiantForm(Model model) {
        model.addAttribute("etudiantForm", new EtudiantFormDto());
        model.addAttribute("filieres", filiereRepository.findAll());
        model.addAttribute("niveaux", new String[]{"L1", "L2", "L3"});
        return "admin/etudiant_form";
    }

    @GetMapping("/etudiants/modifier/{id}")
    public String modifierEtudiantForm(@PathVariable Long id, Model model) {
        Etudiant et = etudiantRepository.findById(id).orElseThrow();
        EtudiantFormDto dto = EtudiantFormDto.builder()
                .id(et.getId())
                .matricule(et.getMatricule())
                .nom(et.getUtilisateur().getNom())
                .prenom(et.getUtilisateur().getPrenom())
                .email(et.getUtilisateur().getEmail())
                .dateNaissance(et.getDateNaissance())
                .telephone(et.getTelephone())
                .adresse(et.getAdresse())
                .niveau(et.getNiveau())
                .filiereId(et.getFiliereActuelle() != null ? et.getFiliereActuelle().getId() : null)
                .build();

        model.addAttribute("etudiantForm", dto);
        model.addAttribute("filieres", filiereRepository.findAll());
        model.addAttribute("niveaux", new String[]{"L1", "L2", "L3"});
        return "admin/etudiant_form";
    }

    @PostMapping("/etudiants/sauver")
    public String sauverEtudiant(@Valid @ModelAttribute("etudiantForm") EtudiantFormDto dto, 
                                 BindingResult result, Authentication auth, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("filieres", filiereRepository.findAll());
            model.addAttribute("niveaux", new String[]{"L1", "L2", "L3"});
            return "admin/etudiant_form";
        }

        try {
            AnneeUniversitaire annee = anneeUniversitaireRepository.findByCouranteTrue()
                    .orElseThrow(() -> new IllegalStateException("Aucune annee universitaire active"));

            if (dto.getId() == null) {
                // Créer l'utilisateur
                Utilisateur user = Utilisateur.builder()
                        .username(dto.getMatricule())
                        .password(passwordEncoder.encode(dto.getMatricule()))
                        .nom(dto.getNom())
                        .prenom(dto.getPrenom())
                        .email(dto.getEmail())
                        .role(Role.ROLE_ETUDIANT)
                        .actif(true)
                        .build();
                utilisateurRepository.save(user);

                // Créer l'étudiant
                Etudiant et = Etudiant.builder()
                        .utilisateur(user)
                        .matricule(dto.getMatricule())
                        .dateNaissance(dto.getDateNaissance())
                        .telephone(dto.getTelephone())
                        .adresse(dto.getAdresse())
                        .niveau(dto.getNiveau())
                        .build();

                if (dto.getFiliereId() != null) {
                    et.setFiliereActuelle(filiereRepository.findById(dto.getFiliereId()).orElse(null));
                }

                etudiantRepository.save(et);
                
                // Inscrire aux semestres selon le niveau
                inscrireEtudiantNiveau(et, annee);
                
                journalService.log(auth.getName(), "CREATION_ETUDIANT", "Etudiant cree avec le matricule: " + et.getMatricule());
            } else {
                Etudiant et = etudiantRepository.findById(dto.getId()).orElseThrow();
                Utilisateur user = et.getUtilisateur();
                
                user.setNom(dto.getNom());
                user.setPrenom(dto.getPrenom());
                user.setEmail(dto.getEmail());
                utilisateurRepository.save(user);

                et.setDateNaissance(dto.getDateNaissance());
                et.setTelephone(dto.getTelephone());
                et.setAdresse(dto.getAdresse());
                et.setNiveau(dto.getNiveau());

                if (dto.getFiliereId() != null) {
                    et.setFiliereActuelle(filiereRepository.findById(dto.getFiliereId()).orElse(null));
                } else {
                    et.setFiliereActuelle(null);
                }

                etudiantRepository.save(et);
                inscrireEtudiantNiveau(et, annee);
                
                journalService.log(auth.getName(), "MODIFICATION_ETUDIANT", "Etudiant modifie: " + et.getMatricule());
            }
            redirect.addFlashAttribute("success", "Etudiant enregistre avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/admin/etudiants/nouveau";
        }
        return "redirect:/admin/etudiants";
    }

    private void inscrireEtudiantNiveau(Etudiant etudiant, AnneeUniversitaire annee) {
        String sA = "S1", sB = "S2";
        if ("L2".equals(etudiant.getNiveau())) {
            sA = "S3"; sB = "S4";
        } else if ("L3".equals(etudiant.getNiveau())) {
            sA = "S5"; sB = "S6";
        }
        inscrireDansSemestre(etudiant, sA, annee);
        inscrireDansSemestre(etudiant, sB, annee);
    }

    private void inscrireDansSemestre(Etudiant etudiant, String codeSemestre, AnneeUniversitaire annee) {
        semestreRepository.findByCodeAndAnneeUniversitaireLibelle(codeSemestre, String.valueOf(annee.getId())).ifPresent(sem -> {
            Optional<Inscription> insOpt = inscriptionRepository.findByEtudiantIdAndSemestreId(etudiant.getId(), sem.getId());
            if (insOpt.isEmpty()) {
                Inscription ins = Inscription.builder()
                        .etudiant(etudiant)
                        .semestre(sem)
                        .anneeUniversitaire(annee)
                        .valide(false)
                        .build();
                inscriptionRepository.save(ins);
            }
        });
    }

    @PostMapping("/etudiants/import")
    public String importerEtudiants(@RequestParam("file") MultipartFile file, Authentication auth, RedirectAttributes redirect) {
        try {
            int count = importExportService.importerEtudiantsExcel(file, auth.getName());
            redirect.addFlashAttribute("success", count + " etudiants importes avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/etudiants";
    }

    @GetMapping("/etudiants/export")
    public ResponseEntity<byte[]> exportEtudiants() {
        byte[] bytes = documentService.exporterEtudiantsExcel(etudiantRepository.findAll());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=etudiants.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // --- GESTION FILIERES ---

    @GetMapping("/filieres")
    public String listeFilieres(Model model) {
        model.addAttribute("filieres", filiereRepository.findAll());
        model.addAttribute("nouvelleFiliere", new Filiere());
        return "admin/filieres";
    }

    @PostMapping("/filieres/sauver")
    public String sauverFiliere(@ModelAttribute("nouvelleFiliere") Filiere filiere, Authentication auth, RedirectAttributes redirect) {
        try {
            filiereRepository.save(filiere);
            journalService.log(auth.getName(), "SAUVE_FILIERE", "Filiere enregistree: " + filiere.getCode());
            redirect.addFlashAttribute("success", "Filiere enregistree avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/filieres";
    }

    @GetMapping("/filieres/supprimer/{id}")
    public String supprimerFiliere(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            Filiere filiere = filiereRepository.findById(id).orElseThrow();
            if (!etudiantRepository.findByFiliereActuelleId(id).isEmpty()) {
                throw new IllegalStateException("Impossible de supprimer : des etudiants sont rattaches a cette filiere.");
            }
            if (!moduleRepository.findByFiliereId(id).isEmpty()) {
                throw new IllegalStateException("Impossible de supprimer : des modules sont rattaches a cette filiere.");
            }
            formuleOrientationRepository.findByFiliereId(id).ifPresent(formuleOrientationRepository::delete);
            filiereRepository.delete(filiere);
            journalService.log(auth.getName(), "SUPPRESSION_FILIERE", "Filiere supprimee: " + filiere.getCode());
            redirect.addFlashAttribute("success", "Filiere supprimee avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/filieres";
    }

    // --- GESTION MODULES & MATIERES ---

    @GetMapping("/modules")
    public String listeModulesEtMatieres(Model model) {
        model.addAttribute("modules", moduleRepository.findAll());
        model.addAttribute("matieres", matiereRepository.findAll());
        model.addAttribute("semestres", semestreRepository.findAll());
        model.addAttribute("filieres", filiereRepository.findAll());
        
        model.addAttribute("nouveauModule", new Module());
        model.addAttribute("nouvelleMatiere", new Matiere());
        
        return "admin/modules";
    }

    @PostMapping("/modules/sauver")
    public String sauverModule(@ModelAttribute("nouveauModule") Module mod, 
                               @RequestParam("semestre") Long semestreId,
                               @RequestParam(value = "filiere", required = false) Long filiereId,
                               Authentication auth, RedirectAttributes redirect) {
        try {
            Semestre semestre = semestreRepository.findById(semestreId)
                    .orElseThrow(() -> new IllegalArgumentException("Semestre non trouve"));
            Filiere filiere = null;
            if (filiereId != null) {
                filiere = filiereRepository.findById(filiereId)
                        .orElseThrow(() -> new IllegalArgumentException("Filiere non trouvee"));
            }

            if (mod.getId() != null) {
                Module existant = moduleRepository.findById(mod.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Module non trouve"));
                existant.setCode(mod.getCode());
                existant.setNom(mod.getNom());
                existant.setSemestre(semestre);
                existant.setFiliere(filiere);
                moduleRepository.save(existant);
                journalService.log(auth.getName(), "MODIF_MODULE", "Module modifie: " + existant.getCode());
                redirect.addFlashAttribute("success", "Module modifie avec succes !");
            } else {
                mod.setSemestre(semestre);
                mod.setFiliere(filiere);
                moduleRepository.save(mod);
                journalService.log(auth.getName(), "SAUVE_MODULE", "Module enregistre: " + mod.getCode());
                redirect.addFlashAttribute("success", "Module enregistre avec succes !");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/modules";
    }

    @GetMapping("/modules/supprimer/{id}")
    public String supprimerModule(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            Module mod = moduleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Module non trouve"));
            for (Matiere mat : matiereRepository.findByModuleId(id)) {
                verifierMatiereSupprimable(mat);
            }
            moduleRepository.delete(mod);
            journalService.log(auth.getName(), "SUPPRESSION_MODULE", "Module supprime: " + mod.getCode());
            redirect.addFlashAttribute("success", "Module supprime avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/modules";
    }

    @PostMapping("/matieres/sauver")
    public String sauverMatiere(@ModelAttribute("nouvelleMatiere") Matiere mat, 
                                @RequestParam("module") Long moduleId,
                                Authentication auth, RedirectAttributes redirect) {
        try {
            Module module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new IllegalArgumentException("Module non trouve"));

            if (mat.getId() != null) {
                Matiere existant = matiereRepository.findById(mat.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Matiere non trouvee"));
                existant.setCode(mat.getCode());
                existant.setNom(mat.getNom());
                existant.setCoefficient(mat.getCoefficient());
                existant.setCredits(mat.getCredits());
                existant.setPresenceTP(mat.isPresenceTP());
                existant.setDescription(mat.getDescription());
                existant.setModule(module);
                matiereRepository.save(existant);
                journalService.log(auth.getName(), "MODIF_MATIERE", "Matiere modifiee: " + existant.getCode());
                redirect.addFlashAttribute("success", "Matiere modifiee avec succes !");
            } else {
                mat.setModule(module);
                matiereRepository.save(mat);
                journalService.log(auth.getName(), "SAUVE_MATIERE", "Matiere enregistree: " + mat.getCode());
                redirect.addFlashAttribute("success", "Matiere enregistree avec succes !");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/modules";
    }

    @GetMapping("/matieres/supprimer/{id}")
    public String supprimerMatiere(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            Matiere mat = matiereRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Matiere non trouvee"));
            verifierMatiereSupprimable(mat);
            matiereRepository.delete(mat);
            journalService.log(auth.getName(), "SUPPRESSION_MATIERE", "Matiere supprimee: " + mat.getCode());
            redirect.addFlashAttribute("success", "Matiere supprimee avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/modules";
    }

    private void verifierMatiereSupprimable(Matiere mat) {
        if (!noteRepository.findByMatiereId(mat.getId()).isEmpty()) {
            throw new IllegalStateException(
                    "Impossible de supprimer la matiere " + mat.getCode() + " : des notes y sont rattachees.");
        }
        if (reclamationRepository.countByMatiereId(mat.getId()) > 0) {
            throw new IllegalStateException(
                    "Impossible de supprimer la matiere " + mat.getCode() + " : des reclamations y sont rattachees.");
        }
    }

    // --- REGLES ACADEMIQUES & FORMULES & ORIENTATION ---

    @GetMapping("/orientation")
    public String pageOrientation(Model model) {
        model.addAttribute("formules", formuleOrientationRepository.findAll());
        model.addAttribute("filieres", filiereRepository.findAll());
        model.addAttribute("affectations", affectationFiliereRepository.findAll());
        model.addAttribute("l1Count", etudiantRepository.findByNiveau("L1").size());
        return "admin/orientation";
    }

    @PostMapping("/formules/sauver")
    public String sauverFormule(@RequestParam Long filiereId, @RequestParam String formule, Authentication auth, RedirectAttributes redirect) {
        Filiere filiere = filiereRepository.findById(filiereId).orElseThrow();
        FormuleOrientation fo = formuleOrientationRepository.findByFiliereId(filiereId)
                .orElse(FormuleOrientation.builder().filiere(filiere).build());
        fo.setFormule(formule);
        formuleOrientationRepository.save(fo);
        journalService.log(auth.getName(), "MODIF_FORMULE", "Formule d'orientation modifiee pour: " + filiere.getCode());
        redirect.addFlashAttribute("success", "Formule d'orientation mise a jour !");
        return "redirect:/admin/orientation";
    }

    @PostMapping("/orientation/calculer")
    public String lancerOrientation(Authentication auth, RedirectAttributes redirect) {
        try {
            orientationService.affecterEtudiantsAutomatiquement(auth.getName());
            redirect.addFlashAttribute("success", "Affectation automatique des filieres executee avec succes !");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Erreur d'affectation : " + e.getMessage());
        }
        return "redirect:/admin/orientation";
    }

    // --- AUDIT LOGS ---

    @GetMapping("/journal")
    public String voirJournal(Model model) {
        model.addAttribute("logs", journalService.getToutesActivites());
        return "admin/journal";
    }
}
