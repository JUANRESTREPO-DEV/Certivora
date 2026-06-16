package com.eduessence.streaming.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerApiStatusCode {
    OK(HttpStatus.OK, "STR-000", "OK"),
    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "STR-400", "Datos inválidos"),
    SESION_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "STR-404-1", "Sesión de streaming no existe"),
    STREAM_KEY_INVALIDA(HttpStatus.UNAUTHORIZED, "STR-401-1", "Stream key inválida"),
    PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "STR-502-1", "Error del proveedor de streaming"),
    WEBHOOK_INVALIDO(HttpStatus.BAD_REQUEST, "STR-400-1", "Payload de webhook inválido"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "STR-500", "Error interno del servicio");

    private final HttpStatus httpStatus;
    private final String codigo;
    private final String descripcion;

    ServerApiStatusCode(HttpStatus httpStatus, String codigo, String descripcion) {
        this.httpStatus = httpStatus;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
}
