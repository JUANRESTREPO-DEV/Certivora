package com.eduessence.auth.exception;

import com.eduessence.auth.model.dto.GeneralResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICE = "authenticate-service";

    @ExceptionHandler(AuthApiException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleAuthApi(AuthApiException ex) {
        ServerApiStatusCode code = ex.getStatusCode();
        log.warn("AuthApiException [{}]: {}", code.getCodigo(), ex.getMessage());
        return ResponseEntity.status(code.getHttpStatus())
                .header("X-Status-Code", code.getCodigo())
                .body(GeneralResponseDTO.builder()
                        .statusCode(code.getHttpStatus().value())
                        .serviceName(SERVICE)
                        .message(ex.getMessage())
                        .codigoError(code.getCodigo())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ServerApiStatusCode code = ServerApiStatusCode.DATOS_INVALIDOS;
        return ResponseEntity.status(code.getHttpStatus())
                .header("X-Status-Code", code.getCodigo())
                .body(GeneralResponseDTO.builder()
                        .statusCode(code.getHttpStatus().value())
                        .serviceName(SERVICE)
                        .message(detalle)
                        .codigoError(code.getCodigo())
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleConstraint(ConstraintViolationException ex) {
        ServerApiStatusCode code = ServerApiStatusCode.DATOS_INVALIDOS;
        return ResponseEntity.status(code.getHttpStatus())
                .header("X-Status-Code", code.getCodigo())
                .body(GeneralResponseDTO.builder()
                        .statusCode(code.getHttpStatus().value())
                        .serviceName(SERVICE)
                        .message(ex.getMessage())
                        .codigoError(code.getCodigo())
                        .build());
    }

    /**
     * 405 — método incorrecto. Ej: alguien hace GET a un endpoint que sólo
     * acepta POST. No es un error del servidor; loguear solo warn con la URL
     * para debug, sin stacktrace.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        log.warn("405 Method Not Allowed: {} {} — permitidos {}",
                req.getMethod(), req.getRequestURI(), ex.getSupportedHttpMethods());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header("X-Status-Code", "AUTH-405")
                .body(GeneralResponseDTO.builder()
                        .statusCode(405)
                        .serviceName(SERVICE)
                        .message("Método " + req.getMethod() + " no soportado para " + req.getRequestURI())
                        .codigoError("AUTH-405")
                        .build());
    }

    /** 404 — path que no matchea ningún controller. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest req) {
        log.warn("404 No Resource: {} {}", req.getMethod(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header("X-Status-Code", "AUTH-404")
                .body(GeneralResponseDTO.builder()
                        .statusCode(404)
                        .serviceName(SERVICE)
                        .message("Ruta no encontrada: " + req.getRequestURI())
                        .codigoError("AUTH-404")
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        ServerApiStatusCode code = ServerApiStatusCode.ERROR_INTERNO;
        return ResponseEntity.status(code.getHttpStatus())
                .header("X-Status-Code", code.getCodigo())
                .body(GeneralResponseDTO.builder()
                        .statusCode(code.getHttpStatus().value())
                        .serviceName(SERVICE)
                        .message(code.getDescripcion())
                        .codigoError(code.getCodigo())
                        .build());
    }
}
