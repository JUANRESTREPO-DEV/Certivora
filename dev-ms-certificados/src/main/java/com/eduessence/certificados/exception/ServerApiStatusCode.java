package com.eduessence.certificados.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerApiStatusCode {
    OK(HttpStatus.OK, "CER-000", "OK"),
    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "CER-400", "Datos inválidos"),
    TEMPLATE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "CER-404-1", "Template de diploma no existe"),
    CERTIFICADO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "CER-404-2", "Certificado no existe"),
    MATRICULA_NO_APROBADA(HttpStatus.BAD_REQUEST, "CER-400-1", "La matrícula no está aprobada"),
    CERTIFICADO_YA_EMITIDO(HttpStatus.CONFLICT, "CER-409-1", "Ya existe un certificado para esta matrícula"),
    HASH_INVALIDO(HttpStatus.BAD_REQUEST, "CER-400-2", "Hash de verificación no coincide"),
    ERROR_PDF(HttpStatus.INTERNAL_SERVER_ERROR, "CER-500-1", "Error generando PDF"),
    ERROR_S3(HttpStatus.INTERNAL_SERVER_ERROR, "CER-500-2", "Error subiendo PDF a S3"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "CER-500", "Error interno");

    private final HttpStatus httpStatus;
    private final String codigo;
    private final String descripcion;

    ServerApiStatusCode(HttpStatus httpStatus, String codigo, String descripcion) {
        this.httpStatus = httpStatus;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
}
