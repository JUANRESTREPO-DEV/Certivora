package com.eduessence.certificados.service;

public interface QrGeneratorService {

    /**
     * Genera un QR PNG con el contenido dado.
     * @param contenido típicamente la URL pública de verificación
     * @param size lado del cuadrado en pixeles
     * @return bytes del PNG
     */
    byte[] generar(String contenido, int size);
}
