package com.eduessence.sendmail.exception;

import com.eduessence.sendmail.model.dto.GeneralResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICE = "sendmail-service";

    @ExceptionHandler(SendmailApiException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handle(SendmailApiException ex) {
        ServerApiStatusCode code = ex.getStatusCode();
        log.warn("SendmailApiException [{}]: {}", code.getCodigo(), ex.getMessage());
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
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
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
