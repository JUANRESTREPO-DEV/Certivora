package com.eduessence.sendmail.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Variable esperada por un template. Define el contrato:
 * - {@code nombre} debe coincidir con un placeholder Velocity {@code ${nombre}}
 * - {@code requerido = true} → debe venir en el request o se rechaza
 * - {@code valorDefecto} → si no viene en el request y no es requerida, se usa
 */
@Entity
@Table(name = "email_template_variable", uniqueConstraints = @UniqueConstraint(
        name = "uk_etv_template_nombre",
        columnNames = {"template_id", "nombre"}
), indexes = @Index(name = "idx_etv_template", columnList = "template_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate template;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "tipo_dato", nullable = false, length = 30)
    private String tipoDato;

    @Column(nullable = false)
    private Boolean requerido;

    @Column(name = "valor_defecto", length = 500)
    private String valorDefecto;

    @Column(length = 300)
    private String descripcion;

    @PrePersist
    void onCreate() {
        if (this.requerido == null) this.requerido = true;
        if (this.tipoDato == null) this.tipoDato = "STRING";
    }
}
