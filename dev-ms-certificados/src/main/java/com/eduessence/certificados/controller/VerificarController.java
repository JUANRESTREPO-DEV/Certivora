package com.eduessence.certificados.controller;

import com.eduessence.certificados.model.dto.GeneralResponseDTO;
import com.eduessence.certificados.model.dto.response.VerificacionResponse;
import com.eduessence.certificados.service.CertificadoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público sin auth: lo apunta el QR del PDF.
 * El gateway lo deja pasar via {@code /certificados/api/verificar/**} en public-paths.
 *
 * <p>Comportamiento:</p>
 * <ul>
 *   <li>Si la request acepta {@code application/json} (lo llama el front
 *       desde la página de verificación), devuelve el JSON con la info.</li>
 *   <li>Si la request acepta {@code text/html} (lo abrió un navegador desde
 *       el QR de un PDF emitido antes del cambio), redirige 302 a la página
 *       del front para que el usuario vea la UI bonita.</li>
 * </ul>
 */
@Tag(name = "Verificación pública")
@RestController
@RequestMapping("/api/verificar")
@RequiredArgsConstructor
public class VerificarController {

    private static final String SERVICE = "certificados-service";
    private final CertificadoService certificadoService;

    @Value("${eduessence.certificados.url-verificacion-base}")
    private String urlVerificacionBase;

    @GetMapping("/{codigo}")
    public ResponseEntity<?> verificar(
            @PathVariable String codigo,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {

        // Backward-compat: si el QR de un certificado viejo apunta a este
        // endpoint y un navegador lo abre directo (Accept: text/html), lo
        // redirigimos a la página pública del front.
        boolean prefiereHtml = accept != null
                && accept.contains(MediaType.TEXT_HTML_VALUE)
                && !accept.contains(MediaType.APPLICATION_JSON_VALUE);
        if (prefiereHtml) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, urlVerificacionBase + codigo)
                    .build();
        }

        VerificacionResponse data = certificadoService.verificar(codigo);
        return ResponseEntity.ok(GeneralResponseDTO.<VerificacionResponse>builder()
                .statusCode(200).serviceName(SERVICE).message(data.getMensaje())
                .response(data).build());
    }
}
