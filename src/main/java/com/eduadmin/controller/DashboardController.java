package com.eduadmin.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.eduadmin.model.Etudiant;
import com.eduadmin.model.JournalActivite;
import com.eduadmin.model.Note;
import com.eduadmin.model.StatutReclamation;
import com.eduadmin.repository.AnnonceRepository;
import com.eduadmin.repository.EtudiantRepository;
import com.eduadmin.repository.FiliereRepository;
import com.eduadmin.repository.JournalActiviteRepository;
import com.eduadmin.repository.MatiereRepository;
import com.eduadmin.repository.ModuleRepository;
import com.eduadmin.repository.NoteRepository;
import com.eduadmin.repository.NotificationRepository;
import com.eduadmin.repository.ReclamationRepository;
import com.eduadmin.service.EvaluationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final EtudiantRepository etudiantRepository;
    private final FiliereRepository filiereRepository;
    private final ModuleRepository moduleRepository;
    private final MatiereRepository matiereRepository;
    private final ReclamationRepository reclamationRepository;
    private final JournalActiviteRepository journalActiviteRepository;
    private final AnnonceRepository annonceRepository;
    private final NotificationRepository notificationRepository;
    private final NoteRepository noteRepository;
    private final EvaluationService evaluationService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        model.addAttribute("username", username);

        if ("ROLE_ADMIN".equals(role)) {
            model.addAttribute("countEtudiants", etudiantRepository.count());
            model.addAttribute("countFilieres", filiereRepository.count());
            model.addAttribute("countModules", moduleRepository.count());
            model.addAttribute("countMatieres", matiereRepository.count());
            model.addAttribute("countReclamationsAttente", reclamationRepository.countByStatut(StatutReclamation.EN_ATTENTE));
            
            List<JournalActivite> activites = journalActiviteRepository.findAllByOrderByDateActionDesc();
            model.addAttribute("recentActivites", activites.subList(0, Math.min(activites.size(), 8)));
            
            return "dashboard_admin";
            
        } else if ("ROLE_SCOLARITE".equals(role)) {
            model.addAttribute("countEtudiants", etudiantRepository.count());
            model.addAttribute("countReclamationsAttente", reclamationRepository.countByStatut(StatutReclamation.EN_ATTENTE));
            
            List<JournalActivite> activites = journalActiviteRepository.findAllByOrderByDateActionDesc();
            model.addAttribute("recentActivites", activites.subList(0, Math.min(activites.size(), 8)));
            
            return "dashboard_scolarite";
            
        } else if ("ROLE_ETUDIANT".equals(role)) {
            Etudiant etudiant = etudiantRepository.findByUtilisateurUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Etudiant introuvable pour l'utilisateur : " + username));
            
            model.addAttribute("etudiant", etudiant);
            model.addAttribute("moyenneGenerale", evaluationService.calculerMoyenneGenerale(etudiant.getId()));
            model.addAttribute("totalCredits", evaluationService.calculerTotalCreditsObtenus(etudiant.getId()));
            
            List<Note> notes = noteRepository.findByInscriptionEtudiantId(etudiant.getId());
            model.addAttribute("recentNotes", notes.subList(0, Math.min(notes.size(), 5)));
            
            model.addAttribute("annonces", annonceRepository.findAllByOrderByDatePublicationDesc());
            model.addAttribute("notifications", notificationRepository.findByDestinataireUsernameAndLueFalseOrderByDateCreationDesc(username));
            
            return "dashboard_etudiant";
        }

        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "login";
    }
}
