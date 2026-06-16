package com.eduessence.certificados.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmitirRequest {

    @NotNull
    private Long matriculaId;

    @NotNull
    private Long usuarioId;

    @NotNull
    private Long cursoId;

    private String tipoParticipante;

    /**
     * Id explícito del {@code diploma_template} a usar. Lo manda el MS de
     * cursos cuando el admin asignó una plantilla en el form del curso
     * ({@code curso.template_certificado_id}). Tiene prioridad sobre la
     * búsqueda por curso+tipoParticipante para soportar plantillas globales
     * (cursoId NULL) reutilizadas entre cursos.
     */
    private Long templateId;

    /** Datos para inyectar al PDF. */
    @NotNull
    private String nombreCompleto;

    /** Número de documento del participante (CC, NIUP, etc.). Sin prefijo
     *  — el template define el prefix ("NIUP ") en la posición `documento`. */
    private String numeroDocumento;

    @NotNull
    private String nombreCurso;

    private String fechaCurso;
    private String intensidadHoras;
}
