package com.eduadmin.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReclamationDto {

    @NotNull(message = "La matiere concernee est obligatoire")
    private Long matiereId;

    @NotNull(message = "La note contestee est obligatoire")
    @Min(value = 0, message = "La note minimale est 0")
    @Max(value = 20, message = "La note maximale est 20")
    private Double noteConcernee;

    @NotBlank(message = "L'objet de la reclamation est obligatoire")
    @Size(max = 150, message = "L'objet ne doit pas depasser 150 caracteres")
    private String objet;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    private MultipartFile pieceJointe;
}
