package com.eduadmin.repository;

import com.eduadmin.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByInscriptionId(Long inscriptionId);
    Optional<Note> findByInscriptionIdAndMatiereId(Long inscriptionId, Long matiereId);
    List<Note> findByInscriptionEtudiantId(Long etudiantId);
    List<Note> findByInscriptionEtudiantMatricule(String matricule);
    List<Note> findByMatiereId(Long matiereId);
}
