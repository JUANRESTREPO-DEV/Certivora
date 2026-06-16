package com.eduessence.sendmail.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVariableDTO {
    private Long id;
    private String nombre;
    private String tipoDato;
    private Boolean requerido;
    private String valorDefecto;
    private String descripcion;
}
