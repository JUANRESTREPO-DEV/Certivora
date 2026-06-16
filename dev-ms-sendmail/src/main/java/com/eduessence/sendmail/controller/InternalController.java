package com.eduessence.sendmail.controller;

import com.eduessence.sendmail.model.dto.EmailResultDTO;
import com.eduessence.sendmail.model.dto.EnviarConAdjuntoRequest;
import com.eduessence.sendmail.model.dto.EnviarEmailRequest;
import com.eduessence.sendmail.model.dto.GeneralResponseDTO;
import com.eduessence.sendmail.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos llamados por los otros micros vía Feign.
 * El gateway los expone sin auth (están en {@code public-paths}).
 */
@Tag(name = "Internal", description = "Envío de correos llamado por otros microservicios")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private static final String SERVICE = "sendmail-service";

    private final EmailService emailService;

    @Operation(summary = "Enviar correo dinámico desde template")
    @PostMapping("/enviar-email")
    public ResponseEntity<GeneralResponseDTO<EmailResultDTO>> enviarEmail(
            @Valid @RequestBody EnviarEmailRequest request
    ) {
        EmailResultDTO data = emailService.enviarDinamico(request);
        return ResponseEntity.ok(GeneralResponseDTO.<EmailResultDTO>builder()
                .statusCode(200).serviceName(SERVICE).message(data.getMensaje()).response(data).build());
    }

    @Operation(summary = "Enviar correo con adjuntos y/o múltiples destinatarios")
    @PostMapping("/enviar-con-adjunto")
    public ResponseEntity<GeneralResponseDTO<EmailResultDTO>> enviarConAdjunto(
            @Valid @RequestBody EnviarConAdjuntoRequest request
    ) {
        EmailResultDTO data = emailService.enviarConAdjuntos(request);
        return ResponseEntity.ok(GeneralResponseDTO.<EmailResultDTO>builder()
                .statusCode(200).serviceName(SERVICE).message(data.getMensaje()).response(data).build());
    }
}
