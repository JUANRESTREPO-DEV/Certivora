package com.eduessence.pagos.model.dto.response;

import com.eduessence.pagos.model.enums.AlcanceCupon;
import com.eduessence.pagos.model.enums.TipoDescuento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuponResponse {

    private Long id;
    private String codigo;
    private String descripcion;
    private TipoDescuento tipoDescuento;
    private Integer valor;
    private AlcanceCupon alcance;
    private Long usuarioAsignadoId;
    private Integer usosMaximos;
    private Integer usosActuales;
    private Long cursoId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Boolean activo;
    private Long creadoPorUsuarioId;
    private LocalDateTime fechaCreacion;
}
