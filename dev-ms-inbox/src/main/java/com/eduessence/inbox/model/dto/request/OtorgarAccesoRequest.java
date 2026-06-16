package com.eduessence.inbox.model.dto.request;

import com.eduessence.inbox.model.enums.RolBuzon;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OtorgarAccesoRequest {
    @NotNull
    private Long usuarioId;

    @NotNull
    private RolBuzon rol;
}
