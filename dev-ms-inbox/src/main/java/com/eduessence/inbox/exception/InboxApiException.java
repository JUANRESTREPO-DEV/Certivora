package com.eduessence.inbox.exception;

import lombok.Getter;

@Getter
public class InboxApiException extends RuntimeException {

    private final ServerApiStatusCode statusCode;

    public InboxApiException(ServerApiStatusCode statusCode) {
        super(statusCode.getDescripcion());
        this.statusCode = statusCode;
    }

    public InboxApiException(ServerApiStatusCode statusCode, String detalle) {
        super(detalle);
        this.statusCode = statusCode;
    }

    public InboxApiException(ServerApiStatusCode statusCode, Throwable cause) {
        super(statusCode.getDescripcion(), cause);
        this.statusCode = statusCode;
    }
}
