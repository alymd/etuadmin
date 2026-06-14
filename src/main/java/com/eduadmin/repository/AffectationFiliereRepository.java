package com.eduadmin.repository;

import com.eduadmin.model.AffectationFiliere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AffectationFiliereRepository extends JpaRepository<AffectationFiliere, Long> {
    Optional<AffectationFiliere> findByEtudiantIdAndAnneeUniversitaireId(Long etudiantId, Long anneeId);
    List<AffectationFiliere> findByAnneeUniversitaireId(Long anneeId);
    List<AffectationFiliere> findByEtudiantId(Long etudiantId);
    List<AffectationFiliere> findByFiliereAffecteeIdAndAnneeUniversitaireId(Long filiereId, Long anneeId);
}
