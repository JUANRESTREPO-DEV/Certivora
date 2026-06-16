package com.eduessence.sendmail.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla de email. El HTML real vive en S3 ({@code pathTemplate} es la
 * key del objeto). En BD solo se guarda el metadata y la lista de variables.
 */
@Entity
@Table(name = "email_template", indexes = @Index(name = "uk_template_nombre", columnList = "nombre", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    /** Key del objeto en S3 (ej: {@code templates/welcome.html}). */
    @Column(name = "path_template", nullable = false, length = 500)
    private String pathTemplate;

    @Column(length = 200)
    private String bucket;

    @Column(name = "asunto_default", length = 200)
    private String asuntoDefault;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EmailTemplateVariable> variables = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
        if (this.activo == null) this.activo = true;
    }

    @PreUpdate
    void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
