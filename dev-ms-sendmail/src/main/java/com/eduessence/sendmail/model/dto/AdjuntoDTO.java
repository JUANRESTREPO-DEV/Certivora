package com.eduessence.sendmail.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjuntoDTO {

    @NotBlank
    private String nombreArchivo;

    @NotBlank
    private String contentType;

    /** Contenido del archivo codificado en base64. */
    @NotBlank
    private String contenidoBase64;
}
