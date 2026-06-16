package com.eduessence.cursos.model.dto.request;

import com.eduessence.cursos.model.enums.ModalidadTipo;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Update parcial de los campos editables de un curso. Todos los campos son
 * opcionales (un null = no tocar). El backend ignora null y aplica el resto.
 *
 * Para campos que sí se quieren borrar (ej. quitar el video de presentación),
 * el frontend envía {@code ""} y el backend lo persiste como null.
 */
@Data
public class ActualizarCursoRequest {

    @Size(max = 200)
    private String nombre;

    @Size(max = 220)
    private String slug;

    @Size(max = 500)
    private String descripcionCorta;

    private String descripcionLarga;
    private String logoUrl;
    private String videoPresentacionUrl;
    private String imagenPresentacionUrl;

    private Boolean emiteCertificado;
    private Long templateCertificadoId;

    private String temarioJson;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime fechaLimiteInscripcion;

    private Integer cupoMaximo;

    @PositiveOrZero
    private Integer precioCop;

    @Positive
    private Integer intensidadHoras;

    private List<Long> instructorUsuarioIds;

    private List<ModalidadTipo> modalidades;
}
