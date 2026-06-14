package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "annees_universitaires")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnneeUniversitaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String libelle;

    @Column(nullable = false)
    private boolean courante = false;
}
