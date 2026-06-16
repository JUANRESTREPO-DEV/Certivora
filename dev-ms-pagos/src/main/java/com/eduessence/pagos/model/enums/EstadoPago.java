package com.eduessence.pagos.model.enums;

public enum EstadoPago {
    PENDIENTE_LLAVE,        // se generó llave Bre-B, esperando que el user pague
    PENDIENTE_CONFIRMACION, // user marcó como pagado, admin debe verificar
    APROBADO,               // admin o pasarela confirmó
    GRATIS_POR_CUPON,       // cupón cubrió 100%
    RECHAZADO,
    REVERSADO,
    EXPIRADO                // reserva vencida sin pago
}
