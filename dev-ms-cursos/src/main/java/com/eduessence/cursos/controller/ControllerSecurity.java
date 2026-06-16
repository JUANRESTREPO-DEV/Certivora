package com.eduessence.cursos.controller;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helpers para leer la identidad inyectada por el gateway en los headers
 * {@code X-User-Id} y {@code X-User-Roles}.
 *
 * <p>Los roles privilegiados que pueden operar inscripciones de otros
 * usuarios son {@code ADMIN} y {@code GERENTE}.</p>
 */
final class ControllerSecurity {

    private static final Set<String> ROLES_ADMIN = Set.of("ADMIN", "GERENTE");

    private ControllerSecurity() { }

    /** Lee {@code X-User-Id}. Lanza 400 si está ausente o no es un número. */
    static Long parseUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Header X-User-Id ausente");
        }
        try {
            return Long.valueOf(header);
        } catch (NumberFormatException ex) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Header X-User-Id inválido");
        }
    }

    /** {@code true} si {@code X-User-Roles} contiene ADMIN o GERENTE. */
    static boolean esAdmin(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        // El gateway serializa el claim "roles" con .toString() de la
        // colección Java, así que puede llegar como CSV plano ("ADMIN,USER")
        // o con formato de array ("[ADMIN, USER]"). Normalizamos quitando
        // corchetes, comillas y espacios antes de partir por coma.
        String limpio = rolesHeader.replaceAll("[\\[\\]\"\\s]", "");
        Set<String> roles = Arrays.stream(limpio.split(","))
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        return roles.stream().anyMatch(ROLES_ADMIN::contains);
    }

    /** Lanza 403 si el usuario no tiene rol de admin. */
    static void requireAdmin(String rolesHeader) {
        if (!esAdmin(rolesHeader)) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Esta operación requiere rol ADMIN o GERENTE");
        }
    }
}
