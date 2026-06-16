package com.eduessence.cursos.model.enums;

/**
 * Tipo del recurso adjunto a un curso. Determina dónde lo muestra el front:
 *  · PROGRAMA_PDF   · descargable en la cabecera del curso (carta de presentación)
 *  · MATERIAL_PDF   · biblioteca de materiales del aula
 *  · DOCUMENTO      · genérico (sin sección específica)
 *  · OTRO           · escape hatch
 */
public enum TipoRecurso {
    PROGRAMA_PDF,
    MATERIAL_PDF,
    DOCUMENTO,
    OTRO
}
