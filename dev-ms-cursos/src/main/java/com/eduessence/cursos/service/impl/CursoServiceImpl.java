package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.feign.AuthenticateServiceClient;
import com.eduessence.cursos.model.dto.request.ActualizarCursoRequest;
import com.eduessence.cursos.model.dto.request.CrearCursoRequest;
import com.eduessence.cursos.model.dto.response.CursoResponse;
import com.eduessence.cursos.model.dto.response.InstructorDTO;
import com.eduessence.cursos.model.entity.ConfiguracionAprobacion;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.CursoInstructor;
import com.eduessence.cursos.model.entity.CursoModalidad;
import com.eduessence.cursos.model.enums.EstadoCurso;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.service.CursoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CursoServiceImpl implements CursoService {

    private final CursoRepository cursoRepository;
    private final AuthenticateServiceClient authClient;

    @Override
    @Transactional
    public CursoResponse crear(CrearCursoRequest req) {
        if (req.getModalidades() == null || req.getModalidades().isEmpty()) {
            throw new CursosApiException(ServerApiStatusCode.CURSO_SIN_MODALIDAD);
        }
        if (req.getInstructorUsuarioIds() == null || req.getInstructorUsuarioIds().isEmpty()) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Debes seleccionar al menos un instructor");
        }

        // NOTA: no validamos aquí que templateCertificadoId esté presente
        // cuando emiteCertificado=true. El flujo de "Crear plantilla para este
        // curso" crea la plantilla DESPUÉS del POST del curso y la asigna con
        // un PUT (ver pipeline en curso-form.ts). La validación estricta solo
        // se aplica al editar (ActualizarCursoRequest) si emite cambia o si se
        // intenta dejar el id en null explícitamente.
        boolean emite = Boolean.TRUE.equals(req.getEmiteCertificado());

        Curso curso = Curso.builder()
                .nombre(req.getNombre())
                .slug(req.getSlug().toLowerCase())
                .descripcionCorta(req.getDescripcionCorta())
                .descripcionLarga(req.getDescripcionLarga())
                .logoUrl(req.getLogoUrl())
                .videoPresentacionUrl(req.getVideoPresentacionUrl())
                .imagenPresentacionUrl(req.getImagenPresentacionUrl())
                .emiteCertificado(emite)
                .templateCertificadoId(emite ? req.getTemplateCertificadoId() : null)
                .temarioJson(emptyToNull(req.getTemarioJson()))
                .estado(EstadoCurso.BORRADOR)
                .fechaInicio(req.getFechaInicio())
                .fechaFin(req.getFechaFin())
                .fechaLimiteInscripcion(req.getFechaLimiteInscripcion())
                .cupoMaximo(req.getCupoMaximo())
                .precioCop(req.getPrecioCop() == null ? 0 : req.getPrecioCop())
                .intensidadHoras(req.getIntensidadHoras())
                .build();

        sincronizarInstructores(curso, req.getInstructorUsuarioIds());

        Set<CursoModalidad> mods = new HashSet<>();
        for (ModalidadTipo t : req.getModalidades()) {
            mods.add(CursoModalidad.builder().curso(curso).tipo(t).activo(true).build());
        }
        curso.setModalidades(mods);

        ConfiguracionAprobacion cfg = ConfiguracionAprobacion.builder()
                .curso(curso)
                .notaMinimaGeneral(new BigDecimal("70.00"))
                .progresoMinimoPct(new BigDecimal("80.00"))
                .presenciaMinimaPct(new BigDecimal("70.00"))
                .presenciaQrMinimaPct(new BigDecimal("80.00"))
                .build();
        curso.setConfiguracionAprobacion(cfg);

        return toResponse(cursoRepository.save(curso));
    }

    @Override
    @Transactional
    public CursoResponse actualizar(Long id, ActualizarCursoRequest req) {
        Curso curso = findById(id);

        if (req.getNombre() != null) curso.setNombre(req.getNombre());
        if (req.getSlug() != null) curso.setSlug(req.getSlug().toLowerCase());
        if (req.getDescripcionCorta() != null) curso.setDescripcionCorta(emptyToNull(req.getDescripcionCorta()));
        if (req.getDescripcionLarga() != null) curso.setDescripcionLarga(emptyToNull(req.getDescripcionLarga()));
        if (req.getLogoUrl() != null) curso.setLogoUrl(emptyToNull(req.getLogoUrl()));
        if (req.getVideoPresentacionUrl() != null) curso.setVideoPresentacionUrl(emptyToNull(req.getVideoPresentacionUrl()));
        if (req.getImagenPresentacionUrl() != null) curso.setImagenPresentacionUrl(emptyToNull(req.getImagenPresentacionUrl()));
        if (req.getTemarioJson() != null) curso.setTemarioJson(emptyToNull(req.getTemarioJson()));

        if (req.getEmiteCertificado() != null) {
            boolean emite = req.getEmiteCertificado();
            curso.setEmiteCertificado(emite);
            if (!emite) {
                curso.setTemplateCertificadoId(null);
            } else if (req.getTemplateCertificadoId() != null) {
                curso.setTemplateCertificadoId(req.getTemplateCertificadoId());
            }
            if (emite && curso.getTemplateCertificadoId() == null) {
                throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Si el curso emite certificado debes elegir una plantilla");
            }
        } else if (req.getTemplateCertificadoId() != null) {
            curso.setTemplateCertificadoId(req.getTemplateCertificadoId());
        }

        if (req.getFechaInicio() != null) curso.setFechaInicio(req.getFechaInicio());
        if (req.getFechaFin() != null) curso.setFechaFin(req.getFechaFin());
        if (req.getFechaLimiteInscripcion() != null) curso.setFechaLimiteInscripcion(req.getFechaLimiteInscripcion());
        if (req.getCupoMaximo() != null) curso.setCupoMaximo(req.getCupoMaximo());
        if (req.getPrecioCop() != null) curso.setPrecioCop(req.getPrecioCop());
        if (req.getIntensidadHoras() != null) curso.setIntensidadHoras(req.getIntensidadHoras());

        if (req.getInstructorUsuarioIds() != null && !req.getInstructorUsuarioIds().isEmpty()) {
            sincronizarInstructores(curso, req.getInstructorUsuarioIds());
        }

        if (req.getModalidades() != null && !req.getModalidades().isEmpty()) {
            // Reemplazo total de modalidades; orphanRemoval limpia las anteriores.
            Set<ModalidadTipo> nuevas = new HashSet<>(req.getModalidades());
            curso.getModalidades().removeIf(m -> !nuevas.contains(m.getTipo()));
            Set<ModalidadTipo> existentes = curso.getModalidades().stream()
                    .map(CursoModalidad::getTipo).collect(Collectors.toSet());
            for (ModalidadTipo t : nuevas) {
                if (!existentes.contains(t)) {
                    curso.getModalidades().add(CursoModalidad.builder()
                            .curso(curso).tipo(t).activo(true).build());
                }
            }
        }

        return toResponse(cursoRepository.save(curso));
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    @Override
    @Transactional(readOnly = true)
    public CursoResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CursoResponse obtenerPorSlug(String slug) {
        return toResponse(cursoRepository.findBySlug(slug)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CursoResponse> listarActivos() {
        return cursoRepository.findAllByEstadoOrderByFechaInicioDesc(EstadoCurso.ACTIVO)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CursoResponse> listarTodos(EstadoCurso estadoFiltro) {
        if (estadoFiltro != null) {
            return cursoRepository.findAllByEstadoOrderByFechaInicioDesc(estadoFiltro)
                    .stream().map(this::toResponse).toList();
        }
        return cursoRepository.findAllByOrderByFechaCreacionDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CursoResponse activar(Long id) {
        Curso c = findById(id);
        c.setEstado(EstadoCurso.ACTIVO);
        return toResponse(cursoRepository.save(c));
    }

    @Override
    @Transactional
    public CursoResponse archivar(Long id) {
        Curso c = findById(id);
        c.setEstado(EstadoCurso.ARCHIVADO);
        return toResponse(cursoRepository.save(c));
    }

    /* ─────────────────── helpers ─────────────────── */

    private Curso findById(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO));
    }

    /**
     * Sincroniza la colección de instructores: el primer ID es el principal,
     * los demás co-instructores. También actualiza el campo legacy
     * {@code instructor_usuario_id} con el principal.
     *
     * <p>Implementación con diff in-place para evitar
     * {@code Duplicate entry 'X-Y' for key uk_ci_curso_usuario}:</p>
     * <ul>
     *   <li>Mantiene los {@link CursoInstructor} cuyo {@code usuarioId} aparece
     *       en la nueva lista (solo actualiza su flag {@code principal}).</li>
     *   <li>Elimina con {@code orphanRemoval} los que ya no aparecen.</li>
     *   <li>Crea solo los verdaderamente nuevos.</li>
     * </ul>
     * Antes hacíamos {@code clear()+add()}; Hibernate flushaba los INSERT
     * antes de los DELETE y chocaba con el UNIQUE (curso, usuario).
     */
    private void sincronizarInstructores(Curso curso, List<Long> ids) {
        Long principalId = ids.get(0);
        Set<Long> nuevosIds = new HashSet<>(ids);

        // 1) eliminar los que ya no están
        curso.getInstructores().removeIf(ci -> !nuevosIds.contains(ci.getUsuarioId()));

        // 2) actualizar flag principal en los que se mantienen + recolectar set actual
        Set<Long> existentes = new HashSet<>();
        for (CursoInstructor ci : curso.getInstructores()) {
            boolean esPrincipal = ci.getUsuarioId().equals(principalId);
            if (!Boolean.valueOf(esPrincipal).equals(ci.getPrincipal())) {
                ci.setPrincipal(esPrincipal);
            }
            existentes.add(ci.getUsuarioId());
        }

        // 3) agregar solo los verdaderamente nuevos
        for (Long uid : ids) {
            if (!existentes.contains(uid)) {
                curso.getInstructores().add(CursoInstructor.builder()
                        .curso(curso)
                        .usuarioId(uid)
                        .principal(uid.equals(principalId))
                        .build());
            }
        }

        curso.setInstructorUsuarioId(principalId);
    }

    private CursoResponse toResponse(Curso c) {
        Set<ModalidadTipo> modalidades = c.getModalidades().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .map(CursoModalidad::getTipo)
                .collect(Collectors.toSet());

        return CursoResponse.builder()
                .id(c.getId()).nombre(c.getNombre()).slug(c.getSlug())
                .descripcionCorta(c.getDescripcionCorta()).descripcionLarga(c.getDescripcionLarga())
                .logoUrl(c.getLogoUrl())
                .videoPresentacionUrl(c.getVideoPresentacionUrl())
                .imagenPresentacionUrl(c.getImagenPresentacionUrl())
                .emiteCertificado(c.getEmiteCertificado())
                .templateCertificadoId(c.getTemplateCertificadoId())
                .temarioJson(c.getTemarioJson())
                .estado(c.getEstado())
                .fechaInicio(c.getFechaInicio()).fechaFin(c.getFechaFin())
                .fechaLimiteInscripcion(c.getFechaLimiteInscripcion())
                .cupoMaximo(c.getCupoMaximo()).precioCop(c.getPrecioCop())
                .intensidadHoras(c.getIntensidadHoras())
                .instructorUsuarioId(c.getInstructorUsuarioId())
                .instructores(enriquecer(c.getInstructores()))
                .modalidades(modalidades).hibrido(modalidades.size() >= 2)
                .build();
    }

    /**
     * Llama vía Feign a authenticate-service para enriquecer cada
     * CursoInstructor con nombres + apellidos + email del usuario.
     * Si authenticate está caído devuelve la lista con solo los IDs.
     */
    @SuppressWarnings("unchecked")
    private List<InstructorDTO> enriquecer(Set<CursoInstructor> instructores) {
        if (instructores == null || instructores.isEmpty()) return List.of();

        // Ordena: principal primero, luego por id ascendente
        List<CursoInstructor> ordenados = new ArrayList<>(instructores);
        ordenados.sort((a, b) -> {
            int pa = Boolean.TRUE.equals(a.getPrincipal()) ? 0 : 1;
            int pb = Boolean.TRUE.equals(b.getPrincipal()) ? 0 : 1;
            if (pa != pb) return Integer.compare(pa, pb);
            return Long.compare(a.getUsuarioId(), b.getUsuarioId());
        });

        List<Long> ids = ordenados.stream().map(CursoInstructor::getUsuarioId).toList();
        Map<Long, Map<String, Object>> dataPorId = new LinkedHashMap<>();
        try {
            Map<String, Object> resp = authClient.lookup(ids);
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m) {
                        Object idVal = m.get("id");
                        if (idVal instanceof Number n) {
                            dataPorId.put(n.longValue(), (Map<String, Object>) m);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo enriquecer instructores: {}", ex.getMessage());
        }

        List<InstructorDTO> out = new ArrayList<>();
        for (CursoInstructor ci : ordenados) {
            Map<String, Object> u = dataPorId.getOrDefault(ci.getUsuarioId(), Collections.emptyMap());
            out.add(InstructorDTO.builder()
                    .usuarioId(ci.getUsuarioId())
                    .nombres((String) u.get("nombres"))
                    .apellidos((String) u.get("apellidos"))
                    .email((String) u.get("email"))
                    .avatarUrl((String) u.get("avatarUrl"))
                    .principal(Boolean.TRUE.equals(ci.getPrincipal()))
                    .build());
        }
        return out;
    }
}
