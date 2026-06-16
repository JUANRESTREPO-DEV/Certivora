package com.eduessence.cursos.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "streaming-service", path = "/streaming", fallback = StreamingServiceFallback.class)
public interface StreamingServiceClient {

    /** Devuelve { sessionId, ingestUrl, streamKey, playbackUrl }. */
    @PostMapping("/api/streams")
    Map<String, Object> crearStream(@RequestBody Map<String, Object> payload);

    /**
     * Pide nuevas credenciales para una sesión existente. Útil al cambiar provider
     * (MOCK → LOCAL_RTMP → AWS_IVS) o si la stream key se vio comprometida.
     */
    @PostMapping("/api/streams/{sessionId}/regenerar")
    Map<String, Object> regenerarStream(@PathVariable("sessionId") String sessionId);

    /**
     * Termina una sesión en vivo: marca TERMINADO, captura URL de grabación
     * si el provider la entrega y libera el canal en el provider (evita acumular
     * recursos vivos en AWS IVS, etc.).
     */
    @PostMapping("/api/streams/{sessionId}/terminar")
    Map<String, Object> terminarStream(@PathVariable("sessionId") String sessionId);

    /**
     * Borra el canal en el provider (AWS IVS, MediaMTX, etc.) y elimina la
     * entidad de tracking del streaming-service. No se puede invocar si el
     * stream está EN_VIVO.
     */
    @DeleteMapping("/api/streams/{sessionId}")
    Map<String, Object> eliminarStream(@PathVariable("sessionId") String sessionId);
}
