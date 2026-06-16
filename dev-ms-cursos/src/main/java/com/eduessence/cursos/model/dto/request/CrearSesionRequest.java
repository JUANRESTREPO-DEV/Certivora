package com.eduessence.cursos.model.dto.request;

import com.eduessence.cursos.model.enums.TipoSesion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Crea / actualiza una sesión del curso. Puede ser:
 *  · VIRTUAL    → al crear se solicita un canal a streaming-service
 *  · PRESENCIAL → se genera un qrToken único para registro de asistencia
 */
@Data
public class CrearSesionRequest {

    @NotNull
    private TipoSesion tipo;

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    @NotNull
    private LocalDateTime fechaInicio;

    @Positive
    private Integer duracionMinutos;

    /** PRESENCIAL: sede / dirección del evento. */
    @Size(max = 500)
    private String ubicacionTexto;
}
