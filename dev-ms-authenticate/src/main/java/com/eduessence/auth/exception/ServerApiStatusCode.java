package com.eduessence.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerApiStatusCode {

    OK(HttpStatus.OK, "AUTH-000", "OK"),

    // 4xx
    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "AUTH-400", "Datos inválidos"),
    EMAIL_DUPLICADO(HttpStatus.CONFLICT, "AUTH-409-1", "El email ya está registrado"),
    USERNAME_DUPLICADO(HttpStatus.CONFLICT, "AUTH-409-2", "El username ya está en uso"),
    USUARIO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "AUTH-404-1", "El usuario no existe"),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "AUTH-401-1", "Credenciales inválidas"),
    USUARIO_BLOQUEADO(HttpStatus.FORBIDDEN, "AUTH-403-1", "Usuario bloqueado temporalmente"),
    USUARIO_INACTIVO(HttpStatus.FORBIDDEN, "AUTH-403-2", "Usuario inactivo"),
    TOKEN_INVALIDO(HttpStatus.UNAUTHORIZED, "AUTH-401-2", "Token inválido o expirado"),
    TOKEN_RESET_INVALIDO(HttpStatus.BAD_REQUEST, "AUTH-400-1", "Token de recuperación inválido o expirado"),
    PASSWORD_DEBIL(HttpStatus.BAD_REQUEST, "AUTH-400-2", "La contraseña no cumple los requisitos"),
    ROL_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "AUTH-404-2", "El rol no existe"),

    // 5xx
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-500", "Error interno del servicio"),
    ERROR_NOTIFICACION(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-500-1", "No se pudo enviar la notificación");

    private final HttpStatus httpStatus;
    private final String codigo;
    private final String descripcion;

    ServerApiStatusCode(HttpStatus httpStatus, String codigo, String descripcion) {
        this.httpStatus = httpStatus;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
}
