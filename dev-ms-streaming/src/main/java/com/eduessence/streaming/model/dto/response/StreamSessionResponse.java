package com.eduessence.streaming.model.dto.response;

import com.eduessence.streaming.model.enums.EstadoStream;
import com.eduessence.streaming.model.enums.ProviderTipo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamSessionResponse {
    private Long id;
    private Long cursoId;
    private Long sesionVirtualId;
    private Long instructorUsuarioId;
    private String titulo;
    private ProviderTipo provider;
    private EstadoStream estado;
    private String playbackUrl;
    private String recordingUrl;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Long duracionRealSegundos;
    private Integer viewersPeak;
}
