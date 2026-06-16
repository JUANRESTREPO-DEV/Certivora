package com.eduessence.pagos.controller;

import com.eduessence.pagos.model.dto.GeneralResponseDTO;
import com.eduessence.pagos.model.dto.response.PagoResponse;
import com.eduessence.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private static final String SERVICE = "pagos-service";
    private final PagoService pagoService;

    @GetMapping("/pagos/{id}")
    public ResponseEntity<GeneralResponseDTO<PagoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(GeneralResponseDTO.<PagoResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(pagoService.obtener(id)).build());
    }

    @PostMapping("/pagos/expirar-vencidos")
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> expirarVencidos() {
        int n = pagoService.expirarVencidos();
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, Object>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(Map.of("expirados", n)).build());
    }
}
