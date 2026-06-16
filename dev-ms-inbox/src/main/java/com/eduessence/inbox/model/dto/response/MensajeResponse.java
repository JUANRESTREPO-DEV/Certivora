package com.eduessence.inbox.model.dto.response;

import com.eduessence.inbox.model.enums.Carpeta;
import com.eduessence.inbox.model.enums.EstadoMensaje;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeResponse {
    private Long id;
    private Long buzonId;
    private String remitenteEmail;
    private String remitenteNombre;
    private String asunto;
    private String snippet;
    private LocalDateTime fechaRecibido;
    private EstadoMensaje estado;
    private Carpeta carpeta;
    private Boolean leido;
    private Boolean destacado;
    private Boolean tieneAdjuntos;
    private String threadId;
}
