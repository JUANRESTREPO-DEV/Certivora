package com.eduessence.sendmail.service;

public interface S3TemplateLoader {

    /**
     * Descarga el contenido del template de S3.
     * @param bucket Bucket de S3. Si es null/blank, usa el bucket por defecto del servicio.
     * @param key    Key del objeto.
     * @return Contenido como String UTF-8.
     */
    String descargar(String bucket, String key);

    /**
     * Sube/sobrescribe el contenido HTML del template en S3.
     * @param bucket  Bucket de S3. Si es null/blank, usa el bucket por defecto.
     * @param key     Key del objeto a escribir.
     * @param content HTML como string UTF-8.
     */
    void subir(String bucket, String key, String content);
}
