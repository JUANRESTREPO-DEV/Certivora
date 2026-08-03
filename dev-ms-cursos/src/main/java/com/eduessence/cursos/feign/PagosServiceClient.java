package com.eduessence.cursos.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Cliente de dev-ms-pagos. Las llamadas autenticadas heredan
 * {@code X-User-Id / X-User-Roles} del request entrante gracias al
 * {@code FeignAuthInterceptor}.
 */
@FeignClient(name = "pagos-service", path = "/pagos", fallback = PagosServiceFallback.class)
public interface PagosServiceClient {

    /** Devuelve estado, monto, cupón aplicado, etc. (uso interno, sin auth). */
    @GetMapping("/internal/pagos/{id}")
    Map<String, Object> obtenerPago(@PathVariable("id") Long pagoId);

    /**
     * Mapa {@code {cursoId: ingresosCop}} — suma de pagos APROBADOS por curso.
     * Usado por el reporte de matrículas / ingresos.
     */
    @GetMapping("/internal/pagos/ingresos-por-curso")
    Map<String, Object> ingresosPorCurso();

    /**
     * Crea la fila {@code pago} (estado {@code PENDIENTE_LLAVE} /
     * {@code PENDIENTE_CONFIRMACION} / {@code GRATIS_POR_CUPON}). El body
     * acepta {@code cursoId}, {@code montoCurso}, {@code cuponCodigo}.
     * Devuelve {@code response.pagoId}, {@code response.estado},
     * {@code response.montoCop}, {@code response.cursoGratis}, etc.
     */
    @PostMapping("/api/pagos/iniciar")
    Map<String, Object> iniciarPago(@RequestBody Map<String, Object> body);
}
