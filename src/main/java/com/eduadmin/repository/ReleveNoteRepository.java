package com.eduadmin.repository;

import com.eduadmin.model.ReleveNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReleveNoteRepository extends JpaRepository<ReleveNote, Long> {
    List<ReleveNote> findByEtudiantId(Long etudiantId);
    List<ReleveNote> findByEtudiantMatricule(String matricule);
}
