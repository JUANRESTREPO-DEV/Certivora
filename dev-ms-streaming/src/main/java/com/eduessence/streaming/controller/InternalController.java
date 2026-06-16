package com.eduessence.streaming.controller;

import com.eduessence.streaming.model.dto.GeneralResponseDTO;
import com.eduessence.streaming.model.dto.response.StreamSessionResponse;
import com.eduessence.streaming.service.StreamingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private static final String SERVICE = "streaming-service";
    private final StreamingService streamingService;

    @GetMapping("/streams/{id}")
    public ResponseEntity<GeneralResponseDTO<StreamSessionResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(GeneralResponseDTO.<StreamSessionResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(streamingService.obtener(id)).build());
    }
}
