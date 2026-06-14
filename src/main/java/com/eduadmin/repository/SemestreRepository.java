package com.eduadmin.repository;

import com.eduadmin.model.Semestre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemestreRepository extends JpaRepository<Semestre, Long> {
    Optional<Semestre> findByCodeAndAnneeUniversitaireLibelle(String code, String anneeLibelle);
    List<Semestre> findByAnneeUniversitaireId(Long anneeId);
    Optional<Semestre> findByCodeAndAnneeUniversitaireCouranteTrue(String code);
}
