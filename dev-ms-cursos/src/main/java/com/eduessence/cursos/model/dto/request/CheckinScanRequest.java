package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body para {@code POST /api/checkin/scan}. El operador (o el propio
 * asistente) escanea el QR de la escarapela y la app envía el
 * {@code escarapelaToken}. El backend deriva la matrícula, valida que el
 * curso tenga sesión presencial hoy, y registra el check-in.
 *
 * <p>Alternativa: si el escáner escanea el QR de la sesión (no de la
 * escarapela), enviar {@code qrToken} + {@code matriculaId}.</p>
 */
@Data
public class CheckinScanRequest {

    /** Token de la escarapela del asistente (preferido). */
    private String escarapelaToken;

    /** Token de la sesión (alternativa: si el QR escaneado es de la sesión). */
    private String qrToken;

    /**
     * Id de la matrícula del asistente. Requerido si se usa {@code qrToken}
     * de sesión (porque el QR de sesión no identifica al asistente).
     */
    private Long matriculaId;

    /** Id de la sesión — si no viene se busca por qrToken. */
    private Long sesionId;
}
