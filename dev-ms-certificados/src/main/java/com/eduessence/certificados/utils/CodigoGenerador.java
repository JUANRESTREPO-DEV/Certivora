package com.eduessence.certificados.utils;

import com.eduessence.certificados.repository.CertificadoEmitidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Month;

/**
 * Genera códigos {@code EDU-YYYY-NNNNNN}, secuenciales por año.
 * Para v1: cuenta los certificados emitidos en el año y suma 1.
 * Para HA: usar secuencia BD o Redis.
 */
@Component
@RequiredArgsConstructor
public class CodigoGenerador {

    private final CertificadoEmitidoRepository repository;

    public synchronized String generar() {
        int year = LocalDateTime.now().getYear();
        LocalDateTime from = LocalDateTime.of(year, Month.JANUARY, 1, 0, 0);
        LocalDateTime to = from.plusYears(1);
        long count = repository.countByFechaEmisionBetween(from, to);
        long siguiente = count + 1;
        return String.format("EDU-%d-%06d", year, siguiente);
    }
}
