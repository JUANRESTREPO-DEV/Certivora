package com.eduessence.streaming.model.enums;

public enum EstadoStream {
    CREADO,         // sesión creada, OBS aún no se conectó
    EN_VIVO,        // OBS conectado, ingest activo
    PAUSADO,        // se desconectó temporalmente
    TERMINADO,      // sesión cerrada manual o por timeout
    FALLIDO
}
