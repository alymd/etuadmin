package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "filieres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Filiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String code; // Ex: MIAGE, DA2I, MPI

    @Column(nullable = false, length = 150)
    private String nom; // Ex: Methodes Informatiques Appliquees a la Gestion des Entreprises

    @Column(nullable = false)
    private int capaciteMax; // Capacité maximale pour l'affectation automatique
}
