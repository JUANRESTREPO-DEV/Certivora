package com.eduessence.streaming.model.dto.request;

import com.eduessence.streaming.model.enums.EventoViewer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ViewerEventRequest {

    @NotNull
    private EventoViewer evento;

    @PositiveOrZero
    private Integer segundosAcumulados;

    private String sessionToken;
}
