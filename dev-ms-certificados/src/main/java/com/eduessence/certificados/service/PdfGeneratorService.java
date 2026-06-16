package com.eduessence.certificados.service;

import com.eduessence.certificados.model.entity.DiplomaTemplate;

public interface PdfGeneratorService {

    /**
     * Genera el PDF del certificado combinando el template base con los
     * datos del usuario y el QR. Devuelve los bytes del PDF final.
     *
     * @param numeroDocumento si viene, se imprime en la posición {@code documento}
     *                        del template con el prefix definido por el editor
     *                        (típicamente "NIUP ").
     */
    byte[] generar(DiplomaTemplate template,
                   String nombreCompleto,
                   String numeroDocumento,
                   String nombreCurso,
                   String codigo,
                   String fechaFormateada,
                   byte[] qrPng);
}
