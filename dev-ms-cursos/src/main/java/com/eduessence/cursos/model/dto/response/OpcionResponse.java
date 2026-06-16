package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionResponse {
    private Long id;
    private Long preguntaId;
    private String texto;
    private Boolean correcta;
    private Integer orden;
}
