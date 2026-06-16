package com.eduessence.pagos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuponUsoResponse {
    private Long id;
    private Long cuponId;
    private Long pagoId;
    private Long usuarioId;
    private Long cursoId;
    private Integer montoDescuento;
    private LocalDateTime fechaUso;
}
