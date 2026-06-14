package com.eduadmin.repository;

import com.eduadmin.model.JournalActivite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JournalActiviteRepository extends JpaRepository<JournalActivite, Long> {
    List<JournalActivite> findAllByOrderByDateActionDesc();
    List<JournalActivite> findByUtilisateurOrderByDateActionDesc(String utilisateur);
}
