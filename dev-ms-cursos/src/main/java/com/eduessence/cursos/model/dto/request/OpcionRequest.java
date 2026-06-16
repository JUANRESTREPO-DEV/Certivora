package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OpcionRequest {
    private Long id;

    @NotBlank
    @Size(max = 500)
    private String texto;

    private Boolean correcta;

    private Integer orden;
}
