package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.feign.AuthenticateServiceClient;
import com.eduessence.cursos.model.dto.ModalidadCursoDTO;
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
import com.eduessence.cursos.service.InvitacionCursoService;
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
    private final InvitacionCursoService invitacionCursoService;

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

        validarReglasPrecio(req.getModalidades());

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
                .precioCop(precioMinimoAsInt(req.getModalidades()))
                .intensidadHoras(req.getIntensidadHoras())
                .build();

        sincronizarInstructores(curso, req.getInstructorUsuarioIds());

        Set<CursoModalidad> mods = new HashSet<>();
        for (ModalidadCursoDTO m : req.getModalidades()) {
            mods.add(CursoModalidad.builder()
                    .curso(curso)
                    .tipo(m.getTipo())
                    .precioCop(m.getPrecioCop())
                    .sede(m.getSede())
                    .cupoModalidad(m.getCupoModalidad())
                    .activo(m.getActivo() == null ? true : m.getActivo())
                    .build());
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
            // NOTA: no validamos que templateCertificadoId esté presente cuando
            // emite=true, mismo criterio que crear(). El front sigue un flujo
            // de 2 pasos: primero guarda el curso con emiteCertificado=true y
            // templateCertificadoId=null, luego crea la plantilla en el MS
            // certificados y hace un segundo PUT con el id resuelto. Si por
            // algún error el segundo PUT no llega, la emisión posterior fallará
            // limpio con "curso sin plantilla configurada" — pero no bloqueamos
            // el guardado del curso.
        } else if (req.getTemplateCertificadoId() != null) {
            curso.setTemplateCertificadoId(req.getTemplateCertificadoId());
        }

        if (req.getFechaInicio() != null) curso.setFechaInicio(req.getFechaInicio());
        if (req.getFechaFin() != null) curso.setFechaFin(req.getFechaFin());
        if (req.getFechaLimiteInscripcion() != null) curso.setFechaLimiteInscripcion(req.getFechaLimiteInscripcion());
        if (req.getCupoMaximo() != null) curso.setCupoMaximo(req.getCupoMaximo());
        if (req.getIntensidadHoras() != null) curso.setIntensidadHoras(req.getIntensidadHoras());

        if (req.getInstructorUsuarioIds() != null && !req.getInstructorUsuarioIds().isEmpty()) {
            sincronizarInstructores(curso, req.getInstructorUsuarioIds());
        }

        if (req.getModalidades() != null && !req.getModalidades().isEmpty()) {
            validarReglasPrecio(req.getModalidades());
            // Upsert por tipo: mantenemos las que siguen y actualizamos su
            // precio/sede/cupo; borramos las que salieron; creamos las nuevas.
            Map<ModalidadTipo, ModalidadCursoDTO> nuevas = req.getModalidades().stream()
                    .collect(Collectors.toMap(ModalidadCursoDTO::getTipo, m -> m));
            curso.getModalidades().removeIf(m -> !nuevas.containsKey(m.getTipo()));
            for (CursoModalidad m : curso.getModalidades()) {
                ModalidadCursoDTO in = nuevas.get(m.getTipo());
                m.setPrecioCop(in.getPrecioCop());
                m.setSede(in.getSede());
                m.setCupoModalidad(in.getCupoModalidad());
                m.setActivo(in.getActivo() == null ? true : in.getActivo());
            }
            Set<ModalidadTipo> existentes = curso.getModalidades().stream()
                    .map(CursoModalidad::getTipo).collect(Collectors.toSet());
            for (ModalidadCursoDTO in : nuevas.values()) {
                if (!existentes.contains(in.getTipo())) {
                    curso.getModalidades().add(CursoModalidad.builder()
                            .curso(curso)
                            .tipo(in.getTipo())
                            .precioCop(in.getPrecioCop())
                            .sede(in.getSede())
                            .cupoModalidad(in.getCupoModalidad())
                            .activo(in.getActivo() == null ? true : in.getActivo())
                            .build());
                }
            }
            curso.setPrecioCop(precioMinimoAsInt(req.getModalidades()));
        }

        return toResponse(cursoRepository.save(curso));
    }

    /**
     * Reglas de precio por modalidad:
     *  - VIRTUAL_LIVE / PRESENCIAL activas: precio obligatorio (≥ 0).
     *  - GRABADO en curso híbrido (con vivo activo): precio DEBE ser null (bonus).
     *  - GRABADO como única activa: precio obligatorio.
     */
    private static void validarReglasPrecio(List<ModalidadCursoDTO> mods) {
        if (mods == null || mods.isEmpty()) return;
        boolean hayVivoActivo = mods.stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()) || m.getActivo() == null)
                .anyMatch(m -> m.getTipo() == ModalidadTipo.VIRTUAL_LIVE
                            || m.getTipo() == ModalidadTipo.PRESENCIAL);

        for (ModalidadCursoDTO m : mods) {
            boolean activo = m.getActivo() == null || Boolean.TRUE.equals(m.getActivo());
            if (!activo) continue;

            if (m.getTipo() == ModalidadTipo.VIRTUAL_LIVE
                    || m.getTipo() == ModalidadTipo.PRESENCIAL) {
                if (m.getPrecioCop() == null) {
                    throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                            "La modalidad " + m.getTipo() + " requiere un precio");
                }
            } else if (m.getTipo() == ModalidadTipo.GRABADO) {
                if (hayVivoActivo && m.getPrecioCop() != null) {
                    throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                            "GRABADO no puede tener precio cuando el curso ofrece vivo — "
                                    + "va incluido como bonus");
                }
                if (!hayVivoActivo && m.getPrecioCop() == null) {
                    throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                            "GRABADO como única modalidad requiere un precio");
                }
            }
        }
    }

    /** Mínimo de precios activos, para el campo legacy {@code curso.precio_cop}. */
    private static Integer precioMinimoAsInt(List<ModalidadCursoDTO> mods) {
        if (mods == null) return 0;
        return mods.stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()) || m.getActivo() == null)
                .map(ModalidadCursoDTO::getPrecioCop)
                .filter(p -> p != null)
                .min(BigDecimal::compareTo)
                .map(BigDecimal::intValue)
                .orElse(0);
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
        boolean transicionABorradorAActivo = c.getEstado() != EstadoCurso.ACTIVO;
        c.setEstado(EstadoCurso.ACTIVO);
        CursoResponse response = toResponse(cursoRepository.save(c));

        // Broadcast de invitación solo cuando pasa a ACTIVO por primera vez
        // (evita spam si el admin re-guarda o re-activa). El envío es async —
        // no afecta el response al cliente.
        if (transicionABorradorAActivo) {
            invitacionCursoService.broadcastActivacion(id);
        }
        return response;
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

        List<ModalidadCursoDTO> modalidadesDetalle = c.getModalidades().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .map(m -> ModalidadCursoDTO.builder()
                        .tipo(m.getTipo())
                        .precioCop(m.getPrecioCop())
                        .sede(m.getSede())
                        .cupoModalidad(m.getCupoModalidad())
                        .activo(m.getActivo())
                        .build())
                .sorted((a, b) -> a.getTipo().name().compareTo(b.getTipo().name()))
                .toList();

        BigDecimal precioDesde = modalidadesDetalle.stream()
                .map(ModalidadCursoDTO::getPrecioCop)
                .filter(p -> p != null)
                .min(BigDecimal::compareTo)
                .orElse(null);

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
                .cupoMaximo(c.getCupoMaximo())
                .precioDesdeCop(precioDesde)
                .precioCop(c.getPrecioCop())
                .intensidadHoras(c.getIntensidadHoras())
                .instructorUsuarioId(c.getInstructorUsuarioId())
                .instructores(enriquecer(c.getInstructores()))
                .modalidades(modalidades)
                .modalidadesDetalle(modalidadesDetalle)
                .hibrido(modalidades.size() >= 2)
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
