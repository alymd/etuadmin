package com.eduadmin.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteSaisieDto {

    private Long id; // note id (if editing)

    @NotNull(message = "L'inscription est obligatoire")
    private Long inscriptionId;

    @NotNull(message = "La matiere est obligatoire")
    private Long matiereId;

    @Min(value = 0, message = "La note minimale est 0")
    @Max(value = 20, message = "La note maximale est 20")
    private Double noteCC;

    @Min(value = 0, message = "La note minimale est 0")
    @Max(value = 20, message = "La note maximale est 20")
    private Double noteTP;

    @Min(value = 0, message = "La note minimale est 0")
    @Max(value = 20, message = "La note maximale est 20")
    private Double noteExamen;

    @Min(value = 0, message = "La note minimale est 0")
    @Max(value = 20, message = "La note maximale est 20")
    private Double noteRattrapage;
}
