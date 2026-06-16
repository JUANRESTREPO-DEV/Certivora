package com.eduessence.cursos.model.dto.request;

import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.model.enums.TipoParticipante;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

/**
 * Inscripción de cortesía creada por un administrador — bypassea pago y, si
 * se solicita, lista de espera. El admin elige el {@link TipoParticipante}
 * (ASISTENTE / PONENTE / ORGANIZADOR / STAFF) — ese campo decide qué plantilla
 * de certificado se emitirá.
 */
@Data
public class CortesiaRequest {

    /** Usuario al que se le va a otorgar la cortesía. */
    @NotNull
    private Long usuarioId;

    /** Rol con el que figurará en el curso. */
    @NotNull
    private TipoParticipante tipo;

    /** Modalidades del curso en las que participa. */
    @NotEmpty
    private Set<ModalidadTipo> modalidades;

    /**
     * Si el cupo está lleno, la cortesía aún así se otorga (no entra a lista
     * de espera) — es la decisión del admin. Sirve para PONENTE/ORGANIZADOR
     * que no deberían contar contra el cupo de asistentes.
     */
    private Boolean ignorarCupo;

    /** Texto libre para auditoría. */
    private String observacion;
}
