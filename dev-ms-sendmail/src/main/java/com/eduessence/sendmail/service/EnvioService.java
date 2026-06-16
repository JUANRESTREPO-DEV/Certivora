package com.eduessence.sendmail.service;

import com.eduessence.sendmail.model.dto.EmailEnvioResponse;
import com.eduessence.sendmail.model.dto.PageResponse;
import com.eduessence.sendmail.model.enums.EstadoEnvio;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Operaciones admin sobre el log de envíos {@code email_envio}.
 */
public interface EnvioService {

    /** Búsqueda paginada con filtros opcionales. */
    PageResponse<EmailEnvioResponse> buscar(EstadoEnvio estado,
                                            Long templateId,
                                            LocalDateTime desde,
                                            LocalDateTime hasta,
                                            String q,
                                            int page,
                                            int size);

    /** Detalle por id (incluye variables parseadas). */
    EmailEnvioResponse obtener(Long id);

    /**
     * Reintenta el envío original: reconstruye contexto desde
     * {@code variables_usadas}, render Velocity y manda otra vez.
     * Persiste un nuevo registro en {@code email_envio} con el resultado.
     */
    EmailEnvioResponse reintentar(Long id);

    /** Conteos {ENVIADO, FALLIDO, REINTENTAR, TOTAL} para los KPIs. */
    Map<String, Long> resumen();
}
