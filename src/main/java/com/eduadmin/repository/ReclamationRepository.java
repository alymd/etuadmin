package com.eduadmin.repository;

import com.eduadmin.model.Reclamation;
import com.eduadmin.model.StatutReclamation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByEtudiantIdOrderByDateCreationDesc(Long etudiantId);
    List<Reclamation> findByEtudiantUtilisateurUsernameOrderByDateCreationDesc(String username);
    List<Reclamation> findByStatutOrderByDateCreationDesc(StatutReclamation statut);
    long countByStatut(StatutReclamation statut);
    long countByMatiereId(Long matiereId);
}
