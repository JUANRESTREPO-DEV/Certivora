package com.eduessence.inbox.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ComposerRequest {

    /** Buzón corporativo desde el que se envía (debe pertenecer a uno con acceso). */
    @NotNull
    private Long buzonId;

    @NotEmpty
    private List<String> to;

    private List<String> cc;
    private List<String> bcc;

    private String asunto;

    private String cuerpoHtml;
    private String cuerpoTexto;
}
