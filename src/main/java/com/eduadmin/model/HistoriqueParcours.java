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
    private Filiere filiere; // Null en L1 avant l'orientation

    @Column(name = "moyenne_annuelle")
    private Double moyenneAnnuelle; // Moyenne de l'année (moyenne des 2 semestres)

    @Column(nullable = false)
    private boolean valide = false; // Indique si l'année est validée (moyenne >= 10)
}
