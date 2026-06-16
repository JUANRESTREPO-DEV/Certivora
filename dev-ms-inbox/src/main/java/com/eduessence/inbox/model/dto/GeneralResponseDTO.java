package com.eduessence.inbox.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralResponseDTO<T> {
    private Integer statusCode;
    private String serviceName;
    private String message;
    private T response;
}
