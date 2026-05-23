package com.taskflow.dto.response;

import com.taskflow.model.Priority;
import com.taskflow.model.Tarefa;
import com.taskflow.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dados da Tarefa retornados pela API.
 * Inclui referência resumida do Projeto e do Responsável (sem expor entidades JPA).
 */
public record TarefaResponseDTO(
        Long id,
        String titulo,
        String descricao,
        TaskStatus status,
        Priority prioridade,
        LocalDate dataEntrega,
        Integer horasEstimadas,
        Integer horasTrabalhadas,
        boolean atrasada,

        // Referência ao Projeto (apenas id + nome)
        Long projetoId,
        String projetoNome,

        // Referência ao Responsável (apenas id + nome, pode ser null)
        Long responsavelId,
        String responsavelNome,

        int totalComentarios,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static TarefaResponseDTO from(Tarefa t) {
        boolean atrasada = t.getDataEntrega() != null
                && t.getDataEntrega().isBefore(LocalDate.now())
                && t.getStatus() != TaskStatus.DONE
                && t.getStatus() != TaskStatus.CANCELLED;

        return new TarefaResponseDTO(
                t.getId(),
                t.getTitulo(),
                t.getDescricao(),
                t.getStatus(),
                t.getPrioridade(),
                t.getDataEntrega(),
                t.getHorasEstimadas(),
                t.getHorasTrabalhadas(),
                atrasada,
                t.getProjeto().getId(),
                t.getProjeto().getNome(),
                t.getResponsavel() != null ? t.getResponsavel().getId() : null,
                t.getResponsavel() != null ? t.getResponsavel().getNome() : null,
                t.getComentarios().size(),
                t.getCriadoEm(),
                t.getAtualizadoEm()
        );
    }
}
