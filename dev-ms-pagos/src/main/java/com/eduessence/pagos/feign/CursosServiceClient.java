package com.eduessence.pagos.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "cursos-service", path = "/cursos", fallback = CursosServiceFallback.class)
public interface CursosServiceClient {

    /**
     * Activa una matrícula conocida por su id.
     * Lo usa cualquier endpoint admin de pagos que ya tenga el id resuelto.
     */
    @PostMapping("/internal/matriculas/{id}/activar")
    void activarMatricula(@PathVariable("id") Long matriculaId);

    /**
     * Notifica a cursos que un pago fue aprobado. Cursos resuelve la
     * matrícula vinculada (campo {@code pago_id}) y la activa.
     * Idempotente: si la matrícula ya está {@code ACTIVA}, no hace nada.
     */
    @PostMapping("/internal/pagos/{pagoId}/aprobado")
    void notificarPagoAprobado(@PathVariable("pagoId") Long pagoId);
}
