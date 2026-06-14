package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inscriptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"etudiant_id", "semestre_id", "annee_universitaire_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"etudiant", "semestre", "anneeUniversitaire"})
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semestre_id", nullable = false)
    private Semestre semestre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annee_universitaire_id", nullable = false)
    private AnneeUniversitaire anneeUniversitaire;

    @Column(name = "moyenne_semestre")
    private Double moyenneSemestre;

    @Column(name = "credits_obtenus")
    private Integer creditsObtenus;

    @Column(nullable = false)
    private boolean valide = false;
}
