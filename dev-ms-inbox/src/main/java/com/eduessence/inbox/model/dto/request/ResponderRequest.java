package com.eduessence.inbox.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ResponderRequest {

    /** Si responde "a todos" agrega los Cc del mensaje original. */
    private Boolean responderATodos = Boolean.FALSE;

    @NotBlank
    private String cuerpoHtml;

    private String cuerpoTexto;

    private List<String> cc;
    private List<String> bcc;
}
