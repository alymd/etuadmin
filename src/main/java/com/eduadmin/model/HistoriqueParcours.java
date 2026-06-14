package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "historique_parcours")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"etudiant", "anneeUniversitaire", "filiere"})
public class HistoriqueParcours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annee_universitaire_id", nullable = false)
    private AnneeUniversitaire anneeUniversitaire;

    @Column(nullable = false, length = 10)
    private String niveau; // L1, L2, L3

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_id")
    private Filiere filiere;

    @Column(name = "moyenne_annuelle")
    private Double moyenneAnnuelle;

    @Column(nullable = false)
    private boolean valide = false;
}
