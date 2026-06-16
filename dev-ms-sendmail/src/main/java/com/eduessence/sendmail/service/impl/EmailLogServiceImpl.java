package com.eduessence.sendmail.service.impl;

import com.eduessence.sendmail.model.entity.EmailEnvio;
import com.eduessence.sendmail.model.entity.EmailTemplate;
import com.eduessence.sendmail.model.enums.EstadoEnvio;
import com.eduessence.sendmail.repository.EmailEnvioRepository;
import com.eduessence.sendmail.service.EmailLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailLogServiceImpl implements EmailLogService {

    private final EmailEnvioRepository envioRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EmailTemplate template,
                          String correo,
                          String nombre,
                          String asunto,
                          Map<String, Object> variablesUsadas,
                          EstadoEnvio estado,
                          String mensajeError) {
        try {
            String varsJson = null;
            if (variablesUsadas != null && !variablesUsadas.isEmpty()) {
                try {
                    varsJson = objectMapper.writeValueAsString(variablesUsadas);
                } catch (JsonProcessingException e) {
                    log.warn("No se pudo serializar variables a JSON: {}", e.getMessage());
                }
            }
            envioRepository.save(EmailEnvio.builder()
                    .template(template)
                    .destinatario(correo)
                    .nombreDestinatario(nombre)
                    .asunto(asunto)
                    .estado(estado)
                    .variablesUsadas(varsJson)
                    .mensajeError(mensajeError)
                    .build());
        } catch (Exception ex) {
            log.warn("No se pudo registrar log de envío: {}", ex.getMessage());
        }
    }
}
