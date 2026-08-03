package com.eduessence.pagos.controller;

import com.eduessence.pagos.model.dto.GeneralResponseDTO;
import com.eduessence.pagos.model.dto.response.PagoResponse;
import com.eduessence.pagos.repository.PagoRepository;
import com.eduessence.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private static final String SERVICE = "pagos-service";
    private final PagoService pagoService;
    private final PagoRepository pagoRepo;

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

    /** Todos los pagos de un usuario, ordenados por fecha desc. */
    @GetMapping("/usuarios/{usuarioId}/pagos")
    public ResponseEntity<GeneralResponseDTO<List<PagoResponse>>> pagosPorUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(GeneralResponseDTO.<List<PagoResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(pagoService.misPagos(usuarioId)).build());
    }

    /**
     * Devuelve un mapa {@code {cursoId: ingresosCop}} con la suma de pagos
     * APROBADOS por curso. Consumido por el reporte de matrículas / ingresos.
     */
    @GetMapping("/pagos/ingresos-por-curso")
    public ResponseEntity<GeneralResponseDTO<Map<Long, Long>>> ingresosPorCurso() {
        Map<Long, Long> out = new HashMap<>();
        for (Object[] row : pagoRepo.sumIngresosAprobadosPorCurso()) {
            if (row == null || row.length < 2) continue;
            Object idObj = row[0];
            Object sumObj = row[1];
            if (!(idObj instanceof Number id) || !(sumObj instanceof Number sum)) continue;
            out.put(id.longValue(), sum.longValue());
        }
        return ResponseEntity.ok(GeneralResponseDTO.<Map<Long, Long>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(out).build());
    }
}
