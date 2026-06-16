package com.eduessence.pagos.model.dto.request;

import lombok.Data;

@Data
public class ConfirmarPagoRequest {
    /** URL S3 del comprobante de pago subido por el usuario. */
    private String comprobanteUrl;
    private String observacion;
}
