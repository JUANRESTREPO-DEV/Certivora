package com.eduessence.certificados.controller;

import com.eduessence.certificados.model.dto.GeneralResponseDTO;
import com.eduessence.certificados.model.dto.request.EmitirRequest;
import com.eduessence.certificados.model.dto.response.CertificadoResponse;
import com.eduessence.certificados.service.CertificadoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint llamado por dev-ms-cursos cuando una matrícula pasa a APROBADA.
 */
@Tag(name = "Internal")
@RestController
@RequestMapping("/internal/certificados")
@RequiredArgsConstructor
public class InternalController {

    private static final String SERVICE = "certificados-service";
    private final CertificadoService certificadoService;

    @PostMapping("/emitir")
    public ResponseEntity<GeneralResponseDTO<CertificadoResponse>> emitir(
            @Valid @RequestBody EmitirRequest req) {
        CertificadoResponse data = certificadoService.emitir(req);
        return ResponseEntity.status(201).body(GeneralResponseDTO.<CertificadoResponse>builder()
                .statusCode(201).serviceName(SERVICE).message("Certificado emitido")
                .response(data).build());
    }

    /**
     * Recalcula el {@code hash_verificacion} del certificado con el algoritmo
     * determinístico actual (fecha truncada a segundos) y lo persiste.
     *
     * Solo para certificados emitidos antes del fix del algoritmo de hash,
     * cuyos hashes ya no se pueden recalcular desde la fecha guardada porque
     * los nanosegundos originales se perdieron en el round-trip por MySQL.
     *
     * El endpoint vive bajo {@code /internal/**} — no expuesto al público vía
     * gateway. Es idempotente.
     */
    @PostMapping("/{codigo}/reparar-hash")
    public ResponseEntity<GeneralResponseDTO<CertificadoResponse>> repararHash(
            @PathVariable String codigo) {
        CertificadoResponse data = certificadoService.repararHash(codigo);
        return ResponseEntity.ok(GeneralResponseDTO.<CertificadoResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("Hash recalculado")
                .response(data).build());
    }
}
