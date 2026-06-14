package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "etudiants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "utilisateur")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", unique = true, nullable = false)
    private Utilisateur utilisateur;

    @Column(unique = true, nullable = false, length = 20)
    private String matricule; // Doit commencer par C

    @Column(nullable = false)
    private LocalDate dateNaissance;

    @Column(length = 20)
    private String telephone;

    @Column(length = 255)
    private String adresse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_actuelle_id")
    private Filiere filiereActuelle; // Nullable en L1 avant l'orientation

    @Column(nullable = false, length = 10)
    private String niveau; // L1, L2, L3
}
