package com.eduessence.auth.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActualizarPerfilRequest {

    @Size(min = 2, max = 120)
    private String nombres;

    @Size(min = 2, max = 120)
    private String apellidos;

    @Size(max = 30)
    private String documento;

    @Size(max = 10)
    private String tipoDocumento;

    @Size(max = 30)
    private String telefono;

    @Size(max = 60)
    private String pais;

    @Size(max = 80)
    private String ciudad;

    @Size(max = 100)
    private String profesion;

    @Size(max = 150)
    private String institucion;
}
