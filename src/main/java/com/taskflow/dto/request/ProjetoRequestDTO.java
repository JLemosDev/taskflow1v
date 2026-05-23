package com.taskflow.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload para criação de um novo Projeto.
 */
public record ProjetoRequestDTO(

        @NotBlank(message = "O nome do projeto é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotBlank(message = "O status é obrigatório")
        @Pattern(regexp = "PLANEJAMENTO|EM_ANDAMENTO|CONCLUIDO|CANCELADO",
                 message = "Status inválido. Use: PLANEJAMENTO, EM_ANDAMENTO, CONCLUIDO ou CANCELADO")
        String status,

        LocalDate dataInicio,

        @FutureOrPresent(message = "O prazo não pode ser uma data passada")
        LocalDate dataPrazo

) {}
