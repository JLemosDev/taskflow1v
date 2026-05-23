package com.taskflow.dto.response;

import com.taskflow.model.Comentario;

import java.time.LocalDateTime;

/**
 * Dados do Comentário retornados pela API.
 */
public record ComentarioResponseDTO(
        Long id,
        String conteudo,
        Long tarefaId,
        String tarefaTitulo,
        Long autorId,
        String autorNome,
        LocalDateTime criadoEm
) {
    public static ComentarioResponseDTO from(Comentario c) {
        return new ComentarioResponseDTO(
                c.getId(),
                c.getConteudo(),
                c.getTarefa().getId(),
                c.getTarefa().getTitulo(),
                c.getAutor().getId(),
                c.getAutor().getNome(),
                c.getCriadoEm()
        );
    }
}
