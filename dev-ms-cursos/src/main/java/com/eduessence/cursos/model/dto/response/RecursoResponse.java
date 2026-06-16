package com.eduessence.cursos.model.dto.response;

import com.eduessence.cursos.model.enums.TipoRecurso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursoResponse {
    private Long id;
    private Long cursoId;
    private TipoRecurso tipo;
    private String titulo;
    private String descripcion;
    private String s3Key;
    private String urlPublica;
    private String mime;
    private Long tamanoBytes;
    private String nombreOriginal;
    private Integer orden;
    private LocalDateTime fechaCreacion;
}
