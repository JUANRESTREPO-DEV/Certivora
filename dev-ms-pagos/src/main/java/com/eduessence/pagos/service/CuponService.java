package com.eduessence.pagos.service;

import com.eduessence.pagos.model.dto.request.CuponRequest;
import com.eduessence.pagos.model.dto.response.CuponResponse;
import com.eduessence.pagos.model.dto.response.CuponUsoResponse;
import com.eduessence.pagos.model.dto.response.ValidarCuponResponse;
import com.eduessence.pagos.model.entity.Cupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CuponService {

    /* ─── Validación + uso (flujo de checkout) ─── */

    /**
     * Valida un cupón contra un usuario+curso+monto SIN consumirlo.
     * Reglas R-CUP-01 a R-CUP-12.
     */
    ValidarCuponResponse validar(String codigo, Long usuarioId, Long cursoId, int montoOriginal);

    /**
     * Reserva (incrementa usos_actuales y crea cupon_uso) cuando el pago se inicia.
     * Si el pago se rechaza/expira luego, llamar a {@link #liberar}.
     */
    Cupon reservar(String codigo, Long usuarioId, Long cursoId, Long pagoId, int montoDescuento);

    /** Libera el cupón si el pago se rechaza/expira: decrementa usos y borra cupon_uso. */
    void liberar(Long pagoId);

    /* ─── CRUD admin ─── */

    /** Listado paginado con filtros opcionales (búsqueda parcial por código/descripción). */
    Page<CuponResponse> listar(String search, String alcance, Boolean activo, Pageable pageable);

    CuponResponse obtener(Long id);

    CuponResponse crear(CuponRequest req, Long creadoPorUsuarioId);

    CuponResponse actualizar(Long id, CuponRequest req);

    /** Toggle de {@code activo} sin tocar nada más — soft enable/disable. */
    CuponResponse cambiarEstado(Long id, boolean activo);

    /** Historial de usos del cupón, más reciente primero. */
    List<CuponUsoResponse> historialUsos(Long cuponId);
}
