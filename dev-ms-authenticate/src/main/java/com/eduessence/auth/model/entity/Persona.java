package com.eduessence.auth.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "persona")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(length = 30)
    private String documento;

    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento;

    @Column(length = 30)
    private String telefono;

    @Column(length = 60)
    private String pais;

    @Column(length = 80)
    private String ciudad;

    @Column(length = 100)
    private String profesion;

    @Column(length = 150)
    private String institucion;
}
