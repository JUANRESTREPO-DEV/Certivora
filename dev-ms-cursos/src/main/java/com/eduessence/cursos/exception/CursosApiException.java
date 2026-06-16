package com.eduessence.cursos.exception;

import lombok.Getter;

@Getter
public class CursosApiException extends RuntimeException {
    private final ServerApiStatusCode statusCode;

    public CursosApiException(ServerApiStatusCode statusCode) {
        super(statusCode.getDescripcion());
        this.statusCode = statusCode;
    }

    public CursosApiException(ServerApiStatusCode statusCode, String mensaje) {
        super(mensaje);
        this.statusCode = statusCode;
    }
}
