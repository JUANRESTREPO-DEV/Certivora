package com.eduessence.certificados.service;

import com.eduessence.certificados.model.dto.request.EmitirRequest;
import com.eduessence.certificados.model.dto.response.CertificadoResponse;
import com.eduessence.certificados.model.dto.response.VerificacionResponse;

import java.util.List;

public interface CertificadoService {

    /** Emite el certificado: genera código, hash, QR, PDF, sube a S3 y notifica al usuario. */
    CertificadoResponse emitir(EmitirRequest request);

    /** Verificación pública por código. No requiere auth. */
    VerificacionResponse verificar(String codigo);

    /** URL pre-firmada para descargar el PDF. */
    String urlDescarga(String codigo);

    List<CertificadoResponse> misCertificados(Long usuarioId);

    /**
     * Recalcula el hash de verificación con el algoritmo determinístico
     * actual y lo persiste. Solo para reparar certificados emitidos antes
     * del fix del algoritmo (cuya fecha original con nanos no se puede
     * reconstruir).
     */
    CertificadoResponse repararHash(String codigo);
}
