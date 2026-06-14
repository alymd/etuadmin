package com.eduadmin.repository;

import com.eduadmin.model.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
    List<Inscription> findByEtudiantId(Long etudiantId);
    List<Inscription> findByEtudiantMatricule(String matricule);
    Optional<Inscription> findByEtudiantIdAndSemestreId(Long etudiantId, Long semestreId);
    Optional<Inscription> findByEtudiantIdAndSemestreCodeAndAnneeUniversitaireCouranteTrue(Long etudiantId, String semestreCode);
    List<Inscription> findBySemestreId(Long semestreId);
    List<Inscription> findByAnneeUniversitaireId(Long anneeId);
}
