package com.eduessence.inbox.exception;

import com.eduessence.inbox.model.dto.GeneralResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICE = "inbox-service";

    @ExceptionHandler(InboxApiException.class)
    public ResponseEntity<GeneralResponseDTO<Void>> handleInbox(InboxApiException ex) {
        var s = ex.getStatusCode();
        log.warn("[{}] {}", s.getCodigo(), ex.getMessage());
        return ResponseEntity.status(s.getHttpStatus()).body(GeneralResponseDTO.<Void>builder()
                .statusCode(s.getHttpStatus().value())
                .serviceName(SERVICE)
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponseDTO<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(GeneralResponseDTO.<Void>builder()
                .statusCode(400).serviceName(SERVICE).message(msg).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponseDTO<Void>> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.internalServerError().body(GeneralResponseDTO.<Void>builder()
                .statusCode(500).serviceName(SERVICE).message("Error interno").build());
    }
}
