package com.eduessence.inbox.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerApiStatusCode {
    OK(HttpStatus.OK, "INB-000", "OK"),
    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "INB-400", "Datos inválidos"),
    DIRECCION_DUPLICADA(HttpStatus.CONFLICT, "INB-409-1", "La dirección ya existe (buzón o alias)"),
    DOMINIO_INVALIDO(HttpStatus.BAD_REQUEST, "INB-400-1", "La dirección debe pertenecer al dominio corporativo"),
    BUZON_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "INB-404-1", "Buzón no existe"),
    BUZON_INACTIVO(HttpStatus.BAD_REQUEST, "INB-400-2", "El buzón está inactivo"),
    BUZON_SOLO_SALIDA(HttpStatus.BAD_REQUEST, "INB-400-3", "El buzón es solo salida y no acepta mensajes entrantes"),
    MENSAJE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "INB-404-2", "Mensaje no existe"),
    ALIAS_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "INB-404-3", "Alias no existe"),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "INB-403-1", "No tienes acceso a este buzón"),
    ROL_INSUFICIENTE(HttpStatus.FORBIDDEN, "INB-403-2", "Tu rol no permite esta acción (requiere ADMIN o GERENTE)"),
    USUARIO_NO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "INB-401-1", "Falta header X-User-Id del gateway"),
    ERROR_S3(HttpStatus.INTERNAL_SERVER_ERROR, "INB-500-1", "Error en S3"),
    ERROR_SES(HttpStatus.INTERNAL_SERVER_ERROR, "INB-500-2", "Error enviando con SES"),
    ERROR_PARSEO_MIME(HttpStatus.INTERNAL_SERVER_ERROR, "INB-500-3", "Error parseando el mensaje MIME"),
    ERROR_SQS(HttpStatus.INTERNAL_SERVER_ERROR, "INB-500-4", "Error consumiendo de SQS"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "INB-500", "Error interno");

    private final HttpStatus httpStatus;
    private final String codigo;
    private final String descripcion;

    ServerApiStatusCode(HttpStatus httpStatus, String codigo, String descripcion) {
        this.httpStatus = httpStatus;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
}
