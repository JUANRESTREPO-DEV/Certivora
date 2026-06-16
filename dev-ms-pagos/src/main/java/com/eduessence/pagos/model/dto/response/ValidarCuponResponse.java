package com.eduessence.pagos.model.dto.response;

import com.eduessence.pagos.model.enums.TipoDescuento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCuponResponse {
    private boolean valido;
    private String codigo;
    private String descripcion;
    private TipoDescuento tipoDescuento;
    private Integer valor;
    private Integer montoOriginal;
    private Integer montoDescuento;
    private Integer montoFinal;
    private boolean esCursoGratisTrasCupon;
}
