package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.feign.AuthenticateServiceClient;
import com.eduessence.cursos.model.dto.request.EscarapelaTemplateRequest;
import com.eduessence.cursos.model.dto.response.EscarapelaRenderResponse;
import com.eduessence.cursos.model.dto.response.EscarapelaTemplateResponse;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.CursoModalidad;
import com.eduessence.cursos.model.entity.EscarapelaTemplate;
import com.eduessence.cursos.model.entity.Matricula;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.EscarapelaTemplateRepository;
import com.eduessence.cursos.repository.MatriculaRepository;
import com.eduessence.cursos.service.EscarapelaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscarapelaServiceImpl implements EscarapelaService {

    private final EscarapelaTemplateRepository templateRepo;
    private final CursoRepository cursoRepo;
    private final MatriculaRepository matriculaRepo;
    private final AuthenticateServiceClient authClient;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    @Transactional(readOnly = true)
    public EscarapelaTemplateResponse obtenerPorCurso(Long cursoId) {
        return templateRepo.findByCursoId(cursoId).map(this::toResponse).orElse(null);
    }

    @Override
    @Transactional
    public EscarapelaTemplateResponse upsert(Long cursoId, EscarapelaTemplateRequest req) {
        Curso curso = cursoRepo.findById(cursoId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO));

        EscarapelaTemplate t = templateRepo.findByCursoId(cursoId).orElseGet(() ->
                EscarapelaTemplate.builder().cursoId(curso.getId()).build());

        if (req.getFondoUrl() != null) t.setFondoUrl(emptyToNull(req.getFondoUrl()));
        if (req.getPosicionesJson() != null) t.setPosicionesJson(emptyToNull(req.getPosicionesJson()));
        if (req.getActivo() != null) t.setActivo(req.getActivo());

        return toResponse(templateRepo.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public EscarapelaRenderResponse renderPorToken(String token) {
        Matricula m = matriculaRepo.findByEscarapelaToken(token)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.MATRICULA_NO_ENCONTRADA,
                        "Escarapela no encontrada"));

        Curso curso = m.getCurso();
        EscarapelaTemplate tpl = templateRepo.findByCursoId(curso.getId()).orElse(null);

        String sede = curso.getModalidades().stream()
                .filter(cm -> Boolean.TRUE.equals(cm.getActivo())
                        && cm.getTipo() == ModalidadTipo.PRESENCIAL)
                .map(CursoModalidad::getSede)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse("");

        UsuarioLookup u = resolverAsistente(m.getUsuarioId());
        String qrUrl = frontendUrl.replaceAll("/+$", "")
                + "/escarapela/scan/" + token;

        return EscarapelaRenderResponse.builder()
                .token(token)
                .fondoUrl(tpl == null ? null : tpl.getFondoUrl())
                .posicionesJson(tpl == null ? null : tpl.getPosicionesJson())
                .nombreAsistente(u.nombreCompleto)
                .emailAsistente(u.email)
                .telefonoAsistente(u.telefono)
                .cursoId(curso.getId())
                .nombreCurso(curso.getNombre())
                .cursoSlug(curso.getSlug())
                .sede(sede)
                .fechaInicio(curso.getFechaInicio() == null ? null : curso.getFechaInicio().toString())
                .qrUrl(qrUrl)
                .build();
    }

    /* ─────────── helpers ─────────── */

    private record UsuarioLookup(String nombreCompleto, String email, String telefono) {}

    @SuppressWarnings("unchecked")
    private UsuarioLookup resolverAsistente(Long usuarioId) {
        try {
            Map<String, Object> resp = authClient.lookup(List.of(usuarioId));
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?> u) {
                Map<String, Object> mu = (Map<String, Object>) u;
                String nombres = strOr(mu.get("nombres"), "");
                String apellidos = strOr(mu.get("apellidos"), "");
                String nombre = (nombres + " " + apellidos).trim();
                if (nombre.isEmpty()) nombre = nombres;
                return new UsuarioLookup(
                        nombre,
                        strOr(mu.get("email"), ""),
                        strOr(mu.get("telefono"), "")
                );
            }
        } catch (Exception ex) {
            log.warn("Lookup asistente {} falló: {}", usuarioId, ex.getMessage());
        }
        return new UsuarioLookup("Asistente", "", "");
    }

    private EscarapelaTemplateResponse toResponse(EscarapelaTemplate t) {
        return EscarapelaTemplateResponse.builder()
                .id(t.getId())
                .cursoId(t.getCursoId())
                .fondoUrl(t.getFondoUrl())
                .posicionesJson(t.getPosicionesJson())
                .activo(t.getActivo())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String strOr(Object o, String def) {
        return o == null ? def : o.toString();
    }
}
