package com.eduessence.cursos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerApiStatusCode {

    OK(HttpStatus.OK, "CUR-000", "OK"),

    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "CUR-400", "Datos inválidos"),
    CURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "CUR-404-1", "Curso no existe"),
    LECCION_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "CUR-404-2", "Lección no existe"),
    EXAMEN_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "CUR-404-3", "Examen no existe"),
    MATRICULA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "CUR-404-4", "Matrícula no existe"),

    CURSO_SIN_MODALIDAD(HttpStatus.BAD_REQUEST, "CUR-400-1", "El curso debe tener al menos una modalidad"),
    MODALIDAD_NO_DISPONIBLE(HttpStatus.BAD_REQUEST, "CUR-400-2", "La modalidad solicitada no está activa en el curso"),
    CUPO_AGOTADO(HttpStatus.CONFLICT, "CUR-409-1", "El cupo del curso está agotado"),
    YA_INSCRITO(HttpStatus.CONFLICT, "CUR-409-2", "El usuario ya está matriculado en este curso"),
    INSCRIPCION_CERRADA(HttpStatus.BAD_REQUEST, "CUR-400-3", "La fecha límite de inscripción ya pasó"),
    CURSO_NO_ACTIVO(HttpStatus.CONFLICT, "CUR-409-5", "El curso no está activo para inscripciones"),
    EN_LISTA_ESPERA(HttpStatus.OK, "CUR-200-1", "Quedaste en lista de espera"),
    RESERVA_EXPIRADA(HttpStatus.GONE, "CUR-410-1", "La reserva de cupo expiró"),
    MATRICULA_NO_CANCELABLE(HttpStatus.CONFLICT, "CUR-409-6", "La matrícula ya no se puede cancelar"),
    MATRICULA_NO_REEMBOLSABLE(HttpStatus.CONFLICT, "CUR-409-7", "La matrícula no es reembolsable"),
    PAGO_NO_DISPONIBLE(HttpStatus.BAD_GATEWAY, "CUR-502-1", "No se pudo iniciar el pago"),

    LECCION_BLOQUEADA(HttpStatus.FORBIDDEN, "CUR-403-1", "Hay lecciones bloqueantes pendientes"),
    INTENTOS_AGOTADOS(HttpStatus.CONFLICT, "CUR-409-3", "Agotaste los intentos permitidos para este examen"),
    INTENTO_NO_FINALIZADO(HttpStatus.CONFLICT, "CUR-409-4", "Hay un intento sin finalizar"),

    PROGRESO_INSUFICIENTE(HttpStatus.BAD_REQUEST, "CUR-400-4", "No se cumple el progreso mínimo"),
    NOTA_INSUFICIENTE(HttpStatus.BAD_REQUEST, "CUR-400-5", "No se cumple la nota mínima"),
    PRESENCIA_INSUFICIENTE(HttpStatus.BAD_REQUEST, "CUR-400-6", "No se cumple el % mínimo de presencia"),

    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "CUR-500", "Error interno del servicio");

    private final HttpStatus httpStatus;
    private final String codigo;
    private final String descripcion;

    ServerApiStatusCode(HttpStatus httpStatus, String codigo, String descripcion) {
        this.httpStatus = httpStatus;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
}
