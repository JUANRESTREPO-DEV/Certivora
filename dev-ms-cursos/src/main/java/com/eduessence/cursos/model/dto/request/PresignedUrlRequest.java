package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Solicita una URL pre-firmada de S3 para subir un archivo desde el browser
 * directamente al bucket. El backend NO recibe los bytes; sólo firma.
 *
 * El destino dentro del bucket se decide a partir de {@code carpeta} +
 * {@code cursoId} + UUID + extensión inferida del nombre.
 */
@Data
public class PresignedUrlRequest {

    /** Nombre original del archivo (sirve para inferir extensión y guardar metadata). */
    @NotBlank
    @Size(max = 300)
    private String nombreArchivo;

    /** MIME type que el browser declara (image/jpeg, video/mp4, application/pdf, …). */
    @NotBlank
    @Size(max = 120)
    private String mime;

    /** Tamaño en bytes — para validar contra cuota. */
    @Positive
    private Long tamanoBytes;

    /**
     * Subcarpeta lógica del recurso. Sirve para separar tipos:
     *  · presentacion-video
     *  · presentacion-imagen
     *  · recurso-pdf
     *  · leccion-video        (Fase C)
     *  · grabacion-sesion     (Fase D)
     *  · certificado-template (Fase B vía certificados-service)
     */
    @NotBlank
    @Pattern(regexp = "[a-z0-9-]+", message = "carpeta inválida")
    private String carpeta;
}
