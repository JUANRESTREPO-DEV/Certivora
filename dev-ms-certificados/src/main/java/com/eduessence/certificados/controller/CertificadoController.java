package com.eduessence.certificados.controller;

import com.eduessence.certificados.exception.CertificadosApiException;
import com.eduessence.certificados.exception.ServerApiStatusCode;
import com.eduessence.certificados.model.dto.GeneralResponseDTO;
import com.eduessence.certificados.model.dto.response.CertificadoResponse;
import com.eduessence.certificados.service.CertificadoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Mis certificados")
@RestController
@RequestMapping("/api/certificados")
@RequiredArgsConstructor
public class CertificadoController {

    private static final String SERVICE = "certificados-service";
    private final CertificadoService certificadoService;

    @GetMapping("/mis")
    public ResponseEntity<GeneralResponseDTO<List<CertificadoResponse>>> mis(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(GeneralResponseDTO.<List<CertificadoResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(certificadoService.misCertificados(userId)).build());
    }

    @GetMapping("/{codigo}/descargar")
    public ResponseEntity<GeneralResponseDTO<Map<String, String>>> descargar(@PathVariable String codigo) {
        String url = certificadoService.urlDescarga(codigo);
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, String>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(Map.of("url", url)).build());
    }

    private Long parseUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new CertificadosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "X-User-Id ausente");
        }
        try { return Long.valueOf(header); }
        catch (NumberFormatException ex) {
            throw new CertificadosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "X-User-Id inválido");
        }
    }
}
