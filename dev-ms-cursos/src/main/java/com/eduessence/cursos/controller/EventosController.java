package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.response.EventoResponse;
import com.eduessence.cursos.model.enums.TipoSesion;
import com.eduessence.cursos.service.EventosService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints de eventos del calendario:
 *
 *  - Público (sin auth): próximas sesiones de cursos ACTIVO no canceladas.
 *    Usado por la landing.
 *
 *  - Admin (requiere EVENTO_VER): filtros por rango, tipo, curso, etc.
 *    Usado por /app/eventos.
 */
@Tag(name = "Eventos")
@RestController
@RequiredArgsConstructor
public class EventosController {

    private static final String SERVICE = "cursos-service";
    private final EventosService eventosService;

    @GetMapping("/api/public/eventos/proximos")
    public ResponseEntity<GeneralResponseDTO<List<EventoResponse>>> publicosProximos(
            @RequestParam(defaultValue = "60") int horizonteDias,
            @RequestParam(defaultValue = "30") int limite
    ) {
        List<EventoResponse> data = eventosService.listarPublicosProximos(horizonteDias, limite);
        return ResponseEntity.ok(ok(data, "OK"));
    }

    @GetMapping("/api/eventos")
    public ResponseEntity<GeneralResponseDTO<List<EventoResponse>>> admin(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) TipoSesion tipo,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") Boolean incluirCanceladas
    ) {
        List<EventoResponse> data = eventosService.listarAdmin(
                desde, hasta, tipo, cursoId, search, incluirCanceladas);
        return ResponseEntity.ok(ok(data, "OK"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
