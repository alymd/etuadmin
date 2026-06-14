package com.eduadmin.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtudiantFormDto {

    private Long id;

    @Pattern(regexp = "^C[0-9]{5}$", message = "Le matricule doit commencer par 'C' suivi de 5 chiffres (ex: C28916)")
    private String matricule;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres")
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire")
    @Size(max = 100, message = "Le prenom ne doit pas depasser 100 caracteres")
    private String prenom;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 150, message = "L'email ne doit pas depasser 150 caracteres")
    private String email;

    @NotNull(message = "La date de naissance est obligatoire")
    @Past(message = "La date de naissance doit etre dans le passe")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateNaissance;

    @Size(max = 20, message = "Le telephone ne doit pas depasser 20 caracteres")
    private String telephone;

    @Size(max = 255, message = "L'adresse ne doit pas depasser 255 caracteres")
    private String adresse;

    @NotBlank(message = "Le niveau est obligatoire")
    private String niveau; // L1  L2 L3

    private Long filiereId;
}
