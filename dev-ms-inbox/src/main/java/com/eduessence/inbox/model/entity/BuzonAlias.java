package com.eduessence.inbox.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "buzon_alias")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BuzonAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buzon_id", nullable = false)
    private Buzon buzon;

    @Column(nullable = false, length = 150, unique = true)
    private String direccion;
}
