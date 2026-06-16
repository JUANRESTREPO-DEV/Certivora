package com.eduessence.certificados.service.impl;

import com.eduessence.certificados.exception.CertificadosApiException;
import com.eduessence.certificados.exception.ServerApiStatusCode;
import com.eduessence.certificados.feign.SendmailServiceClient;
import com.eduessence.certificados.model.dto.request.EmitirRequest;
import com.eduessence.certificados.model.dto.response.CertificadoResponse;
import com.eduessence.certificados.model.dto.response.VerificacionResponse;
import com.eduessence.certificados.model.entity.CertificadoEmitido;
import com.eduessence.certificados.model.entity.DiplomaTemplate;
import com.eduessence.certificados.repository.CertificadoEmitidoRepository;
import com.eduessence.certificados.repository.DiplomaTemplateRepository;
import com.eduessence.certificados.service.*;
import com.eduessence.certificados.utils.CodigoGenerador;
import com.eduessence.certificados.utils.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reglas:
 *  - R-CER-01: solo emitir si matrícula está aprobada (validación delegada al
 *    caller; aquí se asume que el caller —cursos service vía Feign— ya validó)
 *  - R-CER-02: código único {@code EDU-YYYY-NNNNNN}
 *  - R-CER-03: hash SHA-256 de (usuarioId|cursoId|fechaEmision|secret)
 *  - R-CER-04: QR apunta a URL pública de verificación
 *  - Idempotencia: si la matrícula ya tiene certificado, devolverlo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificadoServiceImpl implements CertificadoService {

    private static final String TIPO_PARTICIPANTE_DEFAULT = "ASISTENTE";
    private static final long URL_TTL_SEGUNDOS = 60 * 60 * 24L; // 24h

    private final CertificadoEmitidoRepository emitidoRepository;
    private final DiplomaTemplateRepository templateRepository;
    private final PdfGeneratorService pdfGenerator;
    private final QrGeneratorService qrGenerator;
    private final S3StorageService s3;
    private final CodigoGenerador codigoGenerador;
    private final SendmailServiceClient sendmail;

    @Value("${eduessence.certificados.url-verificacion-base}")
    private String urlVerificacionBase;

    @Value("${eduessence.certificados.hash-secret}")
    private String hashSecret;

    @Override
    @Transactional
    public CertificadoResponse emitir(EmitirRequest req) {
        // R-CER idempotencia
        var existente = emitidoRepository.findByMatriculaId(req.getMatriculaId());
        if (existente.isPresent()) {
            log.info("Certificado ya emitido para matrícula {}", req.getMatriculaId());
            return toResponse(existente.get());
        }

        String tipoPart = req.getTipoParticipante() == null || req.getTipoParticipante().isBlank()
                ? TIPO_PARTICIPANTE_DEFAULT
                : req.getTipoParticipante();

        // Resolución del template con tres niveles de fallback:
        //   1) templateId explícito enviado por el MS de cursos
        //      (curso.template_certificado_id). Soporta plantillas globales
        //      con curso_id = NULL.
        //   2) template específico del curso para ese tipo de participante.
        //   3) template global (curso_id NULL) para ese tipo de participante.
        DiplomaTemplate template = resolverTemplate(req.getTemplateId(), req.getCursoId(), tipoPart);

        String codigo = codigoGenerador.generar();
        // Truncamos a segundos para que la fecha guardada en BD coincida bit
        // a bit con la usada en el hash, sin importar cómo MySQL maneje los
        // micros del DATETIME(6).
        LocalDateTime fechaEmision = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String hash = HashUtils.hashCertificado(
                req.getUsuarioId(), req.getCursoId(), fechaEmision, hashSecret);
        String urlVerificacion = urlVerificacionBase + codigo;

        byte[] qr = qrGenerator.generar(urlVerificacion, 300);

        String fechaFormateada = fechaEmision.format(
                DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "CO")));
        byte[] pdf = pdfGenerator.generar(template, req.getNombreCompleto(),
                req.getNumeroDocumento(), req.getNombreCurso(), codigo, fechaFormateada, qr);

        String key = "certificados/" + codigo + ".pdf";
        s3.subirPdf(key, pdf);

        CertificadoEmitido cert = CertificadoEmitido.builder()
                .matriculaId(req.getMatriculaId())
                .usuarioId(req.getUsuarioId())
                .cursoId(req.getCursoId())
                .codigo(codigo)
                .nombreCompleto(req.getNombreCompleto())
                .nombreCurso(req.getNombreCurso())
                .urlPdf(key)
                .qrUrl(urlVerificacion)
                .hashVerificacion(hash)
                .fechaEmision(fechaEmision)
                .build();
        try {
            cert = emitidoRepository.save(cert);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Carrera: otro hilo (p. ej. la auto-sync del aula + el click del
            // botón "Generar mi certificado") emitió primero. El SELECT
            // idempotente de arriba dio vacío porque ese otro hilo aún no
            // había hecho commit, así que ambos generaron código y solo uno
            // ganó el INSERT. Re-consultamos y devolvemos el ya persistido.
            var ganador = emitidoRepository.findByMatriculaId(req.getMatriculaId());
            if (ganador.isPresent()) {
                log.info("Carrera de emisión detectada — devuelvo el certificado del ganador para matrícula {}",
                        req.getMatriculaId());
                return toResponse(ganador.get());
            }
            throw ex;
        }

        // Notificación por email
        try {
            String urlPdfPublica = s3.urlPreFirmada(key, URL_TTL_SEGUNDOS);
            sendmail.enviarEmail(Map.of(
                    "nombreTemplate", "CERTIFICADO_EMITIDO",
                    "destinatario", Map.of(
                            "correo", "usuario-" + req.getUsuarioId() + "@eduessence.local",
                            "nombre", req.getNombreCompleto()),
                    "variables", Map.of(
                            "nombreCurso", req.getNombreCurso(),
                            "codigoCertificado", codigo,
                            "urlVerificacion", urlVerificacion,
                            "urlPdf", urlPdfPublica
                    )
            ));
        } catch (Exception ex) {
            log.warn("No se pudo notificar emisión {}: {}", codigo, ex.getMessage());
        }

        log.info("Certificado {} emitido para usuario {} en curso {}",
                codigo, req.getUsuarioId(), req.getCursoId());
        return toResponse(cert);
    }

    @Override
    @Transactional
    public VerificacionResponse verificar(String codigo) {
        var cert = emitidoRepository.findByCodigo(codigo).orElse(null);
        if (cert == null) {
            return VerificacionResponse.builder()
                    .valido(false).codigo(codigo)
                    .mensaje("No existe un certificado con ese código.").build();
        }

        // 1) Intentar con el algoritmo nuevo (fecha truncada a segundos +
        //    formato fijo). Es el que generan las emisiones nuevas.
        String hashNuevo = HashUtils.hashCertificado(
                cert.getUsuarioId(), cert.getCursoId(),
                cert.getFechaEmision(), hashSecret);
        boolean valido = hashNuevo.equals(cert.getHashVerificacion());

        // 2) Si no matchea, probar el algoritmo legacy: tomaba
        //    fechaEmision.toString() crudo, que cambiaba después del round-trip
        //    por MySQL. Probamos con la representación actual de toString().
        if (!valido) {
            String hashLegacy = HashUtils.hashCertificadoLegacy(
                    cert.getUsuarioId(), cert.getCursoId(),
                    cert.getFechaEmision().toString(), hashSecret);
            if (hashLegacy.equals(cert.getHashVerificacion())) {
                valido = true;
                // Auto-migración: persistimos el hash en el formato nuevo
                // para que la próxima verificación no tenga que probar el
                // fallback. Idempotente y barato (solo este UPDATE).
                cert.setHashVerificacion(hashNuevo);
                emitidoRepository.save(cert);
                log.info("Hash migrado de legacy → nuevo formato para certificado {}", codigo);
            }
        }

        return VerificacionResponse.builder()
                .valido(valido).codigo(codigo)
                .nombreCompleto(cert.getNombreCompleto())
                .nombreCurso(cert.getNombreCurso())
                .fechaEmision(cert.getFechaEmision())
                .mensaje(valido ? "Certificado válido emitido por Eduessence."
                        : "Certificado corrupto: el hash no coincide.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String urlDescarga(String codigo) {
        var cert = emitidoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new CertificadosApiException(ServerApiStatusCode.CERTIFICADO_NO_ENCONTRADO));
        return s3.urlPreFirmada(cert.getUrlPdf(), URL_TTL_SEGUNDOS);
    }

    @Override
    @Transactional
    public CertificadoResponse repararHash(String codigo) {
        var cert = emitidoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new CertificadosApiException(ServerApiStatusCode.CERTIFICADO_NO_ENCONTRADO));
        String nuevo = HashUtils.hashCertificado(
                cert.getUsuarioId(), cert.getCursoId(),
                cert.getFechaEmision(), hashSecret);
        cert.setHashVerificacion(nuevo);
        emitidoRepository.save(cert);
        log.info("Hash de verificación recalculado para certificado {}", codigo);
        return toResponse(cert);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificadoResponse> misCertificados(Long usuarioId) {
        return emitidoRepository.findAllByUsuarioIdOrderByFechaEmisionDesc(usuarioId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Encuentra el template a usar para la emisión siguiendo la prioridad
     * descrita en {@link #emitir}. Si el {@code templateId} explícito viene
     * pero está inactivo, sigue intentando con los fallbacks para no dejar al
     * usuario sin certificado por un flag {@code activo=false}.
     */
    private DiplomaTemplate resolverTemplate(Long templateId, Long cursoId, String tipoPart) {
        if (templateId != null) {
            var t = templateRepository.findById(templateId).orElse(null);
            if (t != null && Boolean.TRUE.equals(t.getActivo())) return t;
            log.warn("templateId={} no existe o está inactivo; intentando fallback por curso+tipo", templateId);
        }
        var porCurso = templateRepository
                .findByCursoIdAndTipoParticipanteAndActivoTrue(cursoId, tipoPart);
        if (porCurso.isPresent()) return porCurso.get();

        var global = templateRepository
                .findByCursoIdIsNullAndTipoParticipanteAndActivoTrue(tipoPart);
        if (global.isPresent()) {
            log.info("Usando template global (cursoId NULL) para curso={} tipo={}", cursoId, tipoPart);
            return global.get();
        }
        throw new CertificadosApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO,
                "No hay template activo para curso " + cursoId + " / " + tipoPart
                        + (templateId != null ? " (templateId=" + templateId + " inválido)" : ""));
    }

    private CertificadoResponse toResponse(CertificadoEmitido c) {
        return CertificadoResponse.builder()
                .id(c.getId()).codigo(c.getCodigo())
                .usuarioId(c.getUsuarioId()).cursoId(c.getCursoId())
                .nombreCompleto(c.getNombreCompleto()).nombreCurso(c.getNombreCurso())
                .urlPdf(c.getUrlPdf()).qrUrl(c.getQrUrl())
                .fechaEmision(c.getFechaEmision())
                .build();
    }
}
