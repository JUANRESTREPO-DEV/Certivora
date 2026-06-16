package com.eduessence.pagos.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Sube y sirve los comprobantes de transferencia bancaria que los usuarios
 * cargan para que el admin verifique sus pagos manuales.
 */
public interface ComprobanteStorageService {

    /**
     * Sube el archivo a S3 y devuelve la URL pública (proxy a través del
     * backend para evitar exponer el bucket).
     */
    SubidaResult subir(Long pagoId, MultipartFile file);

    /**
     * Lee el binario desde S3 y lo retorna como bytes + content-type, para
     * que el controller los sirva al browser.
     */
    Descarga descargar(String s3Key);

    record SubidaResult(String s3Key, String urlPublica, String mime, long bytes) {}

    record Descarga(byte[] bytes, String mime) {}
}
