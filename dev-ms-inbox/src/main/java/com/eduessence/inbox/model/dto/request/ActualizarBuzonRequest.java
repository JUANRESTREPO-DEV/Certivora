package com.eduessence.inbox.model.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActualizarBuzonRequest {
    private String nombreMostrar;
    private String descripcion;
    private List<String> forwardExternos;
    private String firma;
    private LocalDateTime expiraEn;
    private Boolean activo;
}
