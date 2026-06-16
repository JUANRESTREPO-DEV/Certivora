package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.request.ActualizarConfigAprobacionRequest;
import com.eduessence.cursos.model.dto.response.ConfigAprobacionResponse;
import com.eduessence.cursos.model.entity.ConfiguracionAprobacion;
import com.eduessence.cursos.repository.ConfiguracionAprobacionRepository;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.service.ConfigAprobacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ConfigAprobacionServiceImpl implements ConfigAprobacionService {

    private final ConfiguracionAprobacionRepository repo;
    private final CursoRepository cursoRepo;

    @Override
    @Transactional(readOnly = true)
    public ConfigAprobacionResponse obtener(Long cursoId) {
        ConfiguracionAprobacion c = repo.findByCursoId(cursoId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO,
                        "El curso no tiene configuración de aprobación"));
        return toDto(c);
    }

    @Override
    @Transactional
    public ConfigAprobacionResponse actualizar(Long cursoId, ActualizarConfigAprobacionRequest req) {
        // Verificamos que el curso exista (para mensaje claro si no)
        if (!cursoRepo.existsById(cursoId)) {
            throw new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO);
        }
        ConfiguracionAprobacion c = repo.findByCursoId(cursoId).orElseGet(() -> {
            ConfiguracionAprobacion nueva = ConfiguracionAprobacion.builder()
                    .curso(cursoRepo.getReferenceById(cursoId))
                    .notaMinimaGeneral(new BigDecimal("70.00"))
                    .progresoMinimoPct(new BigDecimal("80.00"))
                    .presenciaMinimaPct(new BigDecimal("70.00"))
                    .presenciaQrMinimaPct(new BigDecimal("80.00"))
                    .build();
            return repo.save(nueva);
        });

        if (req.getNotaMinimaGeneral() != null)    c.setNotaMinimaGeneral(req.getNotaMinimaGeneral());
        if (req.getProgresoMinimoPct() != null)    c.setProgresoMinimoPct(req.getProgresoMinimoPct());
        if (req.getPresenciaMinimaPct() != null)   c.setPresenciaMinimaPct(req.getPresenciaMinimaPct());
        if (req.getPresenciaQrMinimaPct() != null) c.setPresenciaQrMinimaPct(req.getPresenciaQrMinimaPct());

        return toDto(repo.save(c));
    }

    private ConfigAprobacionResponse toDto(ConfiguracionAprobacion c) {
        return ConfigAprobacionResponse.builder()
                .cursoId(c.getCurso() != null ? c.getCurso().getId() : null)
                .notaMinimaGeneral(c.getNotaMinimaGeneral())
                .progresoMinimoPct(c.getProgresoMinimoPct())
                .presenciaMinimaPct(c.getPresenciaMinimaPct())
                .presenciaQrMinimaPct(c.getPresenciaQrMinimaPct())
                .build();
    }
}
