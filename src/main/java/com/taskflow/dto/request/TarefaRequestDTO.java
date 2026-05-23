package com.taskflow.dto.request;

import com.taskflow.model.Priority;
import com.taskflow.model.TaskStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Payload para criação de uma nova Tarefa.
 */
public record TarefaRequestDTO(

        @NotBlank(message = "O título é obrigatório")
        @Size(min = 3, max = 200, message = "O título deve ter entre 3 e 200 caracteres")
        String titulo,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao,

        // Se não informado, o Service aplica o default TODO
        TaskStatus status,

        // Se não informado, o Service aplica o default MEDIUM
        Priority prioridade,

        @Future(message = "A data de entrega deve ser uma data futura")
        LocalDate dataEntrega,

        @Min(value = 1, message = "Horas estimadas deve ser no mínimo 1")
        @Max(value = 999, message = "Horas estimadas deve ser no máximo 999")
        Integer horasEstimadas,

        @NotNull(message = "O ID do projeto é obrigatório")
        Long projetoId,

        // Opcional: tarefa pode ser criada sem responsável
        Long responsavelId

) {}
