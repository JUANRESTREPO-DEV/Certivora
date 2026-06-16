package com.eduessence.inbox.controller;

import com.eduessence.inbox.model.dto.GeneralResponseDTO;
import com.eduessence.inbox.service.InboundMessageProcessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint interno para que SNS HTTPS subscription pueda entregar
 * notificaciones directamente al MS (alternativa a SQS polling).
 *
 * Ojo: la ruta /inbox/internal/** está en la lista de public-paths del gateway,
 * por lo que NO requiere JWT. Asegurate de:
 *   - Restringir el SG/firewall a IPs de SNS, o
 *   - Validar la firma SNS en producción (extra fase 2).
 */
@Tag(name = "Internal")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private static final String SERVICE = "inbox-service";
    private final InboundMessageProcessor processor;

    /**
     * Recibe el body crudo de una notificación SNS (Subscription Confirmation
     * o Notification). El processor sabe distinguirlas.
     */
    @PostMapping(value = "/sns/inbound", consumes = "*/*")
    public ResponseEntity<GeneralResponseDTO<Void>> sns(@RequestBody String body,
                                                        @RequestHeader(value = "x-amz-sns-message-type", required = false) String type) {
        // En "SubscriptionConfirmation" el body trae un SubscribeURL — habría que pegarle GET para confirmar.
        // Lo dejamos pendiente como fase 2: por defecto usamos SQS polling.
        processor.procesarNotificacionSes(body);
        return ResponseEntity.ok(GeneralResponseDTO.<Void>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").build());
    }
}
