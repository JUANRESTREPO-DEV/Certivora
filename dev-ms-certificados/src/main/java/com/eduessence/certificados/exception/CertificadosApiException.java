package com.eduessence.certificados.exception;

import lombok.Getter;

@Getter
public class CertificadosApiException extends RuntimeException {
    private final ServerApiStatusCode statusCode;

    public CertificadosApiException(ServerApiStatusCode statusCode) {
        super(statusCode.getDescripcion());
        this.statusCode = statusCode;
    }

    public CertificadosApiException(ServerApiStatusCode statusCode, String mensaje) {
        super(mensaje);
        this.statusCode = statusCode;
    }

    public CertificadosApiException(ServerApiStatusCode statusCode, Throwable cause) {
        super(statusCode.getDescripcion(), cause);
        this.statusCode = statusCode;
    }
}
