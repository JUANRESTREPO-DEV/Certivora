package com.eduessence.sendmail.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerApiStatusCode {

    OK(HttpStatus.OK, "SM-000", "OK"),

    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "SM-400", "Datos inválidos"),
    TEMPLATE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "SM-404-1", "Template no existe o está inactivo"),
    DESTINATARIOS_VACIOS(HttpStatus.BAD_REQUEST, "SM-400-1", "Debe especificar al menos un destinatario"),
    VARIABLE_REQUERIDA_FALTANTE(HttpStatus.BAD_REQUEST, "SM-400-2", "Falta una variable requerida por el template"),
    ERROR_RENDER_TEMPLATE(HttpStatus.INTERNAL_SERVER_ERROR, "SM-500-1", "Error al renderizar el template Velocity"),
    ERROR_S3(HttpStatus.INTERNAL_SERVER_ERROR, "SM-500-2", "Error descargando template desde S3"),
    ERROR_SMTP(HttpStatus.INTERNAL_SERVER_ERROR, "SM-500-3", "Error enviando correo por SMTP"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "SM-500", "Error interno del servicio");

    private final HttpStatus httpStatus;
    private final String codigo;
    private final String descripcion;

    ServerApiStatusCode(HttpStatus httpStatus, String codigo, String descripcion) {
        this.httpStatus = httpStatus;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
}
