package com.eduessence.pagos.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Genera llaves Bre-B / códigos de referencia con formato
 * {@code EDU-YYYYMMDD-NNNN}. El contador se reinicia por día en memoria
 * (suficiente para v1; para HA migrar a Redis o secuencia BD).
 */
@Component
public class LlaveGenerador {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");

    private LocalDate dia = LocalDate.now();
    private final AtomicInteger contador = new AtomicInteger(0);

    public synchronized String generar() {
        LocalDate hoy = LocalDate.now();
        if (!hoy.equals(dia)) {
            dia = hoy;
            contador.set(0);
        }
        int n = contador.incrementAndGet();
        return "EDU-" + dia.format(DF) + "-" + String.format("%04d", n);
    }
}
