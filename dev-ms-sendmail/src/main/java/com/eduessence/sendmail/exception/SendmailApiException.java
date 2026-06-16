package com.eduessence.sendmail.exception;

import lombok.Getter;

@Getter
public class SendmailApiException extends RuntimeException {

    private final ServerApiStatusCode statusCode;

    public SendmailApiException(ServerApiStatusCode statusCode) {
        super(statusCode.getDescripcion());
        this.statusCode = statusCode;
    }

    public SendmailApiException(ServerApiStatusCode statusCode, String mensaje) {
        super(mensaje);
        this.statusCode = statusCode;
    }

    public SendmailApiException(ServerApiStatusCode statusCode, Throwable cause) {
        super(statusCode.getDescripcion(), cause);
        this.statusCode = statusCode;
    }
}
