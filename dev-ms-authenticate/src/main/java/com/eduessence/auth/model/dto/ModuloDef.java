package com.eduessence.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Sidebar: módulo + sus submódulos visibles para el usuario. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuloDef {
    private Long id;
    private String codigo;
    private String label;
    private String icon;
    private Integer orden;
    private List<SubmoduloDef> submodulos;
}
