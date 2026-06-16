package com.eduessence.auth.exception;

import com.eduessence.auth.model.dto.GeneralResponseDTO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled error", ex);
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
