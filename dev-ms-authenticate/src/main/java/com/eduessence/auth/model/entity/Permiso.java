package com.eduessence.auth.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permiso", indexes = @Index(name = "uk_permiso_codigo", columnList = "codigo", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60, unique = true)
    private String codigo;

    @Column(length = 200)
    private String descripcion;
}
