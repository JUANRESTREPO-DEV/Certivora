package com.eduessence.sendmail.service;

import com.eduessence.sendmail.model.entity.EmailTemplate;
import com.eduessence.sendmail.model.enums.EstadoEnvio;

import java.util.Map;

public interface EmailLogService {
    void registrar(EmailTemplate template,
                   String correo,
                   String nombre,
                   String asunto,
                   Map<String, Object> variablesUsadas,
                   EstadoEnvio estado,
                   String mensajeError);
}
