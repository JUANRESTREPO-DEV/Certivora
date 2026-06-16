package com.eduessence.auth.model.dto;

import com.eduessence.auth.model.enums.OverrideTipo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OverrideRequest {
    @NotNull
    private OverrideTipo tipo;
}
