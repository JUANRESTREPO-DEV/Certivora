package com.eduessence.streaming.model.enums;

/**
 * Proveedores de streaming soportados. Abstracción para que el resto del MS
 * no dependa de un proveedor específico.
 *
 *  - AWS_IVS         · Amazon Interactive Video Service (managed, ultra-baja latencia)
 *  - NGINX_RTMP      · Self-hosted Nginx + nginx-rtmp-module + HLS
 *  - MUX             · Mux.com (managed)
 *  - LOCAL_RTMP      · Servidor RTMP local (MediaMTX/Nginx-RTMP) — ideal para dev
 *  - MOCK            · Solo URLs falsas (no ingiere video real)
 */
public enum ProviderTipo {
    AWS_IVS,
    NGINX_RTMP,
    MUX,
    LOCAL_RTMP,
    MOCK
}
