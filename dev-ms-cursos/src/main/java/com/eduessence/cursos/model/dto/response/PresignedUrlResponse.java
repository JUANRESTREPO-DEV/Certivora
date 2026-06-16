package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {
    /** URL pre-firmada para hacer PUT directo al bucket. Expira en minutos. */
    private String uploadUrl;
    /** Key final dentro del bucket (el front la persistirá tras el PUT exitoso). */
    private String s3Key;
    /** URL pública/CDN para leer el objeto (puede requerir nueva firma para GET). */
    private String urlPublica;
    /** Segundos hasta que expire {@code uploadUrl}. */
    private Integer expiraEnSegundos;
}
