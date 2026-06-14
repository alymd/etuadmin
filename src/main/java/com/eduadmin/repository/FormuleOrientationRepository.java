package com.eduadmin.repository;

import com.eduadmin.model.FormuleOrientation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FormuleOrientationRepository extends JpaRepository<FormuleOrientation, Long> {
    Optional<FormuleOrientation> findByFiliereId(Long filiereId);
    Optional<FormuleOrientation> findByFiliereCode(String code);
}
