package com.eduessence.certificados.service;

import com.eduessence.certificados.model.dto.request.CrearTemplateRequest;
import com.eduessence.certificados.model.dto.response.TemplateResponse;

import java.util.List;

public interface DiplomaTemplateService {

    List<TemplateResponse> listar(String q);

    /** Plantillas disponibles para un curso: globales (cursoId NULL) + las suyas. */
    List<TemplateResponse> listarParaCurso(Long cursoId);

    TemplateResponse obtener(Long id);

    TemplateResponse crear(CrearTemplateRequest req);

    TemplateResponse actualizar(Long id, CrearTemplateRequest req);

    /**
     * Clona una plantilla existente vinculándola al curso indicado. Útil
     * cuando el admin elige reutilizar una plantilla "modelo" desde el form
     * del curso: en vez de compartir el mismo registro (con curso_id NULL o
     * de otro curso), se genera una copia propia de este curso.
     *
     * @param id              id de la plantilla a clonar
     * @param cursoId         curso al que se asignará la copia
     * @param tipoParticipante override del tipo en la copia (si null, se
     *                        mantiene el del original)
     * @param nombreOverride  override del nombre (si null, se mantiene)
     */
    TemplateResponse clonar(Long id, Long cursoId, String tipoParticipante, String nombreOverride);

    void borrar(Long id);
}
