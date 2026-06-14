package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "affectations_filieres", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"etudiant_id", "annee_universitaire_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"etudiant", "filiereAffectee", "anneeUniversitaire"})
public class AffectationFiliere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_affectee_id", nullable = false)
    private Filiere filiereAffectee;

    @Column(name = "moyenne_orientation", nullable = false)
    private Double moyenneOrientation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annee_universitaire_id", nullable = false)
    private AnneeUniversitaire anneeUniversitaire;

    @Column(name = "date_affectation", nullable = false)
    private LocalDate dateAffectation;
}
