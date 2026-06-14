package com.eduadmin.service;

import com.eduadmin.model.*;
import com.eduadmin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportExportService {

    private final EtudiantRepository etudiantRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FiliereRepository filiereRepository;
    private final MatiereRepository matiereRepository;
    private final InscriptionRepository inscriptionRepository;
    private final NoteRepository noteRepository;
    private final SemestreRepository semestreRepository;
    private final AnneeUniversitaireRepository anneeUniversitaireRepository;
    
    private final EvaluationService evaluationService;
    private final PasswordEncoder passwordEncoder;
    private final JournalService journalService;

    @Transactional
    public int importerEtudiantsExcel(MultipartFile file, String executeur) {
        int count = 0;
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            AnneeUniversitaire annee = anneeUniversitaireRepository.findByCouranteTrue()
                    .orElseThrow(() -> new IllegalStateException("Aucune annee universitaire courante"));

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                String matricule = getCellStringValue(row.getCell(0));
                String nom = getCellStringValue(row.getCell(1));
                String prenom = getCellStringValue(row.getCell(2));
                String email = getCellStringValue(row.getCell(3));
                String dateNaissanceStr = getCellStringValue(row.getCell(4));
                String telephone = getCellStringValue(row.getCell(5));
                String adresse = getCellStringValue(row.getCell(6));
                String niveau = getCellStringValue(row.getCell(7));
                String filiereCode = getCellStringValue(row.getCell(8));

                if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) continue;

                // Génération de matricule automatique si absent
                if (matricule.isEmpty()) {
                    matricule = genererMatriculeUnique();
                }

                if (!matricule.startsWith("C")) {
                    matricule = "C" + matricule;
                }

                // Vérifier si l'étudiant existe déjà
                Optional<Etudiant> etOpt = etudiantRepository.findByMatricule(matricule);
                Etudiant etudiant;
                
                if (etOpt.isPresent()) {
                    etudiant = etOpt.get();
                    etudiant.setTelephone(telephone);
                    etudiant.setAdresse(adresse);
                    etudiant.setNiveau(niveau);
                } else {
                    // Création de l'utilisateur associé
                    Utilisateur user = Utilisateur.builder()
                            .username(matricule)
                            .password(passwordEncoder.encode(matricule)) // username = password = matricule haché
                            .nom(nom)
                            .prenom(prenom)
                            .email(email)
                            .role(Role.ROLE_ETUDIANT)
                            .actif(true)
                            .build();

                    user = utilisateurRepository.save(user);

                    // Date de naissance
                    LocalDate dateNaissance = LocalDate.now().minusYears(20); // Valeur par défaut
                    if (!dateNaissanceStr.isEmpty()) {
                        try {
                            if (row.getCell(4).getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(row.getCell(4))) {
                                dateNaissance = row.getCell(4).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            } else {
                                dateNaissance = LocalDate.parse(dateNaissanceStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            }
                        } catch (Exception e) {
                            log.error("Format de date invalide pour l'etudiant {}: {}", nom, dateNaissanceStr);
                        }
                    }

                    etudiant = Etudiant.builder()
                            .utilisateur(user)
                            .matricule(matricule)
                            .dateNaissance(dateNaissance)
                            .telephone(telephone)
                            .adresse(adresse)
                            .niveau(niveau)
                            .build();
                }

                // Affecter la filière
                if (!filiereCode.isEmpty()) {
                    Optional<Filiere> filiereOpt = filiereRepository.findByCode(filiereCode);
                    filiereOpt.ifPresent(etudiant::setFiliereActuelle);
                }

                etudiantRepository.save(etudiant);

                // Inscrire automatiquement l'étudiant aux semestres correspondants à son niveau
                inscrireEtudiantNiveau(etudiant, annee);

                count++;
            }
            journalService.log(executeur, "IMPORT_EXCEL", "Importation reussie de " + count + " etudiants");
        } catch (Exception e) {
            log.error("Erreur lors de l'importation Excel des etudiants : {}", e.getMessage());
            throw new RuntimeException("Erreur de format du fichier Excel : " + e.getMessage());
        }
        return count;
    }

    @Transactional
    public int importerNotesExcel(MultipartFile file, String executeur) {
        int count = 0;
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                String matricule = getCellStringValue(row.getCell(0));
                String matiereCode = getCellStringValue(row.getCell(1));
                String noteCCStr = getCellStringValue(row.getCell(2));
                String noteTPStr = getCellStringValue(row.getCell(3));
                String noteExamenStr = getCellStringValue(row.getCell(4));
                String noteRattrapageStr = getCellStringValue(row.getCell(5));

                if (matricule.isEmpty() || matiereCode.isEmpty()) continue;

                Optional<Etudiant> etOpt = etudiantRepository.findByMatricule(matricule);
                Optional<Matiere> matOpt = matiereRepository.findByCode(matiereCode);

                if (etOpt.isPresent() && matOpt.isPresent()) {
                    Etudiant et = etOpt.get();
                    Matiere mat = matOpt.get();

                    // Trouver le semestre correspondant au module de la matière
                    Semestre semestre = mat.getModule().getSemestre();

                    // Trouver ou créer l'inscription correspondante pour le semestre
                    Inscription ins = inscriptionRepository.findByEtudiantIdAndSemestreId(et.getId(), semestre.getId())
                            .orElseGet(() -> {
                                Inscription newIns = Inscription.builder()
                                        .etudiant(et)
                                        .semestre(semestre)
                                        .anneeUniversitaire(semestre.getAnneeUniversitaire())
                                        .valide(false)
                                        .build();
                                return inscriptionRepository.save(newIns);
                            });

                    // Trouver ou créer la note
                    Note note = noteRepository.findByInscriptionIdAndMatiereId(ins.getId(), mat.getId())
                            .orElseGet(() -> Note.builder()
                                    .inscription(ins)
                                    .matiere(mat)
                                    .sessionExamen(SessionExamen.NORMAL)
                                    .validee(false)
                                    .build());

                    if (!noteCCStr.isEmpty()) note.setNoteCC(Double.parseDouble(noteCCStr));
                    if (!noteTPStr.isEmpty()) note.setNoteTP(Double.parseDouble(noteTPStr));
                    if (!noteExamenStr.isEmpty()) note.setNoteExamen(Double.parseDouble(noteExamenStr));
                    if (!noteRattrapageStr.isEmpty()) note.setNoteRattrapage(Double.parseDouble(noteRattrapageStr));

                    evaluationService.enregistrerNote(note, executeur);
                    count++;
                }
            }
            journalService.log(executeur, "IMPORT_NOTES_EXCEL", "Importation reussie de " + count + " notes");
        } catch (Exception e) {
            log.error("Erreur lors de l'importation Excel des notes : {}", e.getMessage());
            throw new RuntimeException("Erreur de format du fichier Excel : " + e.getMessage());
        }
        return count;
    }

    private void inscrireEtudiantNiveau(Etudiant etudiant, AnneeUniversitaire annee) {
        String niv = etudiant.getNiveau();
        String sA = "S1", sB = "S2";
        if ("L2".equals(niv)) {
            sA = "S3";
            sB = "S4";
        } else if ("L3".equals(niv)) {
            sA = "S5";
            sB = "S6";
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

    private String genererMatriculeUnique() {
        String matricule;
        do {
            int code = 10000 + new java.util.Random().nextInt(90000);
            matricule = "C" + code;
        } while (etudiantRepository.existsByMatricule(matricule));
        return matricule;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            }
            double val = cell.getNumericCellValue();
            if (val == (long) val) {
                return String.valueOf((long) val);
            } else {
                return String.valueOf(val);
            }
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else {
            return cell.getStringCellValue().trim();
        }
    }
}
