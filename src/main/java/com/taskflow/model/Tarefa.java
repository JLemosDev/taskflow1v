package com.taskflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma Tarefa dentro de um Projeto no TaskFlow.
 *
 * Relacionamentos:
 *   - @ManyToOne com Projeto  (muitas tarefas → um projeto)
 *   - @ManyToOne com Usuario  (muitas tarefas → um responsável)
 *   - @OneToMany com Comentario (uma tarefa → muitos comentários)
 */
@Entity
@Table(name = "tb_tarefa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    /**
     * Status atual da tarefa usando o enum TaskStatus.
     * Persiste o nome do enum (ex: "IN_PROGRESS") na coluna.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    /**
     * Prioridade da tarefa usando o enum Priority.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Priority prioridade = Priority.MEDIUM;

    /** Data de entrega prevista para a tarefa. */
    @Column(name = "data_entrega")
    private LocalDate dataEntrega;

    /** Estimativa de esforço em horas. */
    @Column(name = "horas_estimadas")
    private Integer horasEstimadas;

    /** Horas efetivamente trabalhadas na tarefa. */
    @Column(name = "horas_trabalhadas")
    @Builder.Default
    private Integer horasTrabalhadas = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column
    private LocalDateTime atualizadoEm;

    // ──────────────── Relacionamentos ────────────────────────

    /**
     * Projeto ao qual esta tarefa pertence.
     * LAZY: o projeto não é carregado automaticamente junto com a tarefa.
     * nullable = false: toda tarefa deve pertencer a um projeto.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tarefa_projeto"))
    private Projeto projeto;

    /**
     * Usuário responsável pela execução desta tarefa.
     * nullable = true: uma tarefa pode estar sem responsável (não atribuída).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "responsavel_id", nullable = true,
                foreignKey = @ForeignKey(name = "fk_tarefa_responsavel"))
    private Usuario responsavel;

    /**
     * Comentários feitos nesta tarefa.
     * orphanRemoval = true: comentários removidos da lista são deletados.
     */
    @OneToMany(mappedBy = "tarefa",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<Comentario> comentarios = new ArrayList<>();

    // ──────────────── Lifecycle ──────────────────────────────

    @PrePersist
    private void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
