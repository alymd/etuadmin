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
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false)
    private double coefficient;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false)
    private boolean presenceTP;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;
}
