package com.eduessence.inbox.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearAliasRequest {
    @NotNull
    private Long buzonId;

    @NotBlank
    private String direccion;
}
