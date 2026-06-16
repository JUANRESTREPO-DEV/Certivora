package com.eduessence.certificados.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneralResponseDTO<T> {
    private Integer statusCode;
    private String serviceName;
    private String message;
    private String codigoError;
    private T response;
}
