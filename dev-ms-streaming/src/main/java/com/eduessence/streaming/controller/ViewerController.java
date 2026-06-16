package com.eduessence.streaming.controller;

import com.eduessence.streaming.exception.ServerApiStatusCode;
import com.eduessence.streaming.exception.StreamingApiException;
import com.eduessence.streaming.model.dto.GeneralResponseDTO;
import com.eduessence.streaming.model.dto.request.ViewerEventRequest;
import com.eduessence.streaming.service.StreamingService;
import com.eduessence.streaming.utils.HttpUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Viewers")
@RestController
@RequestMapping("/api/streams/{id}")
@RequiredArgsConstructor
public class ViewerController {

    private static final String SERVICE = "streaming-service";
    private final StreamingService streamingService;

    @PostMapping("/viewer-event")
    public ResponseEntity<GeneralResponseDTO<Void>> registrarEvento(
            @PathVariable("id") Long sessionId,
            @Valid @RequestBody ViewerEventRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            HttpServletRequest http
    ) {
        Long usuarioId = parseUserId(userIdHeader);
        streamingService.registrarEvento(sessionId, usuarioId, body.getSessionToken(),
                body.getEvento(), body.getSegundosAcumulados(),
                HttpUtils.clientIp(http), HttpUtils.userAgent(http));
        return ResponseEntity.ok(GeneralResponseDTO.<Void>builder()
                .statusCode(200).serviceName(SERVICE).message("Evento registrado").build());
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
