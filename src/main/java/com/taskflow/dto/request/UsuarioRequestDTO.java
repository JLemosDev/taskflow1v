package com.taskflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload para criação de um novo Usuário.
 * Validado automaticamente pelo Spring com @Valid no Controller.
 */
public record UsuarioRequestDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "O role é obrigatório")
        @Pattern(regexp = "ADMIN|MEMBRO|GERENTE",
                 message = "Role inválido. Use: ADMIN, GERENTE ou MEMBRO")
        String role

) {}
