package com.taskflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa um Comentário feito em uma Tarefa.
 *
 * Relacionamentos:
 *   - @ManyToOne com Tarefa   (muitos comentários → uma tarefa)
 *   - @ManyToOne com Usuario  (muitos comentários → um autor)
 */
@Entity
@Table(name = "tb_comentario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Conteúdo textual do comentário.
     * Limitado a 2000 caracteres.
     */
    @Column(nullable = false, length = 2000)
    private String conteudo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    // ──────────────── Relacionamentos ────────────────────────

    /**
     * Tarefa à qual este comentário pertence.
     * LAZY: a tarefa não é carregada ao buscar um comentário.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarefa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comentario_tarefa"))
    private Tarefa tarefa;

    /**
     * Usuário que criou o comentário.
     * nullable = false: todo comentário precisa de um autor identificado.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comentario_autor"))
    private Usuario autor;

    // ──────────────── Lifecycle ──────────────────────────────

    @PrePersist
    private void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
