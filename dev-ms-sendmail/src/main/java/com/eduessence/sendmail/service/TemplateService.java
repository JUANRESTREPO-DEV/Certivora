package com.eduessence.sendmail.service;

import com.eduessence.sendmail.model.dto.PreviewRequest;
import com.eduessence.sendmail.model.dto.PreviewResponse;
import com.eduessence.sendmail.model.dto.TemplateRequest;
import com.eduessence.sendmail.model.dto.TemplateResponse;
import com.eduessence.sendmail.model.dto.TestSendRequest;
import com.eduessence.sendmail.model.entity.EmailTemplate;

import java.util.List;

public interface TemplateService {

    /** Busca por nombre solo si está activo. Lanza 404 si no existe. */
    EmailTemplate findActivoPorNombre(String nombre);

    /** Lista solo plantillas activas (uso del catálogo público al renderizar). */
    List<TemplateResponse> listarActivos();

    /** Lista TODAS las plantillas incluyendo inactivas — para admin. */
    List<TemplateResponse> listarTodos();

    TemplateResponse obtener(Long id);

    TemplateResponse crear(TemplateRequest request);

    TemplateResponse actualizar(Long id, TemplateRequest request);

    TemplateResponse cambiarEstado(Long id, boolean activo);

    /** Devuelve el HTML crudo del template descargado desde S3. */
    String obtenerHtml(Long id);

    /** Sube/sobrescribe el HTML del template en S3. */
    void guardarHtml(Long id, String html);

    /**
     * Renderiza el HTML del template con las variables provistas (mezcladas
     * con valores por defecto + contexto del sistema). No envía nada — solo
     * devuelve el HTML resultante para previsualización.
     */
    PreviewResponse previsualizar(Long id, PreviewRequest req);

    /**
     * Envía un correo real al destinatario provisto usando el render de
     * preview. Útil para que el admin pruebe la plantilla en su buzón antes
     * de activarla en producción.
     */
    String enviarPrueba(Long id, TestSendRequest req);
}
