package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.response.CursoResponse;
import com.eduessence.cursos.service.CursoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Cursos públicos")
@RestController
@RequestMapping("/api/public/cursos")
@RequiredArgsConstructor
public class PublicCursoController {

    private static final String SERVICE = "cursos-service";
    private final CursoService cursoService;

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<CursoResponse>>> listarActivos() {
        return ResponseEntity.ok(GeneralResponseDTO.<List<CursoResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(cursoService.listarActivos()).build());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<GeneralResponseDTO<CursoResponse>> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(GeneralResponseDTO.<CursoResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(cursoService.obtenerPorSlug(slug)).build());
    }
}
