package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearSeccionRequest {

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    private Integer orden;

    /** Si true, hay que completar las lecciones bloqueantes para avanzar. */
    private Boolean bloqueante;
}
