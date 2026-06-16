package com.eduessence.auth.exception;

import lombok.Getter;

@Getter
public class AuthApiException extends RuntimeException {

    private final ServerApiStatusCode statusCode;

    public AuthApiException(ServerApiStatusCode statusCode) {
        super(statusCode.getDescripcion());
        this.statusCode = statusCode;
    }

    public AuthApiException(ServerApiStatusCode statusCode, String customMessage) {
        super(customMessage);
        this.statusCode = statusCode;
    }
}
