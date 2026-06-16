package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Cancelación de una matrícula. La usa tanto el usuario (auto-baja antes de
 * empezar) como el admin (reembolso/expulsión).
 */
@Data
public class CancelarMatriculaRequest {

    /** Motivo opcional. Se guarda en {@code cancelado_motivo}. */
    @Size(max = 500)
    private String motivo;
}
