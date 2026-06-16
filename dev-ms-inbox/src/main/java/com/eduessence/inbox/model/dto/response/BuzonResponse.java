package com.eduessence.inbox.model.dto.response;

import com.eduessence.inbox.model.enums.TipoBuzon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuzonResponse {
    private Long id;
    private String direccion;
    private String nombreMostrar;
    private TipoBuzon tipo;
    private Long referenciaId;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime expiraEn;
    private List<String> forwardExternos;
    private String firma;
    private List<String> aliases;
    private List<AccesoResponse> accesos;
    private Long noLeidos;
    private Long creadoPorUsuarioId;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccesoResponse {
        private Long usuarioId;
        private String rol;
    }
}
