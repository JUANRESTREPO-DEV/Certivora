package com.eduessence.cursos.model.dto.request;

import com.eduessence.cursos.model.enums.TipoRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Persiste un recurso ya subido a S3 (a través de una presigned URL).
 * El front llama primero al endpoint de presigned, sube los bytes,
 * y entonces invoca este endpoint con el {@code s3Key} devuelto.
 */
@Data
public class CrearRecursoRequest {

    @NotNull
    private TipoRecurso tipo;

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    @NotBlank
    @Size(max = 500)
    private String s3Key;

    @Size(max = 700)
    private String urlPublica;

    @Size(max = 120)
    private String mime;

    private Long tamanoBytes;

    @Size(max = 300)
    private String nombreOriginal;

    private Integer orden;
}
