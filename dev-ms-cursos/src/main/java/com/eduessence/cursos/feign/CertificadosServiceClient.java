package com.eduessence.cursos.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "certificados-service", path = "/certificados", fallback = CertificadosServiceFallback.class)
public interface CertificadosServiceClient {

    @PostMapping("/internal/certificados/emitir")
    Map<String, Object> emitir(@RequestBody Map<String, Object> payload);

    /** Lista plantillas disponibles. Opcionalmente filtra por curso. */
    @GetMapping("/api/templates")
    Map<String, Object> listarTemplates(@RequestParam(value = "q", required = false) String q,
                                        @RequestParam(value = "cursoId", required = false) Long cursoId);

    @GetMapping("/api/templates/{id}")
    Map<String, Object> obtenerTemplate(@PathVariable("id") Long id);

    /**
     * Crea una nueva plantilla. Body esperado:
     *  {nombre, descripcion?, cursoId?, tipoParticipante?, templateUrl, posiciones?}
     */
    @PostMapping("/api/templates")
    Map<String, Object> crearTemplate(@RequestBody Map<String, Object> payload);

    /**
     * Clona una plantilla existente vinculándola a un curso específico.
     * Body: {@code {"cursoId": Long, "tipoParticipante"?: String, "nombre"?: String}}
     */
    @PostMapping("/api/templates/{id}/clonar")
    Map<String, Object> clonarTemplate(@PathVariable("id") Long id, @RequestBody Map<String, Object> payload);

    /** Actualiza una plantilla existente. Body igual que crear. */
    @PutMapping("/api/templates/{id}")
    Map<String, Object> actualizarTemplate(@PathVariable("id") Long id,
                                            @RequestBody Map<String, Object> payload);

    /** Borra (soft-delete) una plantilla. */
    @DeleteMapping("/api/templates/{id}")
    Map<String, Object> borrarTemplate(@PathVariable("id") Long id);

    /**
     * Mapa {@code {cursoId: emitidos}} — conteo de certificados emitidos por
     * curso. Usado por el reporte de certificados.
     */
    @GetMapping("/internal/certificados/emitidos-por-curso")
    Map<String, Object> emitidosPorCurso();
}
