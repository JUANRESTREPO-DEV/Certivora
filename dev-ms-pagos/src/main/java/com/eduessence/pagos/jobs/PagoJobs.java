package com.eduessence.pagos.jobs;

import com.eduessence.pagos.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Jobs programados de pagos.
 *
 * <h3>Expirar pagos vencidos</h3>
 * Cierra los pagos {@code PENDIENTE_LLAVE} cuya {@code reserva_expira} pasó.
 * Marca {@code EXPIRADO} y libera el cupón (si tenía).
 *
 * <p>Cron por defecto: cada 15 minutos. Configurable vía
 * {@code app.pagos.cron-expirar-vencidos}. El intervalo conviene mantenerlo
 * sincronizado con el job de matrículas en cursos para que ambos sistemas
 * vean la "muerte" del pago casi al mismo tiempo.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagoJobs {

    private final PagoService pagoService;

    @Scheduled(cron = "${app.pagos.cron-expirar-vencidos:0 */15 * * * *}")
    public void expirarVencidos() {
        try {
            int n = pagoService.expirarVencidos();
            if (n > 0) {
                log.info("[Job] expirar-vencidos: {} pagos expirados", n);
            } else {
                log.debug("[Job] expirar-vencidos: nada por expirar");
            }
        } catch (Exception ex) {
            log.error("[Job] expirar-vencidos falló", ex);
        }
    }
}
