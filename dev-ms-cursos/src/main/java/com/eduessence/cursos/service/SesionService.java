package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.CrearSesionRequest;
import com.eduessence.cursos.model.dto.response.SesionResponse;

import java.util.List;

public interface SesionService {
    List<SesionResponse> listar(Long cursoId);
    SesionResponse crear(Long cursoId, CrearSesionRequest req);
    SesionResponse actualizar(Long cursoId, Long sesionId, CrearSesionRequest req);
    void borrar(Long cursoId, Long sesionId);

    /**
     * Regenera las credenciales OBS / playback de una sesión VIRTUAL.
     * Útil cuando el provider de streaming cambió o la stream key se filtró.
     */
    SesionResponse regenerarCredencialesStream(Long cursoId, Long sesionId);

    /**
     * Termina la transmisión actual: marca TERMINADO, libera el canal en el
     * provider y guarda la URL de grabación si el provider la entrega.
     */
    SesionResponse terminarTransmision(Long cursoId, Long sesionId);

    /**
     * Cancela una sesión programada. Conserva la fila por trazabilidad y para
     * que los matriculados vean el aviso en el aula virtual.
     * Si la sesión es VIRTUAL y tiene canal de streaming activo, también lo libera.
     */
    SesionResponse cancelar(Long cursoId, Long sesionId, String razon);
}
