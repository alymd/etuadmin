package com.eduadmin.repository;

import com.eduadmin.model.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    Optional<Etudiant> findByMatricule(String matricule);
    Optional<Etudiant> findByUtilisateurUsername(String username);
    List<Etudiant> findByNiveau(String niveau);
    List<Etudiant> findByFiliereActuelleId(Long filiereId);
    List<Etudiant> findByFiliereActuelleCode(String code);
    boolean existsByMatricule(String matricule);

    @Query("""
            SELECT DISTINCT e FROM Etudiant e
            JOIN e.utilisateur u
            LEFT JOIN e.filiereActuelle f
            WHERE (:nom IS NULL OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :nom, '%'))
                OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :nom, '%'))
                OR LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE LOWER(CONCAT('%', :nom, '%')))
            AND (:matricule IS NULL OR LOWER(e.matricule) LIKE LOWER(CONCAT('%', :matricule, '%')))
            AND (:filiereId IS NULL OR f.id = :filiereId)
            AND (:niveau IS NULL OR e.niveau = :niveau)
            AND (:anneeId IS NULL OR EXISTS (
                SELECT i FROM Inscription i
                WHERE i.etudiant = e AND i.anneeUniversitaire.id = :anneeId))
            ORDER BY e.matricule
            """)
    List<Etudiant> search(@Param("nom") String nom,
                          @Param("matricule") String matricule,
                          @Param("filiereId") Long filiereId,
                          @Param("niveau") String niveau,
                          @Param("anneeId") Long anneeId);
}
