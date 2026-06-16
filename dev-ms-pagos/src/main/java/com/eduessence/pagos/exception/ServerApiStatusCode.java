package com.eduessence.pagos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerApiStatusCode {

    OK(HttpStatus.OK, "PAG-000", "OK"),
    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "PAG-400", "Datos inválidos"),
    PAGO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "PAG-404-1", "Pago no existe"),

    CUPON_NO_EXISTE(HttpStatus.NOT_FOUND, "CUP-001", "Cupón no existe"),
    CUPON_INACTIVO(HttpStatus.BAD_REQUEST, "CUP-002", "Cupón inactivo"),
    CUPON_FUERA_VIGENCIA(HttpStatus.BAD_REQUEST, "CUP-003", "Cupón fuera de vigencia"),
    CUPON_NO_ASIGNADO(HttpStatus.FORBIDDEN, "CUP-004", "Cupón no asignado a este usuario"),
    CUPON_AGOTADO(HttpStatus.CONFLICT, "CUP-005", "Cupón agotado"),
    CUPON_YA_USADO(HttpStatus.CONFLICT, "CUP-006", "Ya usaste este cupón"),
    CUPON_NO_APLICA_CURSO(HttpStatus.BAD_REQUEST, "CUP-007", "Cupón no aplica a este curso"),
    CUPON_CONFIG_INVALIDA(HttpStatus.BAD_REQUEST, "CUP-008", "Configuración de cupón inválida"),
    CUPON_CODIGO_DUPLICADO(HttpStatus.CONFLICT, "CUP-009", "Ya existe un cupón con ese código"),
    CUPON_TIENE_USOS(HttpStatus.CONFLICT, "CUP-010", "El cupón ya tiene usos: no se pueden cambiar tipo/valor/alcance, solo desactivarlo"),

    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "PAG-500", "Error interno del servicio");

    private final HttpStatus httpStatus;
    private final String codigo;
    private final String descripcion;

    ServerApiStatusCode(HttpStatus httpStatus, String codigo, String descripcion) {
        this.httpStatus = httpStatus;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
}
