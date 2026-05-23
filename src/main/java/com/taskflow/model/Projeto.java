package com.taskflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um Projeto gerenciado pelo TaskFlow.
 *
 * Relacionamentos:
 *   - @OneToMany com Tarefa (um projeto → muitas tarefas)
 */
@Entity
@Table(name = "tb_projeto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 500)
    private String descricao;

    /**
     * Status do projeto: PLANEJAMENTO, EM_ANDAMENTO, CONCLUIDO, CANCELADO.
     * Mantido como String para evitar migrações ao adicionar novos valores.
     */
    @Column(nullable = false, length = 30)
    private String status;

    /** Data de início prevista ou real do projeto. */
    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    /** Prazo final do projeto. */
    @Column(name = "data_prazo")
    private LocalDate dataPrazo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    /**
     * Um projeto contém zero ou muitas tarefas.
     * orphanRemoval = true: ao remover uma tarefa da lista, ela é deletada do banco.
     */
    @OneToMany(mappedBy = "projeto",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<Tarefa> tarefas = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
