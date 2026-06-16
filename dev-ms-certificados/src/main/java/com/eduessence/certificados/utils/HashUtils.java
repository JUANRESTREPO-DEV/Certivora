package com.eduessence.certificados.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class HashUtils {

    /**
     * Formato determinístico para la fecha dentro del hash. Sin fracciones
     * de segundo — así sobrevive el round-trip por MySQL DATETIME(6) sin
     * importar si los micros guardados terminan en 0 (en cuyo caso
     * {@link LocalDateTime#toString()} omite la parte fraccional y el hash
     * recalculado deja de coincidir con el original).
     */
    private static final DateTimeFormatter HASH_FECHA_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private HashUtils() {}

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    /**
     * Calcula el hash de verificación de un certificado a partir de su
     * tripleta (usuarioId, cursoId, fechaEmision) más un secreto. La fecha
     * se normaliza a segundos y al formato {@code yyyy-MM-dd'T'HH:mm:ss}
     * para que el cálculo sea estable a través del round-trip por la BD.
     */
    public static String hashCertificado(Long usuarioId, Long cursoId,
                                         LocalDateTime fechaEmision, String secret) {
        String fecha = fechaEmision.truncatedTo(ChronoUnit.SECONDS).format(HASH_FECHA_FMT);
        return sha256(usuarioId + "|" + cursoId + "|" + fecha + "|" + secret);
    }

    /**
     * @deprecated Variante histórica que toma la fecha como string
     *             arbitrario. Se mantiene solo para validar certificados
     *             emitidos antes del cambio a formato determinístico. Los
     *             nuevos deben usar {@link #hashCertificado(Long, Long, LocalDateTime, String)}.
     */
    @Deprecated
    public static String hashCertificadoLegacy(Long usuarioId, Long cursoId,
                                               String fechaEmisionRaw, String secret) {
        return sha256(usuarioId + "|" + cursoId + "|" + fechaEmisionRaw + "|" + secret);
    }
}
