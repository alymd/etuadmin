package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "releves_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"etudiant", "anneeUniversitaire", "semestre"})
public class ReleveNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annee_universitaire_id")
    private AnneeUniversitaire anneeUniversitaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semestre_id")
    private Semestre semestre; // Null si c'est annuel ou complet

    @Column(nullable = false, length = 30)
    private String type; // SEMESTRIEL, ANNUEL, COMPLET

    @Column(name = "date_generation", nullable = false)
    private LocalDate dateGeneration;

    @Column(name = "genere_par", nullable = false, length = 100)
    private String generePar;
}
