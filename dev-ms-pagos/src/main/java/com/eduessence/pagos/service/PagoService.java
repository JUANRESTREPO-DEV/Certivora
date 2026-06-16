package com.eduessence.pagos.service;

import com.eduessence.pagos.model.dto.request.ConfirmarPagoRequest;
import com.eduessence.pagos.model.dto.request.IniciarPagoRequest;
import com.eduessence.pagos.model.dto.response.IniciarPagoResponse;
import com.eduessence.pagos.model.dto.response.PagoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PagoService {

    IniciarPagoResponse iniciar(Long usuarioId, IniciarPagoRequest request);

    PagoResponse confirmarPago(Long pagoId, ConfirmarPagoRequest request);

    PagoResponse aprobar(Long pagoId, Long adminId, String observacion);

    PagoResponse rechazar(Long pagoId, Long adminId, String observacion);

    /** Llamado por dev-ms-jobs cada hora para expirar reservas vencidas. */
    int expirarVencidos();

    PagoResponse obtener(Long pagoId);

    List<PagoResponse> misPagos(Long usuarioId);

    /**
     * Sube el comprobante de transferencia bancaria del usuario, asocia la
     * URL al pago y deja el pago en {@code PENDIENTE_CONFIRMACION} para que
     * un admin lo verifique.
     */
    PagoResponse subirComprobante(Long pagoId, Long usuarioId, MultipartFile file, String observacion);

    /** Listado para admin: pagos en {@code PENDIENTE_CONFIRMACION}. */
    List<PagoResponse> listarPendientesConfirmacion();
}
