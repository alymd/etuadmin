package com.eduadmin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"inscription_id", "matiere_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"inscription", "matiere"})
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @Column(name = "note_cc")
    private Double noteCC;

    @Column(name = "note_tp")
    private Double noteTP;

    @Column(name = "note_examen")
    private Double noteExamen;

    @Column(name = "note_rattrapage")
    private Double noteRattrapage;

    @Column(name = "note_generale")
    private Double noteGenerale;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_examen", nullable = false, length = 30)
    @Builder.Default
    private SessionExamen sessionExamen = SessionExamen.NORMAL;

    @Column(nullable = false)
    private boolean validee = false;

    public void recalculerNoteGenerale() {
        if (noteCC == null && noteExamen == null && noteTP == null && noteRattrapage == null) {
            this.noteGenerale = null;
            this.validee = false;
            return;
        }

        double cc = this.noteCC != null ? this.noteCC : 0.0;
        double exam = this.noteExamen != null ? this.noteExamen : 0.0;
        
        if (this.noteRattrapage != null && this.noteRattrapage > exam) {
            exam = this.noteRattrapage;
            this.sessionExamen = SessionExamen.RATTRAPAGE;
        }

        if (this.matiere != null && this.matiere.isPresenceTP()) {
            double tp = this.noteTP != null ? this.noteTP : 0.0;
            this.noteGenerale = Math.round(((tp + 2 * cc + 3 * exam) / 6.0) * 100.0) / 100.0;
        } else {
            this.noteGenerale = Math.round(((2 * cc + 3 * exam) / 5.0) * 100.0) / 100.0;
        }

        this.validee = this.noteGenerale != null && this.noteGenerale >= 10.0;
    }
}
