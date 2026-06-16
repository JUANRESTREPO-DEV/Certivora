package com.eduessence.pagos.model.dto.response;

import com.eduessence.pagos.model.enums.EstadoPago;
import com.eduessence.pagos.model.enums.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponse {
    private Long id;
    private Long usuarioId;
    private Long cursoId;
    private Integer montoOriginal;
    private Integer montoDescuento;
    private Integer montoCop;
    private String moneda;
    private MetodoPago metodo;
    private EstadoPago estado;
    private String llave;
    private LocalDateTime reservaExpira;
    private Long cuponId;
    private String comprobanteUrl;
    private String observacion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacion;
}
