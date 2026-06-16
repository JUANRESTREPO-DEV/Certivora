package com.eduessence.cursos.model.dto.request;

import com.eduessence.cursos.model.enums.ModalidadTipo;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrearCursoRequest {

    @NotBlank @Size(max = 200)
    private String nombre;

    @NotBlank @Size(max = 220)
    private String slug;

    @Size(max = 500)
    private String descripcionCorta;

    private String descripcionLarga;
    private String logoUrl;

    /** S3 key/URL del video corto de presentación (opcional). */
    private String videoPresentacionUrl;

    /** S3 key/URL de la imagen de presentación / hero (opcional). */
    private String imagenPresentacionUrl;

    /** Si el curso emite certificado. Si es true, debe acompañarse de templateCertificadoId. */
    private Boolean emiteCertificado;

    /** Id de la plantilla en certificados-service. Obligatorio si emiteCertificado=true. */
    private Long templateCertificadoId;

    /** Temario JSON: [{titulo, items: [...]}, ...]. Se renderiza como acordeón. */
    private String temarioJson;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime fechaLimiteInscripcion;
    private Integer cupoMaximo;

    @PositiveOrZero
    private Integer precioCop;

    @Positive
    private Integer intensidadHoras;

    /**
     * IDs de los usuarios instructores. El primero del array se considera
     * principal; los demás co-instructores. Debe tener al menos 1.
     */
    @NotEmpty(message = "Debes seleccionar al menos un instructor")
    private List<Long> instructorUsuarioIds;

    @NotEmpty
    private List<ModalidadTipo> modalidades;
}
