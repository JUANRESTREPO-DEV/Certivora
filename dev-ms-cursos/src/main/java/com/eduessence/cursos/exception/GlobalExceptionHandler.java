package com.eduessence.cursos.exception;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICE = "cursos-service";

    @ExceptionHandler(CursosApiException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handle(CursosApiException ex) {
        ServerApiStatusCode c = ex.getStatusCode();
        log.warn("CursosApiException [{}]: {}", c.getCodigo(), ex.getMessage());
        return ResponseEntity.status(c.getHttpStatus())
                .header("X-Status-Code", c.getCodigo())
                .body(GeneralResponseDTO.builder()
                        .statusCode(c.getHttpStatus().value())
                        .serviceName(SERVICE)
                        .message(ex.getMessage())
                        .codigoError(c.getCodigo()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ServerApiStatusCode c = ServerApiStatusCode.DATOS_INVALIDOS;
        return ResponseEntity.status(c.getHttpStatus()).header("X-Status-Code", c.getCodigo())
                .body(GeneralResponseDTO.builder()
                        .statusCode(c.getHttpStatus().value()).serviceName(SERVICE)
                        .message(detalle).codigoError(c.getCodigo()).build());
    }

    /**
     * El browser cerró la conexión mientras streamábamos un recurso (típico de
     * vídeos donde el usuario navega antes de que termine la descarga). El
     * response ya está committed con Content-Type del binario; no podemos —
     * ni debemos — serializar un DTO JSON encima. Devolver {@code null} le
     * dice a Spring que no haga nada más con la respuesta.
     */
    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    public ResponseEntity<Void> handleClientAbort(Exception ex) {
        log.debug("Cliente abortó la conexión: {}", ex.getMessage());
        return null;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleUnexpected(Exception ex) {
        // Por si el ClientAbort llega envuelto en otra excepción, lo detectamos
        // por la causa raíz y lo silenciamos sin ensuciar el log con un stack.
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof ClientAbortException
                    || cause instanceof AsyncRequestNotUsableException) {
                log.debug("Cliente abortó la conexión (anidado): {}", ex.getMessage());
                return null;
            }
            cause = cause.getCause();
        }
        log.error("Unhandled", ex);
        ServerApiStatusCode c = ServerApiStatusCode.ERROR_INTERNO;
        return ResponseEntity.status(c.getHttpStatus()).header("X-Status-Code", c.getCodigo())
                .body(GeneralResponseDTO.builder()
                        .statusCode(c.getHttpStatus().value()).serviceName(SERVICE)
                        .message(c.getDescripcion()).codigoError(c.getCodigo()).build());
    }
}
