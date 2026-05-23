package com.taskflow.dto.response;

import com.taskflow.model.Projeto;
import com.taskflow.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dados do Projeto retornados pela API.
 */
public record ProjetoResponseDTO(
        Long id,
        String nome,
        String descricao,
        String status,
        LocalDate dataInicio,
        LocalDate dataPrazo,
        int totalTarefas,
        long tarefasConcluidas,
        LocalDateTime criadoEm
) {
    public static ProjetoResponseDTO from(Projeto p) {
        long concluidas = p.getTarefas().stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        return new ProjetoResponseDTO(
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                p.getStatus(),
                p.getDataInicio(),
                p.getDataPrazo(),
                p.getTarefas().size(),
                concluidas,
                p.getCriadoEm()
        );
    }
}
