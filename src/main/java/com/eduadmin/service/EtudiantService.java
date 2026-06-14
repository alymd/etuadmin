package com.eduadmin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eduadmin.model.Etudiant;
import com.eduadmin.repository.AffectationFiliereRepository;
import com.eduadmin.repository.ChoixFiliereRepository;
import com.eduadmin.repository.EtudiantRepository;
import com.eduadmin.repository.HistoriqueParcoursRepository;
import com.eduadmin.repository.InscriptionRepository;
import com.eduadmin.repository.NoteRepository;
import com.eduadmin.repository.NotificationRepository;
import com.eduadmin.repository.ReclamationRepository;
import com.eduadmin.repository.ReleveNoteRepository;
import com.eduadmin.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EtudiantService {

    private final EtudiantRepository etudiantRepository;
    private final InscriptionRepository inscriptionRepository;
    private final NoteRepository noteRepository;
    private final ReclamationRepository reclamationRepository;
    private final ChoixFiliereRepository choixFiliereRepository;
    private final AffectationFiliereRepository affectationFiliereRepository;
    private final HistoriqueParcoursRepository historiqueParcoursRepository;
    private final ReleveNoteRepository releveNoteRepository;
    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public List<Etudiant> rechercher(String nom, String matricule, Long filiereId, String niveau, Long anneeId) {
        String nomFilter = StringUtils.hasText(nom) ? nom.trim() : null;
        String matriculeFilter = StringUtils.hasText(matricule) ? matricule.trim() : null;
        String niveauFilter = StringUtils.hasText(niveau) ? niveau.trim() : null;
        return etudiantRepository.search(nomFilter, matriculeFilter, filiereId, niveauFilter, anneeId);
    }

    @Transactional
    public void supprimerEtudiant(Long id) {
        Etudiant etudiant = etudiantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Etudiant introuvable"));

        reclamationRepository.deleteAll(
                reclamationRepository.findByEtudiantIdOrderByDateCreationDesc(id));
        choixFiliereRepository.deleteAll(choixFiliereRepository.findByEtudiantId(id));
        affectationFiliereRepository.deleteAll(affectationFiliereRepository.findByEtudiantId(id));
        historiqueParcoursRepository.deleteAll(historiqueParcoursRepository.findByEtudiantId(id));
        releveNoteRepository.deleteAll(releveNoteRepository.findByEtudiantId(id));

        inscriptionRepository.findByEtudiantId(id).forEach(ins -> {
            noteRepository.deleteAll(noteRepository.findByInscriptionId(ins.getId()));
            inscriptionRepository.delete(ins);
        });

        var utilisateur = etudiant.getUtilisateur();
        notificationRepository.deleteAll(
                notificationRepository.findByDestinataireIdOrderByDateCreationDesc(utilisateur.getId()));
        etudiantRepository.delete(etudiant);
        utilisateurRepository.delete(utilisateur);
    }
}
