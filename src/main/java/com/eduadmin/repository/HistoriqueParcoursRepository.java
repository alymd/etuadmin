package com.eduadmin.repository;

import com.eduadmin.model.HistoriqueParcours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoriqueParcoursRepository extends JpaRepository<HistoriqueParcours, Long> {
    List<HistoriqueParcours> findByEtudiantId(Long etudiantId);
    List<HistoriqueParcours> findByEtudiantMatricule(String matricule);
    Optional<HistoriqueParcours> findByEtudiantIdAndAnneeUniversitaireId(Long etudiantId, Long anneeId);
}
