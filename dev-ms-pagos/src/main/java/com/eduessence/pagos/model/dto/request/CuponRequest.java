package com.eduessence.pagos.model.dto.request;

import com.eduessence.pagos.model.enums.AlcanceCupon;
import com.eduessence.pagos.model.enums.TipoDescuento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payload de creación / edición de cupones.
 *
 * Notas:
 *  - {@code codigo} se guarda siempre en mayúsculas (se normaliza en backend).
 *  - Para {@code PORCENTAJE} el valor debe estar entre 1 y 100.
 *  - Para {@code MONTO_FIJO} el valor está en COP enteros.
 *  - {@code usuarioAsignadoId} es obligatorio solo para {@code USUARIO_UNICO}.
 *  - {@code usosMaximos} es obligatorio solo para {@code MULTI_USO}.
 *  - {@code cursoId} opcional → si null el cupón aplica a cualquier curso.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuponRequest {

    @NotBlank
    @Size(max = 40)
    private String codigo;

    @Size(max = 200)
    private String descripcion;

    @NotNull
    private TipoDescuento tipoDescuento;

    @NotNull
    @Positive
    private Integer valor;

    @NotNull
    private AlcanceCupon alcance;

    private Long usuarioAsignadoId;

    @Positive
    private Integer usosMaximos;

    private Long cursoId;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;
}
