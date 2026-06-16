package com.eduessence.inbox.common;

import com.eduessence.inbox.exception.InboxApiException;
import com.eduessence.inbox.exception.ServerApiStatusCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lee los headers X-User-Id / X-User-Email / X-User-Roles inyectados por el
 * apigateway tras validar el JWT.
 *
 * Reglas:
 *  - {@link #usuarioId()} lanza 401 si no viene el header.
 *  - {@link #requireRol(String...)} lanza 403 si el rol no está.
 *  - {@link #esAdminOGerente()} es el atajo usado en BuzonController/InternalController.
 */
@Component
@RequiredArgsConstructor
public class RequestContext {

    private final HttpServletRequest request;

    public Long usuarioId() {
        String v = request.getHeader("X-User-Id");
        if (v == null || v.isBlank())
            throw new InboxApiException(ServerApiStatusCode.USUARIO_NO_AUTENTICADO);
        try {
            return Long.valueOf(v);
        } catch (NumberFormatException ex) {
            throw new InboxApiException(ServerApiStatusCode.USUARIO_NO_AUTENTICADO);
        }
    }

    public String email() {
        String v = request.getHeader("X-User-Email");
        return v == null ? "" : v;
    }

    public Set<String> roles() {
        String v = request.getHeader("X-User-Roles");
        if (v == null || v.isBlank()) return Collections.emptySet();
        return Arrays.stream(v.replace("[", "").replace("]", "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    public boolean tieneRol(String... rolesEsperados) {
        Set<String> mios = roles();
        for (String r : rolesEsperados) {
            if (mios.contains(r.toUpperCase())) return true;
        }
        return false;
    }

    public boolean esAdminOGerente() {
        return tieneRol("ADMIN", "GERENTE");
    }

    public void requireRol(String... rolesEsperados) {
        if (!tieneRol(rolesEsperados))
            throw new InboxApiException(ServerApiStatusCode.ROL_INSUFICIENTE,
                    "Requiere alguno de: " + List.of(rolesEsperados));
    }

    public void requireAdminOGerente() {
        if (!esAdminOGerente())
            throw new InboxApiException(ServerApiStatusCode.ROL_INSUFICIENTE,
                    "Solo ADMIN o GERENTE pueden ejecutar esta acción");
    }
}
