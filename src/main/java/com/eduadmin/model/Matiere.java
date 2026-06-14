package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "matieres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "module")
public class Matiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String code; // Ex: INF101

    @Column(nullable = false, length = 150)
    private String nom; // Ex: Algorithmique en Java

    @Column(nullable = false)
    private double coefficient;

    @Column(nullable = false)
    private int credits; // Crédits ECTS

    @Column(nullable = false)
    private boolean presenceTP; // true si la matière possède des TP, false sinon

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;
}
