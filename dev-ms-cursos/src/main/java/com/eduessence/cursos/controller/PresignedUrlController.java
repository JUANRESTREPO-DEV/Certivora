package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.PresignedUrlRequest;
import com.eduessence.cursos.model.dto.response.PresignedUrlResponse;
import com.eduessence.cursos.service.S3PresignedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de subida directa a S3 sin estar asociados a un recurso persistido.
 *
 * <p>Caso típico: el form de "Nuevo curso" necesita subir video/imagen de
 * presentación ANTES de tener el {@code curso.id}. El front llama a
 * {@code POST /api/uploads/presigned-url}, recibe la URL firmada, sube los
 * bytes, y guarda la {@code s3Key} en el body del POST que crea el curso.
 *
 * <p>Para recursos asociados a un curso ya existente, usar el endpoint
 * {@code /api/cursos/{id}/recursos/presigned-url}.
 */
@Tag(name = "Uploads")
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class PresignedUrlController {

    private static final String SERVICE = "cursos-service";
    private final S3PresignedService s3;

    /**
     * Presigned URL para subidas pre-curso. Usa el path lógico
     * {@code cursos/_temp/{carpeta}/...}; el front "mueve" el archivo a su
     * ubicación final guardándolo en {@code curso.video_presentacion_url} o
     * {@code imagen_presentacion_url} al crear el curso.
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<GeneralResponseDTO<PresignedUrlResponse>> presigned(
            @Valid @RequestBody PresignedUrlRequest req) {
        // 0 = "temp" — el service usa este número como prefijo cursos/0/...
        return ResponseEntity.ok(
                GeneralResponseDTO.<PresignedUrlResponse>builder()
                        .statusCode(200).serviceName(SERVICE)
                        .message("URL pre-firmada generada")
                        .response(s3.generarUploadUrl(0L, req))
                        .build());
    }

    /**
     * Variante explícita para recursos vinculados a un curso ya existente.
     * Reutilizada por {@link CursoRecursoController#presignedUrl} también.
     */
    @PostMapping("/cursos/{cursoId}/presigned-url")
    public ResponseEntity<GeneralResponseDTO<PresignedUrlResponse>> presignedCurso(
            @PathVariable Long cursoId,
            @Valid @RequestBody PresignedUrlRequest req) {
        return ResponseEntity.ok(
                GeneralResponseDTO.<PresignedUrlResponse>builder()
                        .statusCode(200).serviceName(SERVICE)
                        .message("URL pre-firmada generada")
                        .response(s3.generarUploadUrl(cursoId, req))
                        .build());
    }

    /* ──────────── Upload directo vía backend (multipart) ────────────
     * El binario viaja por el backend → sin CORS en S3 + auth/validación
     * server-side. Es el flujo estándar usado por el FileUploader del front.
     */

    /** Subida temporal pre-creación de curso (cursoId = 0). */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GeneralResponseDTO<PresignedUrlResponse>> subirFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("carpeta") String carpeta) {
        return ResponseEntity.ok(
                GeneralResponseDTO.<PresignedUrlResponse>builder()
                        .statusCode(200).serviceName(SERVICE)
                        .message("Archivo subido")
                        .response(s3.subirArchivo(0L, file, carpeta))
                        .build());
    }

    /** Subida asociada a un curso ya existente. */
    @PostMapping(value = "/cursos/{cursoId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GeneralResponseDTO<PresignedUrlResponse>> subirFileCurso(
            @PathVariable Long cursoId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("carpeta") String carpeta) {
        return ResponseEntity.ok(
                GeneralResponseDTO.<PresignedUrlResponse>builder()
                        .statusCode(200).serviceName(SERVICE)
                        .message("Archivo subido")
                        .response(s3.subirArchivo(cursoId, file, carpeta))
                        .build());
    }
}
