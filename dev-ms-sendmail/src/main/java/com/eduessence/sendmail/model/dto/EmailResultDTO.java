package com.eduessence.sendmail.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResultDTO {
    private boolean ok;
    private String mensaje;
    private int exitosos;
    private int fallidos;
}
