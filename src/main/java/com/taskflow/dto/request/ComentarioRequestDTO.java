package com.taskflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload para criação de um Comentário em uma Tarefa.
 */
public record ComentarioRequestDTO(

        @NotBlank(message = "O conteúdo do comentário é obrigatório")
        @Size(min = 1, max = 2000, message = "O comentário deve ter entre 1 e 2000 caracteres")
        String conteudo,

        @NotNull(message = "O ID da tarefa é obrigatório")
        Long tarefaId,

        @NotNull(message = "O ID do autor é obrigatório")
        Long autorId

) {}
