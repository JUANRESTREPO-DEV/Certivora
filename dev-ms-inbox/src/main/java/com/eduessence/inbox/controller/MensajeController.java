package com.eduessence.inbox.controller;

import com.eduessence.inbox.common.RequestContext;
import com.eduessence.inbox.model.dto.GeneralResponseDTO;
import com.eduessence.inbox.model.dto.request.ComposerRequest;
import com.eduessence.inbox.model.dto.request.ResponderRequest;
import com.eduessence.inbox.model.dto.response.MensajeDetalleResponse;
import com.eduessence.inbox.model.dto.response.MensajeResponse;
import com.eduessence.inbox.model.enums.Carpeta;
import com.eduessence.inbox.service.MensajeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Mensajes")
@RestController
@RequestMapping("/api/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private static final String SERVICE = "inbox-service";
    private final MensajeService mensajeService;
    private final RequestContext ctx;

    @GetMapping("/buzon/{buzonId}")
    public ResponseEntity<GeneralResponseDTO<Page<MensajeResponse>>> listar(
            @PathVariable Long buzonId,
            @RequestParam(required = false, defaultValue = "INBOX") Carpeta carpeta,
            Pageable pageable) {
        return ResponseEntity.ok(ok(mensajeService.listarDeBuzon(buzonId, carpeta, ctx.usuarioId(), pageable), "OK"));
    }

    @GetMapping("/buzon/{buzonId}/buscar")
    public ResponseEntity<GeneralResponseDTO<Page<MensajeResponse>>> buscar(
            @PathVariable Long buzonId,
            @RequestParam String q,
            Pageable pageable) {
        return ResponseEntity.ok(ok(mensajeService.buscar(buzonId, q, ctx.usuarioId(), pageable), "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<MensajeDetalleResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(mensajeService.obtener(id, ctx.usuarioId()), "OK"));
    }

    @PatchMapping("/{id}/leido")
    public ResponseEntity<GeneralResponseDTO<Void>> marcarLeido(
            @PathVariable Long id, @RequestParam boolean valor) {
        mensajeService.marcarLeido(id, valor, ctx.usuarioId());
        return ResponseEntity.ok(ok(null, "Actualizado"));
    }

    @PatchMapping("/{id}/destacado")
    public ResponseEntity<GeneralResponseDTO<Void>> destacar(
            @PathVariable Long id, @RequestParam boolean valor) {
        mensajeService.destacar(id, valor, ctx.usuarioId());
        return ResponseEntity.ok(ok(null, "Actualizado"));
    }

    @PatchMapping("/{id}/carpeta")
    public ResponseEntity<GeneralResponseDTO<Void>> mover(
            @PathVariable Long id, @RequestParam Carpeta carpeta) {
        mensajeService.mover(id, carpeta, ctx.usuarioId());
        return ResponseEntity.ok(ok(null, "Mensaje movido"));
    }

    @PostMapping("/{id}/responder")
    public ResponseEntity<GeneralResponseDTO<Map<String, Long>>> responder(
            @PathVariable Long id, @Valid @RequestBody ResponderRequest req) {
        Long nuevoId = mensajeService.responder(id, req, ctx.usuarioId());
        return ResponseEntity.status(201).body(ok(Map.of("mensajeId", nuevoId), "Respuesta enviada"));
    }

    @PostMapping("/componer")
    public ResponseEntity<GeneralResponseDTO<Map<String, Long>>> componer(@Valid @RequestBody ComposerRequest req) {
        Long nuevoId = mensajeService.componer(req, ctx.usuarioId());
        return ResponseEntity.status(201).body(ok(Map.of("mensajeId", nuevoId), "Correo enviado"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
