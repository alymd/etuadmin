package com.eduadmin.config;

import com.eduadmin.model.*;
import com.eduadmin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final EtudiantRepository etudiantRepository;
    private final FiliereRepository filiereRepository;
    private final AnneeUniversitaireRepository anneeUniversitaireRepository;
    private final SemestreRepository semestreRepository;
    private final FormuleOrientationRepository formuleOrientationRepository;
    private final AnnonceRepository annonceRepository;
    
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (utilisateurRepository.count() > 0) {
            log.info("La base de donnees contient deja des donnees, saut du seeder.");
            return;
        }

        log.info("Initialisation du jeu de donnees de demonstration...");


        Utilisateur admin = Utilisateur.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .nom("Dupont")
                .prenom("Jean")
                .email("admin@eduadmin.fr")
                .role(Role.ROLE_ADMIN)
                .actif(true)
                .build();
        utilisateurRepository.save(admin);

        Utilisateur scolarite = Utilisateur.builder()
                .username("scol")
                .password(passwordEncoder.encode("scol"))
                .nom("Martin")
                .prenom("Alice")
                .email("scolarite@eduadmin.fr")
                .role(Role.ROLE_SCOLARITE)
                .actif(true)
                .build();
        utilisateurRepository.save(scolarite);


        AnneeUniversitaire annee = AnneeUniversitaire.builder()
                .libelle("2025-2026")
                .courante(true)
                .build();
        annee = anneeUniversitaireRepository.save(annee);


        List<Semestre> semestres = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Semestre sem = Semestre.builder()
                    .code("S" + i)
                    .dateDebut(LocalDate.of(2025, 9, 1).plusMonths((i - 1) * 5))
                    .dateFin(LocalDate.of(2026, 1, 31).plusMonths((i - 1) * 5))
                    .anneeUniversitaire(annee)
                    .build();
            semestres.add(semestreRepository.save(sem));
        }


        Filiere mpi = filiereRepository.save(Filiere.builder().code("MPI").nom("Mathematiques-Physique-Informatique").capaciteMax(100).build());
        Filiere bg = filiereRepository.save(Filiere.builder().code("BG").nom("Biologie-Geologie").capaciteMax(100).build());
        Filiere pc = filiereRepository.save(Filiere.builder().code("PC").nom("Physique-Chimie").capaciteMax(100).build());

        Filiere miage = filiereRepository.save(Filiere.builder().code("MIAGE").nom("Methodes Informatiques Appliquees a la Gestion").capaciteMax(2).build()); // Petite capacité pour tester saturation
        Filiere da2i = filiereRepository.save(Filiere.builder().code("DA2I").nom("Developpement d Applications Internet et Mobile").capaciteMax(10).build());
        Filiere pf = filiereRepository.save(Filiere.builder().code("PF").nom("Parcours Fondamental").capaciteMax(10).build());
        Filiere eea = filiereRepository.save(Filiere.builder().code("EEA").nom("Electronique-Electrotechnique-Automatique").capaciteMax(10).build());
        Filiere chimie = filiereRepository.save(Filiere.builder().code("Chimie").nom("Sciences Chimiques").capaciteMax(10).build());
        Filiere bio = filiereRepository.save(Filiere.builder().code("BIO").nom("Biologie").capaciteMax(10).build());

        formuleOrientationRepository.save(FormuleOrientation.builder().filiere(miage).formule("(MO1S1 + MO1S2 + 2*MO2S1 + 2*MO2S2 + MGS1 + MGS2) / 8").build());
        formuleOrientationRepository.save(FormuleOrientation.builder().filiere(da2i).formule("(MO1S1 + MO1S2 + 2*MO2S1 + 2*MO2S2 + MGS1 + MGS2) / 8").build());
        formuleOrientationRepository.save(FormuleOrientation.builder().filiere(pf).formule("(MO1S1 + MO1S2 + 2*MO2S1 + 2*MO2S2 + MGS1 + MGS2) / 8").build());
        formuleOrientationRepository.save(FormuleOrientation.builder().filiere(eea).formule("(MO1S1 + MO1S2 + 2*MO2S1 + 2*MO2S2 + MGS1 + MGS2) / 8").build());



        annonceRepository.save(Annonce.builder()
                .titre("Ouverture des voeux d'orientation")
                .contenu("Les etudiants de premiere annee (L1) peuvent desormais formuler leurs voeux pour le choix de leur filiere de L2 depuis leur espace personnel. La date limite de soumission est fixee a la fin du semestre.")
                .datePublication(LocalDateTime.now())
                .auteur(admin)
                .build());

        log.info("Jeu de donnees de demonstration initialise avec succes !");
    }

    private void creerEtudiantDemo(String matricule, String nom, String prenom, String email,
                                  AnneeUniversitaire annee, Filiere parcoursL1) {
        
        Utilisateur u = Utilisateur.builder()
                .username(matricule)
                .password(passwordEncoder.encode(matricule))
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .role(Role.ROLE_ETUDIANT)
                .actif(true)
                .build();
        u = utilisateurRepository.save(u);

        Etudiant et = Etudiant.builder()
                .utilisateur(u)
                .matricule(matricule)
                .dateNaissance(LocalDate.of(2005, 5, 15))
                .telephone("43080280")
                .adresse("Tvz, Nkc")
                .niveau("L1")
                .filiereActuelle(parcoursL1)
                .build();
        et = etudiantRepository.save(et);
    }
}
