package com.taskflow.service;

import com.taskflow.dto.request.ComentarioRequestDTO;
import com.taskflow.dto.response.ComentarioResponseDTO;
import com.taskflow.exception.RegraDeNegocioException;
import com.taskflow.exception.RecursoNaoEncontradoException;
import com.taskflow.model.Comentario;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ComentarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Serviço de negócio para a entidade Comentario.
 *
 * Regras implementadas:
 *  RN-C1: Não é permitido comentar em tarefas CANCELLED.
 *  RN-C2: O conteúdo não pode ser vazio após trim (reforço além do @NotBlank do DTO).
 *  RN-C3: Apenas o autor pode excluir seu próprio comentário.
 *  RN-C4: Comentários não podem ser editados (imutabilidade de registro).
 */
@Service
@RequiredArgsConstructor
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final TarefaService        tarefaService;
    private final UsuarioService       usuarioService;

    // ── Listar por tarefa ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ComentarioResponseDTO> listarPorTarefa(Long tarefaId) {
        tarefaService.buscarEntidadePorId(tarefaId); // valida existência
        return comentarioRepository.findByTarefaIdOrderByCriadoEmDesc(tarefaId)
                .stream()
                .map(ComentarioResponseDTO::from)
                .toList();
    }

    // ── Criar ─────────────────────────────────────────────────────────────────
    /**
     * RN-C1: Tarefa cancelada não aceita comentários.
     * RN-C2: Conteúdo não pode ser vazio após trim.
     */
    @Transactional
    public ComentarioResponseDTO criar(ComentarioRequestDTO dto) {
        var tarefa = tarefaService.buscarEntidadePorId(dto.tarefaId());
        var autor  = usuarioService.buscarEntidadePorId(dto.autorId());

        // RN-C1
        if (tarefa.getStatus() == TaskStatus.CANCELLED) {
            throw new RegraDeNegocioException(
                    "Não é permitido adicionar comentários a tarefas canceladas.");
        }

        // RN-C2
        if (dto.conteudo().trim().isBlank()) {
            throw new RegraDeNegocioException(
                    "O conteúdo do comentário não pode ser vazio.");
        }

        Comentario comentario = Comentario.builder()
                .conteudo(dto.conteudo().trim())
                .tarefa(tarefa)
                .autor(autor)
                .build();

        return ComentarioResponseDTO.from(comentarioRepository.save(comentario));
    }

    // ── Excluir ───────────────────────────────────────────────────────────────
    /**
     * RN-C3: Apenas o autor pode excluir o próprio comentário.
     */
    @Transactional
    public void excluir(Long comentarioId, Long solicitanteId) {
        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Comentário não encontrado com ID: " + comentarioId));

        // RN-C3
        if (!comentario.getAutor().getId().equals(solicitanteId)) {
            throw new RegraDeNegocioException(
                    "Apenas o autor do comentário pode excluí-lo.");
        }

        comentarioRepository.delete(comentario);
    }

    // ── Relatório: engajamento por projeto ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> engajamentoPorProjeto(Long projetoId) {
        return comentarioRepository.findEngajamentoComentariosPorProjeto(projetoId)
                .stream()
                .map(row -> Map.of(
                        "autor",            row[0],
                        "projeto",          row[1],
                        "totalComentarios", row[2],
                        "ultimoComentario", row[3] != null ? row[3].toString() : null
                ))
                .toList();
    }
}
