package com.eduessence.pagos.exception;

import lombok.Getter;

@Getter
public class PagosApiException extends RuntimeException {
    private final ServerApiStatusCode statusCode;

    public PagosApiException(ServerApiStatusCode statusCode) {
        super(statusCode.getDescripcion());
        this.statusCode = statusCode;
    }

    public PagosApiException(ServerApiStatusCode statusCode, String mensaje) {
        super(mensaje);
        this.statusCode = statusCode;
    }
}
