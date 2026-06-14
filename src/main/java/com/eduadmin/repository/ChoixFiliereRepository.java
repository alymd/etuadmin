package com.eduadmin.repository;

import com.eduadmin.model.ChoixFiliere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChoixFiliereRepository extends JpaRepository<ChoixFiliere, Long> {
    List<ChoixFiliere> findByEtudiantId(Long etudiantId);
    List<ChoixFiliere> findByEtudiantIdAndAnneeUniversitaireIdOrderByOrdrePreferenceAsc(Long etudiantId, Long anneeId);
    List<ChoixFiliere> findByAnneeUniversitaireId(Long anneeId);
}
