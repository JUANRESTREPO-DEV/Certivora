package com.eduessence.cursos.model.dto.response;

import com.eduessence.cursos.model.enums.EstadoCurso;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoResponse {
    private Long id;
    private String nombre;
    private String slug;
    private String descripcionCorta;
    private String descripcionLarga;
    private String logoUrl;
    private String videoPresentacionUrl;
    private String imagenPresentacionUrl;
    private Boolean emiteCertificado;
    private Long templateCertificadoId;
    private String temarioJson;
    private EstadoCurso estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime fechaLimiteInscripcion;
    private Integer cupoMaximo;
    private Integer precioCop;
    private Integer intensidadHoras;
    /** Mantenido por backward-compat: id del instructor principal. */
    private Long instructorUsuarioId;
    /** Lista enriquecida de instructores (nombres, apellidos, email). */
    private List<InstructorDTO> instructores;
    private Set<ModalidadTipo> modalidades;
    private Boolean hibrido;
}
