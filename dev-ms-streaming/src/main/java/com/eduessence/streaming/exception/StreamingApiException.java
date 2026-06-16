package com.eduessence.streaming.exception;

import lombok.Getter;

@Getter
public class StreamingApiException extends RuntimeException {
    private final ServerApiStatusCode statusCode;

    public StreamingApiException(ServerApiStatusCode statusCode) {
        super(statusCode.getDescripcion());
        this.statusCode = statusCode;
    }

    public StreamingApiException(ServerApiStatusCode statusCode, String mensaje) {
        super(mensaje);
        this.statusCode = statusCode;
    }

    public StreamingApiException(ServerApiStatusCode statusCode, Throwable cause) {
        super(statusCode.getDescripcion(), cause);
        this.statusCode = statusCode;
    }
}
