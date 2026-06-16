package com.eduessence.pagos.model.enums;

public enum AlcanceCupon {
    USUARIO_UNICO,   // solo el usuario_asignado_id
    MULTI_USO,       // hasta usos_maximos veces, 1 vez por usuario
    GLOBAL           // ilimitado en total, 1 vez por usuario
}
