package com.eduessence.cursos.jobs;

import com.eduessence.cursos.service.MatriculaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Jobs programados del ciclo de inscripción.
 *
 * <h3>Liberar reservas vencidas</h3>
 * Cada N minutos busca matrículas en estado {@code PENDIENTE_PAGO} con
 * {@code reservaExpira < now()}, las marca {@code CANCELADA} y promueve al
 * siguiente en la lista de espera. Es la única forma en que un alumno que
 * abandona el flujo de pago libera su cupo automáticamente.
 *
 * <p>Cron por defecto: cada 15 minutos. Configurable vía
 * {@code app.matricula.cron-liberar-reservas} (expresión cron Spring). Para
 * deshabilitar en un entorno (ej. tests), usar
 * {@code app.matricula.cron-liberar-reservas=-}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatriculaJobs {

    private final MatriculaService matriculaService;

    @Scheduled(cron = "${app.matricula.cron-liberar-reservas:0 */15 * * * *}")
    public void liberarReservasVencidas() {
        try {
            int n = matriculaService.liberarReservasVencidas();
            if (n > 0) {
                log.info("[Job] liberar-reservas-vencidas: {} matrículas liberadas", n);
            } else {
                log.debug("[Job] liberar-reservas-vencidas: nada por liberar");
            }
        } catch (Exception ex) {
            // No dejamos que una excepción mate el scheduler — siempre log y seguir
            log.error("[Job] liberar-reservas-vencidas falló", ex);
        }
    }
}
