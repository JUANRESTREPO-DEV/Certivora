package com.eduessence.pagos.controller;

import com.eduessence.pagos.model.dto.GeneralResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Expone los datos bancarios para que el usuario pueda hacer la
 * transferencia. Se configuran vía properties:
 *
 * <pre>
 *   eduessence.pagos.banco.razon-social=Eduessence S.A.S
 *   eduessence.pagos.banco.nit=900.123.456-7
 *   eduessence.pagos.banco.banco=Bancolombia
 *   eduessence.pagos.banco.tipo-cuenta=Ahorros
 *   eduessence.pagos.banco.numero-cuenta=123-456789-01
 *   eduessence.pagos.banco.titular=Eduessence S.A.S
 *   eduessence.pagos.banco.contacto=pagos@eduessence.com
 *   eduessence.pagos.banco.instrucciones=...
 * </pre>
 *
 * En el futuro podríamos guardarlos en BD para que admin los edite desde UI;
 * por ahora properties es suficiente y se redespliegan rápido.
 */
@Tag(name = "Datos bancarios")
@RestController
@RequestMapping("/api/public/datos-bancarios")
@RequiredArgsConstructor
public class DatosBancariosController {

    private static final String SERVICE = "pagos-service";

    @Value("${eduessence.pagos.banco.razon-social:Eduessence S.A.S}")
    private String razonSocial;

    @Value("${eduessence.pagos.banco.nit:}")
    private String nit;

    @Value("${eduessence.pagos.banco.banco:Bancolombia}")
    private String banco;

    @Value("${eduessence.pagos.banco.tipo-cuenta:Ahorros}")
    private String tipoCuenta;

    @Value("${eduessence.pagos.banco.numero-cuenta:}")
    private String numeroCuenta;

    @Value("${eduessence.pagos.banco.titular:Eduessence S.A.S}")
    private String titular;

    @Value("${eduessence.pagos.banco.contacto:pagos@eduessence.com}")
    private String contacto;

    @Value("${eduessence.pagos.banco.instrucciones:Realiza la transferencia y sube tu comprobante. Te confirmamos por email cuando lo validemos.}")
    private String instrucciones;

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<Map<String, String>>> obtener() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("razonSocial", razonSocial);
        data.put("nit", nit);
        data.put("banco", banco);
        data.put("tipoCuenta", tipoCuenta);
        data.put("numeroCuenta", numeroCuenta);
        data.put("titular", titular);
        data.put("contacto", contacto);
        data.put("instrucciones", instrucciones);
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, String>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(data).build());
    }
}
