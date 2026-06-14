package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"etudiant", "matiere"})
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @Column(name = "note_concernee", nullable = false)
    private Double noteConcernee;

    @Column(nullable = false, length = 150)
    private String objet;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "piece_jointe_nom", length = 255)
    private String pieceJointeNom; // Nom du fichier stocké

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @Column(name = "commentaire_admin", length = 500)
    private String commentaireAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutReclamation statut = StatutReclamation.EN_ATTENTE;
}
