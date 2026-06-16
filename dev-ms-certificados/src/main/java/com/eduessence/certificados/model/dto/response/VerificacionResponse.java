package com.eduessence.certificados.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionResponse {
    private boolean valido;
    private String codigo;
    private String nombreCompleto;
    private String nombreCurso;
    private LocalDateTime fechaEmision;
    private String mensaje;
}
