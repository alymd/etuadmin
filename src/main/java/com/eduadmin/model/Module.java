package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "matieres")
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semestre_id", nullable = false)
    private Semestre semestre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_id")
    private Filiere filiere;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Matiere> matieres = new ArrayList<>();

    public int getCreditsCalculated() {
        if (matieres == null) return 0;
        return matieres.stream().mapToInt(Matiere::getCredits).sum();
    }
}
