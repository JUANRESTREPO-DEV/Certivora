package com.eduessence.cursos.model.dto.response;

import com.eduessence.cursos.model.enums.TipoLeccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeccionResponse {
    private Long id;
    private Long seccionId;
    private String titulo;
    private String descripcion;
    private TipoLeccion tipo;
    private Integer orden;
    private Boolean bloqueante;
    private Integer duracionSegundos;
    private String recursoUrl;
    private String contenidoTexto;
    private Long examenId;
    private Long actividadId;
    private Long sesionVirtualId;
}
