package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeccionResponse {
    private Long id;
    private Long cursoId;
    private String titulo;
    private String descripcion;
    private Integer orden;
    private Boolean bloqueante;
    private List<LeccionResponse> lecciones;
}
