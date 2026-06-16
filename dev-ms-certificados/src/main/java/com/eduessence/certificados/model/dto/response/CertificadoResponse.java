package com.eduessence.certificados.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificadoResponse {
    private Long id;
    private String codigo;
    private Long usuarioId;
    private Long cursoId;
    private String nombreCompleto;
    private String nombreCurso;
    private String urlPdf;
    private String qrUrl;
    private LocalDateTime fechaEmision;
}
