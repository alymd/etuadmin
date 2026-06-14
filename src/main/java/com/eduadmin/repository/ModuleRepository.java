package com.eduadmin.repository;

import com.eduadmin.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    Optional<Module> findByCode(String code);
    List<Module> findBySemestreId(Long semestreId);
    List<Module> findBySemestreIdAndFiliereId(Long semestreId, Long filiereId);
    List<Module> findByFiliereId(Long filiereId);
}
