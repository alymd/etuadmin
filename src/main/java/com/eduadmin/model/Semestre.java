package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "semestres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Semestre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annee_universitaire_id", nullable = false)
    private AnneeUniversitaire anneeUniversitaire;
}
