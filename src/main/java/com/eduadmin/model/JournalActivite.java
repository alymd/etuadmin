package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_activites")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String utilisateur;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 1000)
    private String details;
}
