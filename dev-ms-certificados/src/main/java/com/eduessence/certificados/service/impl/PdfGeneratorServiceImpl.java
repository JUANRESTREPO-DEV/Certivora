package com.eduessence.certificados.service.impl;

import com.eduessence.certificados.exception.CertificadosApiException;
import com.eduessence.certificados.exception.ServerApiStatusCode;
import com.eduessence.certificados.model.entity.DiplomaTemplate;
import com.eduessence.certificados.service.PdfGeneratorService;
import com.eduessence.certificados.service.S3StorageService;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Genera el PDF combinando:
 *   - Template base (imagen PNG/JPG en S3) como fondo
 *   - Texto del nombre del participante
 *   - Texto del curso
 *   - Código + fecha
 *   - QR de verificación
 *
 * Las coordenadas vienen del JSON {@code template.posiciones}:
 *   {
 *     "nombre":   { "x": 200, "y": 350, "fontSize": 32 },
 *     "curso":    { "x": 200, "y": 280, "fontSize": 20 },
 *     "codigo":   { "x":  60, "y":  60, "fontSize": 10 },
 *     "fecha":    { "x": 600, "y":  60, "fontSize": 10 },
 *     "qr":       { "x": 700, "y": 100, "size": 100 }
 *   }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

    private final S3StorageService s3;

    @Override
    public byte[] generar(DiplomaTemplate template,
                          String nombreCompleto,
                          String numeroDocumento,
                          String nombreCurso,
                          String codigo,
                          String fechaFormateada,
                          byte[] qrPng) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(out)) {

            JSONObject posiciones = template.getPosiciones() == null || template.getPosiciones().isBlank()
                    ? new JSONObject()
                    : new JSONObject(template.getPosiciones());

            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4.rotate()); // horizontal

            Document doc = new Document(pdf);
            doc.setMargins(0, 0, 0, 0);

            // 1. Fondo
            try {
                byte[] templateBytes = s3.descargarTemplate(template.getTemplateUrl());
                ImageData bg = ImageDataFactory.create(templateBytes);
                Image bgImg = new Image(bg);
                bgImg.setFixedPosition(0, 0);
                bgImg.scaleAbsolute(pdf.getDefaultPageSize().getWidth(), pdf.getDefaultPageSize().getHeight());
                doc.add(bgImg);
            } catch (Exception ex) {
                log.warn("No se pudo cargar template de fondo, generando sin fondo: {}", ex.getMessage());
            }

            // 2. Nombre
            addText(doc, posiciones, "nombre", nombreCompleto, 32);
            // 3. Documento (prefix tipicamente "NIUP " — viene en el JSON)
            if (numeroDocumento != null && !numeroDocumento.isBlank()) {
                addText(doc, posiciones, "documento", numeroDocumento, 18);
            }
            // 4. Curso
            addText(doc, posiciones, "curso", nombreCurso, 20);
            // 5. Código
            addText(doc, posiciones, "codigo", codigo, 10);
            // 6. Fecha
            addText(doc, posiciones, "fecha", fechaFormateada, 10);

            // 7. QR — solo si el editor visual lo arrastró al canvas.
            JSONObject qrPos = posiciones.optJSONObject("qr");
            if (qrPos != null && qrPng != null && qrPng.length > 0) {
                float x = qrPos.optFloat("x", 700f);
                float y = qrPos.optFloat("y", 80f);
                float size = qrPos.optFloat("size", 100f);
                float pageW = pdf.getDefaultPageSize().getWidth();
                float pageH = pdf.getDefaultPageSize().getHeight();
                boolean esPct = x <= 100f && y <= 100f && size <= 100f;
                if (esPct) {
                    float sizePx = (size / 100f) * pageW;
                    float xPx = (x / 100f) * pageW;
                    float yPx = ((100f - y) / 100f) * pageH;
                    Image qrImg = new Image(ImageDataFactory.create(qrPng));
                    qrImg.setFixedPosition(xPx - sizePx / 2f, yPx - sizePx / 2f);
                    qrImg.scaleAbsolute(sizePx, sizePx);
                    doc.add(qrImg);
                } else {
                    Image qrImg = new Image(ImageDataFactory.create(qrPng));
                    qrImg.setFixedPosition(x, y);
                    qrImg.scaleAbsolute(size, size);
                    doc.add(qrImg);
                }
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Error generando PDF: {}", ex.getMessage(), ex);
            throw new CertificadosApiException(ServerApiStatusCode.ERROR_PDF, ex);
        }
    }

    private void addText(Document doc, JSONObject posiciones, String campo, String texto, float defaultFontSize) {
        if (texto == null) return;
        JSONObject pos = posiciones.optJSONObject(campo);
        // Si la variable NO fue arrastrada al canvas por el editor visual, no la
        // pintamos. Antes caíamos a coords default (x=100, y=400) y aparecía
        // texto extraviado en el borde de todos los certificados.
        if (pos == null) return;
        float x = pos.optFloat("x", 100f);
        float y = pos.optFloat("y", 400f);
        // Acepta tanto "size" (formato nuevo del editor visual) como "fontSize" (formato legado).
        float fontSize = pos.optFloat("size", pos.optFloat("fontSize", defaultFontSize));
        float width = pos.optFloat("width", 500f);
        String prefix = pos.optString("prefix", "");
        String alignStr = pos.optString("align", "left");
        String colorHex = pos.optString("color", "");

        TextAlignment align = switch (alignStr.toLowerCase()) {
            case "center" -> TextAlignment.CENTER;
            case "right" -> TextAlignment.RIGHT;
            default -> TextAlignment.LEFT;
        };

        Color color = parseColor(colorHex);

        // Si x/y están en porcentaje (0-100) los convertimos a coords del PDF (A4 landscape).
        float pageW = doc.getPdfDocument().getDefaultPageSize().getWidth();
        float pageH = doc.getPdfDocument().getDefaultPageSize().getHeight();
        boolean esPct = x <= 100f && y <= 100f && width <= 100f;
        if (esPct) {
            x = (x / 100f) * pageW;
            // El editor visual usa Y con origen arriba; PDF tiene origen abajo
            y = ((100f - y) / 100f) * pageH;
            width = (width / 100f) * pageW;
        }

        Paragraph p = new Paragraph(prefix + texto)
                .setFontSize(fontSize)
                .setFontColor(color)
                .setTextAlignment(align)
                .setFixedPosition(x - width / 2f, y - fontSize, width);
        doc.add(p);
    }

    private static Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) return ColorConstants.BLACK;
        try {
            String h = hex.trim();
            if (h.startsWith("#")) h = h.substring(1);
            if (h.length() == 6) {
                int r = Integer.parseInt(h.substring(0, 2), 16);
                int g = Integer.parseInt(h.substring(2, 4), 16);
                int b = Integer.parseInt(h.substring(4, 6), 16);
                return new DeviceRgb(r, g, b);
            }
        } catch (Exception ignored) {}
        return ColorConstants.BLACK;
    }
}
