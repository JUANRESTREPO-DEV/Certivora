package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Guarda la respuesta del alumno a una pregunta. Se llama mientras el intento
 * está abierto. Idempotente: si la respuesta ya existe, se sobrescribe.
 */
@Data
public class GuardarRespuestaRequest {

    @NotNull
    private Long preguntaId;

    /** IDs de opciones seleccionadas para SINGLE/MULTIPLE/VERDADERO_FALSO. */
    private List<Long> opcionesSeleccionadas;

    /** Respuesta libre para TEXTO_CORTO. */
    private String respuestaTexto;
}
