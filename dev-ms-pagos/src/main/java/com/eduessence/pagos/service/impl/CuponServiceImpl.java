package com.eduessence.pagos.service.impl;

import com.eduessence.pagos.exception.PagosApiException;
import com.eduessence.pagos.exception.ServerApiStatusCode;
import com.eduessence.pagos.model.dto.request.CuponRequest;
import com.eduessence.pagos.model.dto.response.CuponResponse;
import com.eduessence.pagos.model.dto.response.CuponUsoResponse;
import com.eduessence.pagos.model.dto.response.ValidarCuponResponse;
import com.eduessence.pagos.model.entity.Cupon;
import com.eduessence.pagos.model.entity.CuponUso;
import com.eduessence.pagos.model.enums.AlcanceCupon;
import com.eduessence.pagos.model.enums.TipoDescuento;
import com.eduessence.pagos.repository.CuponRepository;
import com.eduessence.pagos.repository.CuponUsoRepository;
import com.eduessence.pagos.service.CuponService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reglas R-CUP-01 a R-CUP-12:
 *  - 01: código uppercase, lookup case-insensitive
 *  - 02: validaciones en orden (existe → activo → vigencia → alcance → no usado → cupos → curso)
 *  - 03/04: 100% → curso gratis
 *  - 05: USUARIO_UNICO solo el asignado
 *  - 06: MULTI_USO tope + 1 vez por usuario
 *  - 07: GLOBAL sin tope + 1 vez por usuario
 *  - 08: filtro por curso_id si está seteado
 *  - 09: usos_actuales++ al INICIAR, liberar al rechazar/expirar
 *  - 10: 1 cupón por pago
 *  - 11: cupón no libera reserva
 *  - 12: auditoría en cupon_uso.monto_descuento
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuponServiceImpl implements CuponService {

    private final CuponRepository cuponRepository;
    private final CuponUsoRepository cuponUsoRepository;

    @Override
    @Transactional(readOnly = true)
    public ValidarCuponResponse validar(String codigo, Long usuarioId, Long cursoId, int montoOriginal) {
        Cupon c = cargarYValidar(codigo, usuarioId, cursoId);
        int descuento = calcularDescuento(c, montoOriginal);
        int montoFinal = Math.max(0, montoOriginal - descuento);
        return ValidarCuponResponse.builder()
                .valido(true).codigo(c.getCodigo()).descripcion(c.getDescripcion())
                .tipoDescuento(c.getTipoDescuento()).valor(c.getValor())
                .montoOriginal(montoOriginal).montoDescuento(descuento).montoFinal(montoFinal)
                .esCursoGratisTrasCupon(montoFinal == 0)
                .build();
    }

    @Override
    @Transactional
    public Cupon reservar(String codigo, Long usuarioId, Long cursoId, Long pagoId, int montoDescuento) {
        Cupon c = cargarYValidar(codigo, usuarioId, cursoId);
        c.setUsosActuales(c.getUsosActuales() + 1);
        cuponRepository.save(c);
        cuponUsoRepository.save(CuponUso.builder()
                .cupon(c).pagoId(pagoId).usuarioId(usuarioId).cursoId(cursoId)
                .montoDescuento(montoDescuento)
                .build());
        return c;
    }

    @Override
    @Transactional
    public void liberar(Long pagoId) {
        cuponUsoRepository.findByPagoId(pagoId).ifPresent(uso -> {
            Cupon c = uso.getCupon();
            c.setUsosActuales(Math.max(0, c.getUsosActuales() - 1));
            cuponRepository.save(c);
            cuponUsoRepository.delete(uso);
            log.info("Cupón {} liberado (pago {})", c.getCodigo(), pagoId);
        });
    }

    /* ─────────────────── CRUD admin ─────────────────── */

    @Override
    @Transactional(readOnly = true)
    public Page<CuponResponse> listar(String search, String alcance, Boolean activo, Pageable pageable) {
        Specification<Cupon> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("codigo")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("descripcion"), "")), like)
                ));
            }
            if (alcance != null && !alcance.isBlank()) {
                preds.add(cb.equal(root.get("alcance"), AlcanceCupon.valueOf(alcance)));
            }
            if (activo != null) {
                preds.add(cb.equal(root.get("activo"), activo));
            }
            return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
        };
        return cuponRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CuponResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public CuponResponse crear(CuponRequest req, Long creadoPorUsuarioId) {
        validarConfig(req);
        String codigoNormalizado = req.getCodigo().trim().toUpperCase();
        cuponRepository.findByCodigoIgnoreCase(codigoNormalizado).ifPresent(c -> {
            throw new PagosApiException(ServerApiStatusCode.CUPON_CODIGO_DUPLICADO);
        });

        Cupon c = Cupon.builder()
                .codigo(codigoNormalizado)
                .descripcion(req.getDescripcion())
                .tipoDescuento(req.getTipoDescuento())
                .valor(req.getValor())
                .alcance(req.getAlcance())
                .usuarioAsignadoId(req.getUsuarioAsignadoId())
                .usosMaximos(req.getUsosMaximos())
                .cursoId(req.getCursoId())
                .fechaInicio(req.getFechaInicio())
                .fechaFin(req.getFechaFin())
                .activo(true)
                .usosActuales(0)
                .creadoPorUsuarioId(creadoPorUsuarioId)
                .build();
        c = cuponRepository.save(c);
        log.info("Cupón {} creado por {}", c.getCodigo(), creadoPorUsuarioId);
        return toResponse(c);
    }

    @Override
    @Transactional
    public CuponResponse actualizar(Long id, CuponRequest req) {
        Cupon c = buscarOFallar(id);
        validarConfig(req);

        boolean tieneUsos = c.getUsosActuales() != null && c.getUsosActuales() > 0;
        boolean cambiaConfig = !Objects.equals(c.getTipoDescuento(), req.getTipoDescuento())
                || !Objects.equals(c.getValor(), req.getValor())
                || !Objects.equals(c.getAlcance(), req.getAlcance())
                || !Objects.equals(c.getCursoId(), req.getCursoId())
                || !Objects.equals(c.getUsuarioAsignadoId(), req.getUsuarioAsignadoId());
        if (tieneUsos && cambiaConfig) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_TIENE_USOS);
        }

        String codigoNormalizado = req.getCodigo().trim().toUpperCase();
        if (!codigoNormalizado.equalsIgnoreCase(c.getCodigo())) {
            cuponRepository.findByCodigoIgnoreCase(codigoNormalizado).ifPresent(otro -> {
                if (!Objects.equals(otro.getId(), id))
                    throw new PagosApiException(ServerApiStatusCode.CUPON_CODIGO_DUPLICADO);
            });
            c.setCodigo(codigoNormalizado);
        }

        c.setDescripcion(req.getDescripcion());
        c.setTipoDescuento(req.getTipoDescuento());
        c.setValor(req.getValor());
        c.setAlcance(req.getAlcance());
        c.setUsuarioAsignadoId(req.getUsuarioAsignadoId());
        c.setUsosMaximos(req.getUsosMaximos());
        c.setCursoId(req.getCursoId());
        c.setFechaInicio(req.getFechaInicio());
        c.setFechaFin(req.getFechaFin());
        return toResponse(cuponRepository.save(c));
    }

    @Override
    @Transactional
    public CuponResponse cambiarEstado(Long id, boolean activo) {
        Cupon c = buscarOFallar(id);
        c.setActivo(activo);
        return toResponse(cuponRepository.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuponUsoResponse> historialUsos(Long cuponId) {
        buscarOFallar(cuponId);
        return cuponUsoRepository.findByCupon_IdOrderByFechaUsoDesc(cuponId).stream()
                .map(this::toUsoResponse)
                .toList();
    }

    /* ───────────────────── helpers ───────────────────── */

    private Cupon buscarOFallar(Long id) {
        return cuponRepository.findById(id)
                .orElseThrow(() -> new PagosApiException(ServerApiStatusCode.CUPON_NO_EXISTE));
    }

    private void validarConfig(CuponRequest req) {
        if (req.getTipoDescuento() == TipoDescuento.PORCENTAJE
                && (req.getValor() < 1 || req.getValor() > 100)) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_CONFIG_INVALIDA,
                    "El porcentaje debe estar entre 1 y 100");
        }
        if (req.getAlcance() == AlcanceCupon.USUARIO_UNICO && req.getUsuarioAsignadoId() == null) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_CONFIG_INVALIDA,
                    "USUARIO_UNICO requiere usuarioAsignadoId");
        }
        if (req.getAlcance() == AlcanceCupon.MULTI_USO
                && (req.getUsosMaximos() == null || req.getUsosMaximos() < 1)) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_CONFIG_INVALIDA,
                    "MULTI_USO requiere usosMaximos >= 1");
        }
        if (req.getFechaInicio() != null && req.getFechaFin() != null
                && req.getFechaFin().isBefore(req.getFechaInicio())) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_CONFIG_INVALIDA,
                    "La fecha fin debe ser posterior a la fecha inicio");
        }
    }

    private CuponResponse toResponse(Cupon c) {
        return CuponResponse.builder()
                .id(c.getId())
                .codigo(c.getCodigo())
                .descripcion(c.getDescripcion())
                .tipoDescuento(c.getTipoDescuento())
                .valor(c.getValor())
                .alcance(c.getAlcance())
                .usuarioAsignadoId(c.getUsuarioAsignadoId())
                .usosMaximos(c.getUsosMaximos())
                .usosActuales(c.getUsosActuales())
                .cursoId(c.getCursoId())
                .fechaInicio(c.getFechaInicio())
                .fechaFin(c.getFechaFin())
                .activo(c.getActivo())
                .creadoPorUsuarioId(c.getCreadoPorUsuarioId())
                .fechaCreacion(c.getFechaCreacion())
                .build();
    }

    private CuponUsoResponse toUsoResponse(CuponUso u) {
        return CuponUsoResponse.builder()
                .id(u.getId())
                .cuponId(u.getCupon().getId())
                .pagoId(u.getPagoId())
                .usuarioId(u.getUsuarioId())
                .cursoId(u.getCursoId())
                .montoDescuento(u.getMontoDescuento())
                .fechaUso(u.getFechaUso())
                .build();
    }

    /* ─────────────── helpers de validación de uso ─────────────── */

    private Cupon cargarYValidar(String codigo, Long usuarioId, Long cursoId) {
        if (codigo == null || codigo.isBlank()) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_NO_EXISTE);
        }
        Cupon c = cuponRepository.findByCodigoIgnoreCase(codigo.trim().toUpperCase())
                .orElseThrow(() -> new PagosApiException(ServerApiStatusCode.CUPON_NO_EXISTE));

        if (!Boolean.TRUE.equals(c.getActivo())) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_INACTIVO);
        }
        LocalDateTime now = LocalDateTime.now();
        if (c.getFechaInicio() != null && now.isBefore(c.getFechaInicio()))
            throw new PagosApiException(ServerApiStatusCode.CUPON_FUERA_VIGENCIA);
        if (c.getFechaFin() != null && now.isAfter(c.getFechaFin()))
            throw new PagosApiException(ServerApiStatusCode.CUPON_FUERA_VIGENCIA);

        switch (c.getAlcance()) {
            case USUARIO_UNICO -> {
                if (!Objects.equals(c.getUsuarioAsignadoId(), usuarioId))
                    throw new PagosApiException(ServerApiStatusCode.CUPON_NO_ASIGNADO);
            }
            case MULTI_USO -> {
                if (c.getUsosMaximos() != null && c.getUsosActuales() >= c.getUsosMaximos())
                    throw new PagosApiException(ServerApiStatusCode.CUPON_AGOTADO);
            }
            case GLOBAL -> { /* sin tope global */ }
        }

        if (c.getAlcance() != AlcanceCupon.USUARIO_UNICO
                && cuponUsoRepository.findByCupon_IdAndUsuarioId(c.getId(), usuarioId).isPresent()) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_YA_USADO);
        }

        if (c.getCursoId() != null && !Objects.equals(c.getCursoId(), cursoId)) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_NO_APLICA_CURSO);
        }

        if (c.getTipoDescuento() == TipoDescuento.PORCENTAJE
                && (c.getValor() < 1 || c.getValor() > 100)) {
            throw new PagosApiException(ServerApiStatusCode.CUPON_CONFIG_INVALIDA);
        }
        return c;
    }

    private int calcularDescuento(Cupon c, int montoOriginal) {
        return switch (c.getTipoDescuento()) {
            case PORCENTAJE -> montoOriginal * c.getValor() / 100;
            case MONTO_FIJO -> Math.min(c.getValor(), montoOriginal);
        };
    }
}
