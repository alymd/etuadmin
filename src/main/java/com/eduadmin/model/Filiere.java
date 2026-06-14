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
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false)
    private int capaciteMax;
}
