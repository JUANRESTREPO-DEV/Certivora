package com.eduessence.cursos.model.dto.request;

import com.eduessence.cursos.model.enums.TipoLeccion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearLeccionRequest {

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    @NotNull
    private TipoLeccion tipo;

    private Integer orden;

    private Boolean bloqueante;

    private Integer duracionSegundos;

    /** Para VIDEO/PDF: URL pública del recurso en S3. */
    @Size(max = 500)
    private String recursoUrl;

    /** Para TEXTO: contenido en HTML/Markdown. */
    private String contenidoTexto;

    /** Para EXAMEN: id del examen asociado (debe pertenecer al mismo curso). */
    private Long examenId;

    /** Para ACTIVIDAD: id de la actividad. */
    private Long actividadId;

    /** Para SESION_VIRTUAL: id de la sesión virtual. */
    private Long sesionVirtualId;
}
