package com.eduessence.certificados.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearTemplateRequest {

    /** Si se asocia a un curso particular. {@code null} = plantilla global reutilizable. */
    private Long cursoId;

    /** "ASISTENTE", "PONENTE", "ORGANIZADOR", … */
    @Size(max = 40)
    private String tipoParticipante;

    @NotBlank @Size(max = 150)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    /** Key S3 del archivo base (PDF/PNG). */
    @NotBlank @Size(max = 500)
    private String templateUrl;

    /** JSON con coordenadas de placeholders. Si se omite, se usa el default. */
    private String posiciones;
}
