package com.eduadmin.repository;

import com.eduadmin.model.AnneeUniversitaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AnneeUniversitaireRepository extends JpaRepository<AnneeUniversitaire, Long> {
    Optional<AnneeUniversitaire> findByLibelle(String libelle);
    Optional<AnneeUniversitaire> findByCouranteTrue();
}
