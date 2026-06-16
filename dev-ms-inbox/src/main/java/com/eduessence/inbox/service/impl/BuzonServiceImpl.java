package com.eduessence.inbox.service.impl;

import com.eduessence.inbox.exception.InboxApiException;
import com.eduessence.inbox.exception.ServerApiStatusCode;
import com.eduessence.inbox.model.dto.request.ActualizarBuzonRequest;
import com.eduessence.inbox.model.dto.request.CrearAliasRequest;
import com.eduessence.inbox.model.dto.request.CrearBuzonRequest;
import com.eduessence.inbox.model.dto.request.OtorgarAccesoRequest;
import com.eduessence.inbox.model.dto.response.BuzonResponse;
import com.eduessence.inbox.model.entity.Buzon;
import com.eduessence.inbox.model.entity.BuzonAcceso;
import com.eduessence.inbox.model.entity.BuzonAlias;
import com.eduessence.inbox.model.enums.Carpeta;
import com.eduessence.inbox.model.enums.RolBuzon;
import com.eduessence.inbox.model.enums.TipoBuzon;
import com.eduessence.inbox.repository.BuzonAccesoRepository;
import com.eduessence.inbox.repository.BuzonAliasRepository;
import com.eduessence.inbox.repository.BuzonRepository;
import com.eduessence.inbox.repository.MensajeRepository;
import com.eduessence.inbox.service.BuzonService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Reglas del dominio:
 *  - R-BUZ-01: dirección/alias únicos
 *  - R-BUZ-02: la dirección debe pertenecer a alguno de los dominios corporativos
 *  - R-BUZ-03: solo ADMIN o GERENTE pueden crear/editar/eliminar (impuesto en el
 *    controlador vía {@link com.eduessence.inbox.common.RequestContext})
 *  - R-BUZ-04: un buzón inactivo no acepta correos entrantes (se descarta)
 *  - R-BUZ-05: SOLO_SALIDA no aparece en el ruteo de entrada
 *  - R-BUZ-06: el CATCHALL es fallback cuando no hay match exacto
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuzonServiceImpl implements BuzonService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BuzonRepository buzonRepository;
    private final BuzonAliasRepository aliasRepository;
    private final BuzonAccesoRepository accesoRepository;
    private final MensajeRepository mensajeRepository;

    /** Lista de dominios aceptados, separados por coma. Ej: "eduessence.com,eduessence.co". */
    @Value("${eduessence.inbox.dominios-corporativos}")
    private String dominiosCorporativos;

    @Override
    @Transactional
    public BuzonResponse crear(CrearBuzonRequest req, Long usuarioCreador) {
        String direccion = normalizarDireccion(req.getDireccion());
        validarDominio(direccion);

        if (buzonRepository.existsByDireccionIgnoreCase(direccion)
                || aliasRepository.existsByDireccionIgnoreCase(direccion))
            throw new InboxApiException(ServerApiStatusCode.DIRECCION_DUPLICADA);

        Buzon b = Buzon.builder()
                .direccion(direccion)
                .nombreMostrar(req.getNombreMostrar())
                .tipo(req.getTipo())
                .referenciaId(req.getReferenciaId())
                .descripcion(req.getDescripcion())
                .firma(req.getFirma())
                .expiraEn(req.getExpiraEn())
                .forwardExternosJson(serializarLista(req.getForwardExternos()))
                .creadoPorUsuarioId(usuarioCreador)
                .activo(true)
                .build();
        b = buzonRepository.save(b);

        // El creador queda como ADMIN_BUZON automáticamente
        accesoRepository.save(BuzonAcceso.builder()
                .buzon(b)
                .usuarioId(usuarioCreador)
                .rol(RolBuzon.ADMIN_BUZON)
                .creadoPorUsuarioId(usuarioCreador)
                .build());

        log.info("Buzón creado direccion={} tipo={} por usuarioId={}", direccion, req.getTipo(), usuarioCreador);
        return toResponse(b);
    }

    @Override
    @Transactional(readOnly = true)
    public BuzonResponse obtener(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Override
    @Transactional
    public BuzonResponse actualizar(Long id, ActualizarBuzonRequest req) {
        Buzon b = getOrThrow(id);
        if (req.getNombreMostrar() != null) b.setNombreMostrar(req.getNombreMostrar());
        if (req.getDescripcion() != null) b.setDescripcion(req.getDescripcion());
        if (req.getForwardExternos() != null) b.setForwardExternosJson(serializarLista(req.getForwardExternos()));
        if (req.getFirma() != null) b.setFirma(req.getFirma());
        if (req.getExpiraEn() != null) b.setExpiraEn(req.getExpiraEn());
        if (req.getActivo() != null) b.setActivo(req.getActivo());
        return toResponse(buzonRepository.save(b));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Buzon b = getOrThrow(id);
        // Borrado lógico — no destruimos histórico de mensajes
        b.setActivo(false);
        buzonRepository.save(b);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuzonResponse> listar(Boolean soloActivos, Pageable pageable) {
        Page<Buzon> page = (soloActivos != null && soloActivos)
                ? buzonRepository.findAllByActivo(true, pageable)
                : buzonRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuzonResponse> misBuzones(Long usuarioId) {
        return accesoRepository.findAllByUsuarioId(usuarioId).stream()
                .map(BuzonAcceso::getBuzon)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BuzonResponse crearAlias(CrearAliasRequest req) {
        Buzon b = getOrThrow(req.getBuzonId());
        String alias = normalizarDireccion(req.getDireccion());
        validarDominio(alias);

        if (buzonRepository.existsByDireccionIgnoreCase(alias)
                || aliasRepository.existsByDireccionIgnoreCase(alias))
            throw new InboxApiException(ServerApiStatusCode.DIRECCION_DUPLICADA);

        aliasRepository.save(BuzonAlias.builder().buzon(b).direccion(alias).build());
        return toResponse(b);
    }

    @Override
    @Transactional
    public void eliminarAlias(Long aliasId) {
        BuzonAlias a = aliasRepository.findById(aliasId)
                .orElseThrow(() -> new InboxApiException(ServerApiStatusCode.ALIAS_NO_ENCONTRADO));
        aliasRepository.delete(a);
    }

    @Override
    @Transactional
    public BuzonResponse otorgarAcceso(Long buzonId, OtorgarAccesoRequest req, Long usuarioCreador) {
        Buzon b = getOrThrow(buzonId);
        var existente = accesoRepository.findByBuzonIdAndUsuarioId(buzonId, req.getUsuarioId());
        if (existente.isPresent()) {
            existente.get().setRol(req.getRol());
            accesoRepository.save(existente.get());
        } else {
            accesoRepository.save(BuzonAcceso.builder()
                    .buzon(b)
                    .usuarioId(req.getUsuarioId())
                    .rol(req.getRol())
                    .creadoPorUsuarioId(usuarioCreador)
                    .build());
        }
        return toResponse(b);
    }

    @Override
    @Transactional
    public void revocarAcceso(Long buzonId, Long usuarioId) {
        accesoRepository.deleteByBuzonIdAndUsuarioId(buzonId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Buzon resolverBuzonPorDireccion(String direccion) {
        String d = normalizarDireccion(direccion);

        Optional<Buzon> exacto = buzonRepository.findByDireccionIgnoreCase(d);
        if (exacto.isPresent() && exacto.get().getActivo()) return exacto.get();

        Optional<BuzonAlias> alias = aliasRepository.findByDireccionIgnoreCase(d);
        if (alias.isPresent() && alias.get().getBuzon().getActivo()) return alias.get().getBuzon();

        // Catch-all como fallback
        return buzonRepository.findFirstByTipoAndActivoTrue(TipoBuzon.CATCHALL).orElse(null);
    }

    /* ───────────── helpers ───────────── */

    private Buzon getOrThrow(Long id) {
        return buzonRepository.findById(id)
                .orElseThrow(() -> new InboxApiException(ServerApiStatusCode.BUZON_NO_ENCONTRADO));
    }

    private void validarDominio(String direccion) {
        String dominio = direccion.substring(direccion.indexOf('@') + 1);
        List<String> permitidos = List.of(dominiosCorporativos.split(","));
        boolean ok = permitidos.stream().map(String::trim).anyMatch(p -> p.equalsIgnoreCase(dominio));
        if (!ok)
            throw new InboxApiException(ServerApiStatusCode.DOMINIO_INVALIDO,
                    "Dominio " + dominio + " no es corporativo. Permitidos: " + permitidos);
    }

    private static String normalizarDireccion(String d) {
        if (d == null || !d.contains("@"))
            throw new InboxApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Dirección inválida");
        return d.trim().toLowerCase();
    }

    private static String serializarLista(List<String> lista) {
        if (lista == null || lista.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(lista);
        } catch (JsonProcessingException ex) {
            throw new InboxApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Error serializando lista");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> deserializarLista(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, List.class);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private BuzonResponse toResponse(Buzon b) {
        long noLeidos = mensajeRepository.countByBuzonIdAndCarpetaAndLeidoFalse(b.getId(), Carpeta.INBOX);
        List<String> aliases = new ArrayList<>(
                aliasRepository.findAllByBuzonId(b.getId()).stream().map(BuzonAlias::getDireccion).toList());
        List<BuzonResponse.AccesoResponse> accesos = accesoRepository.findAllByBuzonId(b.getId()).stream()
                .map(a -> BuzonResponse.AccesoResponse.builder()
                        .usuarioId(a.getUsuarioId())
                        .rol(a.getRol().name())
                        .build())
                .toList();

        return BuzonResponse.builder()
                .id(b.getId())
                .direccion(b.getDireccion())
                .nombreMostrar(b.getNombreMostrar())
                .tipo(b.getTipo())
                .referenciaId(b.getReferenciaId())
                .descripcion(b.getDescripcion())
                .activo(b.getActivo())
                .expiraEn(b.getExpiraEn())
                .forwardExternos(deserializarLista(b.getForwardExternosJson()))
                .firma(b.getFirma())
                .aliases(aliases)
                .accesos(accesos)
                .noLeidos(noLeidos)
                .creadoPorUsuarioId(b.getCreadoPorUsuarioId())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
