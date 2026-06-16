package com.eduessence.inbox.model.dto.request;

import com.eduessence.inbox.model.enums.TipoBuzon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrearBuzonRequest {

    @NotBlank
    @Size(max = 150)
    private String direccion;

    @Size(max = 150)
    private String nombreMostrar;

    @NotNull
    private TipoBuzon tipo;

    private Long referenciaId;

    @Size(max = 500)
    private String descripcion;

    /** Direcciones externas a reenviar (Gmail/Outlook personal del responsable). */
    private List<String> forwardExternos;

    private String firma;

    private LocalDateTime expiraEn;
}
