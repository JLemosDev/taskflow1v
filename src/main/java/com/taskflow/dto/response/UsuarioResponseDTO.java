package com.taskflow.dto.response;

import com.taskflow.model.Usuario;

import java.time.LocalDateTime;

/**
 * Dados do Usuário retornados pela API.
 * Nunca expõe a entidade JPA diretamente.
 */
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String role,
        int totalTarefas,
        LocalDateTime criadoEm
) {
    /** Método de fábrica: converte a entidade JPA para o DTO de resposta. */
    public static UsuarioResponseDTO from(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getRole(),
                u.getTarefas().size(),
                u.getCriadoEm()
        );
    }
}
