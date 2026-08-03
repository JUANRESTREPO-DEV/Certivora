package com.eduessence.cursos.model.dto.response;

import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.model.enums.TipoParticipante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatriculaResponse {
    private Long id;
    private Long usuarioId;
    private Long cursoId;
    private String cursoNombre;

    private EstadoMatricula estado;
    private TipoParticipante tipo;

    /** Si está en EN_ESPERA, indica el puesto en la cola (1 = siguiente). */
    private Integer posicionEspera;

    /** Si está en PENDIENTE_PAGO, hasta cuándo se mantiene reservado el cupo. */
    private LocalDateTime reservaExpira;

    /** ID del pago vinculado (en dev-ms-pagos). Null si gratis o cortesía. */
    private Long pagoId;

    /** Precio COP congelado al inscribirse. Null si es cortesía o gratis. */
    private BigDecimal precioCopPagado;

    /** Token público de la escarapela — usable en /escarapela/{token}. Null si no aplica. */
    private String escarapelaToken;

    /** True si fue creada por admin sin cobro. */
    private Boolean cortesia;

    private Set<ModalidadTipo> modalidades;
    private BigDecimal progresoPct;
    private BigDecimal notaFinal;

    private LocalDateTime fechaInscripcion;
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaCancelacion;
    private String canceladoMotivo;

    private Boolean certificadoEmitido;
}
