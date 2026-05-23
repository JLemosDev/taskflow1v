package com.taskflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um usuário do sistema TaskFlow.
 * Um usuário pode ter várias tarefas atribuídas a ele.
 *
 * Relacionamentos:
 *   - @OneToMany com Tarefa (um usuário → muitas tarefas)
 */
@Entity
@Table(name = "tb_usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    /**
     * Papel do usuário no sistema (ex: ADMIN, MEMBRO).
     * Armazenado como String para facilitar leitura no banco.
     */
    @Column(nullable = false, length = 30)
    private String role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    /**
     * Um usuário pode ter zero ou muitas tarefas atribuídas.
     * CascadeType.ALL para que tarefas órfãs sejam removidas junto.
     * FetchType.LAZY: tarefas só carregadas quando explicitamente acessadas.
     */
    @OneToMany(mappedBy = "responsavel",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = false)
    @Builder.Default
    private List<Tarefa> tarefas = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
