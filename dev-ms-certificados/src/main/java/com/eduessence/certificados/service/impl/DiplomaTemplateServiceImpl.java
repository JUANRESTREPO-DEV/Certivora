package com.eduessence.certificados.service.impl;

import com.eduessence.certificados.exception.CertificadosApiException;
import com.eduessence.certificados.exception.ServerApiStatusCode;
import com.eduessence.certificados.model.dto.request.CrearTemplateRequest;
import com.eduessence.certificados.model.dto.response.TemplateResponse;
import com.eduessence.certificados.model.entity.DiplomaTemplate;
import com.eduessence.certificados.repository.DiplomaTemplateRepository;
import com.eduessence.certificados.service.DiplomaTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiplomaTemplateServiceImpl implements DiplomaTemplateService {

    private static final String POSICIONES_DEFAULT = """
            {
              "nombre":   {"x": 200, "y": 320, "font": "Helvetica-Bold", "size": 36, "color": "#0f172a"},
              "curso":    {"x": 200, "y": 260, "font": "Helvetica",      "size": 22, "color": "#1e3a8a"},
              "codigo":   {"x": 60,  "y": 40,  "font": "Helvetica",      "size": 10, "color": "#475569"},
              "fecha":    {"x": 200, "y": 200, "font": "Helvetica",      "size": 16, "color": "#475569"},
              "qr":       {"x": 700, "y": 40,  "size": 110}
            }
            """;

    private final DiplomaTemplateRepository repo;

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> listar(String q) {
        List<DiplomaTemplate> result = (q != null && !q.isBlank())
                ? repo.findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(q.trim())
                : repo.findByActivoTrueOrderByNombreAsc();
        return result.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> listarParaCurso(Long cursoId) {
        return repo.findByActivoTrueAndCursoIdIsNullOrCursoId(cursoId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse obtener(Long id) {
        return toDto(findOrThrow(id));
    }

    @Override
    @Transactional
    public TemplateResponse crear(CrearTemplateRequest req) {
        DiplomaTemplate t = DiplomaTemplate.builder()
                .cursoId(req.getCursoId())
                .tipoParticipante(req.getTipoParticipante() == null ? "ASISTENTE" : req.getTipoParticipante())
                .nombre(req.getNombre())
                .descripcion(req.getDescripcion())
                .templateUrl(req.getTemplateUrl())
                .posiciones(req.getPosiciones() == null ? POSICIONES_DEFAULT : req.getPosiciones())
                .activo(true)
                .build();
        return toDto(repo.save(t));
    }

    @Override
    @Transactional
    public TemplateResponse actualizar(Long id, CrearTemplateRequest req) {
        DiplomaTemplate t = findOrThrow(id);
        if (req.getCursoId() != null) t.setCursoId(req.getCursoId());
        if (req.getTipoParticipante() != null) t.setTipoParticipante(req.getTipoParticipante());
        if (req.getNombre() != null) t.setNombre(req.getNombre());
        if (req.getDescripcion() != null) t.setDescripcion(req.getDescripcion());
        if (req.getTemplateUrl() != null) t.setTemplateUrl(req.getTemplateUrl());
        if (req.getPosiciones() != null) t.setPosiciones(req.getPosiciones());
        return toDto(repo.save(t));
    }

    @Override
    @Transactional
    public TemplateResponse clonar(Long id, Long cursoId, String tipoParticipante, String nombreOverride) {
        if (cursoId == null) {
            throw new CertificadosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "cursoId es obligatorio para clonar una plantilla");
        }
        DiplomaTemplate original = findOrThrow(id);
        String tipo = (tipoParticipante == null || tipoParticipante.isBlank())
                ? original.getTipoParticipante()
                : tipoParticipante;
        String nombre = (nombreOverride == null || nombreOverride.isBlank())
                ? original.getNombre() + " (copia)"
                : nombreOverride;

        // Idempotencia best-effort: si ya existe una plantilla activa
        // (curso_id, tipo) para este curso, devolverla en vez de duplicar.
        var existente = repo.findByCursoIdAndTipoParticipanteAndActivoTrue(cursoId, tipo);
        if (existente.isPresent()) return toDto(existente.get());

        DiplomaTemplate copia = DiplomaTemplate.builder()
                .cursoId(cursoId)
                .tipoParticipante(tipo)
                .nombre(nombre)
                .descripcion(original.getDescripcion())
                .templateUrl(original.getTemplateUrl())
                .posiciones(original.getPosiciones())
                .activo(true)
                .build();
        return toDto(repo.save(copia));
    }

    @Override
    @Transactional
    public void borrar(Long id) {
        DiplomaTemplate t = findOrThrow(id);
        t.setActivo(false);
        repo.save(t);
    }

    private DiplomaTemplate findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new CertificadosApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO));
    }

    private TemplateResponse toDto(DiplomaTemplate t) {
        return TemplateResponse.builder()
                .id(t.getId())
                .cursoId(t.getCursoId())
                .tipoParticipante(t.getTipoParticipante())
                .nombre(t.getNombre())
                .descripcion(t.getDescripcion())
                .templateUrl(t.getTemplateUrl())
                .posiciones(t.getPosiciones())
                .activo(t.getActivo())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
