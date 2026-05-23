package com.taskflow.dto.request;

import com.taskflow.model.Priority;
import com.taskflow.model.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload para atualização parcial de uma Tarefa (PATCH).
 * Todos os campos são opcionais — apenas os informados serão atualizados.
 */
public record TarefaUpdateRequestDTO(

        @Size(min = 3, max = 200, message = "O título deve ter entre 3 e 200 caracteres")
        String titulo,

        @Size(max = 1000)
        String descricao,

        TaskStatus status,

        Priority prioridade,

        LocalDate dataEntrega,

        @Min(1) @Max(999)
        Integer horasEstimadas,

        @Min(value = 0, message = "Horas trabalhadas não pode ser negativo")
        Integer horasTrabalhadas,

        Long responsavelId

) {}
