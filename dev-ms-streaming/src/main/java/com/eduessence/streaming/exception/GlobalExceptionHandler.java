package com.eduessence.streaming.exception;

import com.eduessence.streaming.model.dto.GeneralResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICE = "streaming-service";

    @ExceptionHandler(StreamingApiException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handle(StreamingApiException ex) {
        ServerApiStatusCode c = ex.getStatusCode();
        log.warn("StreamingApiException [{}]: {}", c.getCodigo(), ex.getMessage());
        return ResponseEntity.status(c.getHttpStatus()).header("X-Status-Code", c.getCodigo())
                .body(GeneralResponseDTO.builder().statusCode(c.getHttpStatus().value())
                        .serviceName(SERVICE).message(ex.getMessage()).codigoError(c.getCodigo()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ServerApiStatusCode c = ServerApiStatusCode.DATOS_INVALIDOS;
        return ResponseEntity.status(c.getHttpStatus()).header("X-Status-Code", c.getCodigo())
                .body(GeneralResponseDTO.builder().statusCode(c.getHttpStatus().value())
                        .serviceName(SERVICE).message(detalle).codigoError(c.getCodigo()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponseDTO<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled", ex);
        ServerApiStatusCode c = ServerApiStatusCode.ERROR_INTERNO;
        return ResponseEntity.status(c.getHttpStatus()).header("X-Status-Code", c.getCodigo())
                .body(GeneralResponseDTO.builder().statusCode(c.getHttpStatus().value())
                        .serviceName(SERVICE).message(c.getDescripcion()).codigoError(c.getCodigo()).build());
    }
}
