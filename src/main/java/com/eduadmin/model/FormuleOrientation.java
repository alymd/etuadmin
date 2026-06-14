package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "formules_orientation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "filiere")
public class FormuleOrientation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_id", unique = true, nullable = false)
    private Filiere filiere;

    @Column(nullable = false, length = 255)
    private String formule; // Ex: (MO1S1 + MO1S2 + 2*MO2S1 + 2*MO2S2 + MGS1 + MGS2) / 8
}
