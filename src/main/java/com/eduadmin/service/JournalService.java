package com.eduadmin.service;

import com.eduadmin.model.JournalActivite;
import com.eduadmin.repository.JournalActiviteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalService {

    private final JournalActiviteRepository journalActiviteRepository;

    @Transactional
    public void log(String utilisateur, String action, String details) {
        JournalActivite journal = JournalActivite.builder()
                .utilisateur(utilisateur)
                .action(action)
                .details(details)
                .dateAction(LocalDateTime.now())
                .build();
        journalActiviteRepository.save(journal);
        log.info("[AUDIT] User: {} | Action: {} | Details: {}", utilisateur, action, details);
    }

    public List<JournalActivite> getToutesActivites() {
        return journalActiviteRepository.findAllByOrderByDateActionDesc();
    }
}
