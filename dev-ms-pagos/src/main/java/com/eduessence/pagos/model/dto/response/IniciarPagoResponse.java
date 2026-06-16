package com.eduessence.pagos.model.dto.response;

import com.eduessence.pagos.model.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IniciarPagoResponse {
    private Long pagoId;
    private EstadoPago estado;
    private Integer montoOriginal;
    private Integer montoDescuento;
    private Integer montoCop;
    private String llave;
    private LocalDateTime reservaExpira;
    private boolean cursoGratis;
}
