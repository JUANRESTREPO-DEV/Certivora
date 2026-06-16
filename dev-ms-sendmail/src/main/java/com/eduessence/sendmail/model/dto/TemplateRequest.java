package com.eduessence.sendmail.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String pathTemplate;

    private String bucket;
    private String asuntoDefault;
    private String descripcion;
    private Boolean activo;

    private List<TemplateVariableDTO> variables;
}
