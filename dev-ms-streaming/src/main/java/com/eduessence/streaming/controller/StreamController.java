package com.eduessence.streaming.controller;

import com.eduessence.streaming.exception.StreamingApiException;
import com.eduessence.streaming.exception.ServerApiStatusCode;
import com.eduessence.streaming.model.dto.GeneralResponseDTO;
import com.eduessence.streaming.model.dto.request.CrearStreamRequest;
import com.eduessence.streaming.model.dto.response.StreamCredentialsResponse;
import com.eduessence.streaming.model.dto.response.StreamSessionResponse;
import com.eduessence.streaming.service.StreamingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Streams")
@RestController
@RequestMapping("/api/streams")
@RequiredArgsConstructor
public class StreamController {

    private static final String SERVICE = "streaming-service";
    private final StreamingService streamingService;

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<StreamCredentialsResponse>> crear(
            @Valid @RequestBody CrearStreamRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long instructorId = parseUserId(userIdHeader);
        StreamCredentialsResponse data = streamingService.crearSesion(
                req.getCursoId(), req.getSesionVirtualId(), instructorId, req.getTitulo());
        return ResponseEntity.status(201).body(ok(data, "Sesión creada"));
    }

    @PostMapping("/{id}/terminar")
    public ResponseEntity<GeneralResponseDTO<StreamSessionResponse>> terminar(@PathVariable Long id) {
        return ResponseEntity.ok(ok(streamingService.terminar(id), "Sesión terminada"));
    }

    @PostMapping("/{id}/regenerar")
    public ResponseEntity<GeneralResponseDTO<StreamCredentialsResponse>> regenerar(@PathVariable Long id) {
        return ResponseEntity.ok(ok(streamingService.regenerarCredenciales(id), "Credenciales regeneradas"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<Void>> eliminar(@PathVariable Long id) {
        streamingService.eliminar(id);
        return ResponseEntity.ok(ok(null, "Sesión eliminada"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<StreamSessionResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(streamingService.obtener(id), "OK"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }

    private Long parseUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new StreamingApiException(ServerApiStatusCode.DATOS_INVALIDOS, "X-User-Id ausente");
        }
        try { return Long.valueOf(header); }
        catch (NumberFormatException ex) {
            throw new StreamingApiException(ServerApiStatusCode.DATOS_INVALIDOS, "X-User-Id inválido");
        }
    }
}
