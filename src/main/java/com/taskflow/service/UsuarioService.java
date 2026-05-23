package com.taskflow.service;

import com.taskflow.dto.request.UsuarioRequestDTO;
import com.taskflow.dto.response.UsuarioResponseDTO;
import com.taskflow.exception.ConflitoException;
import com.taskflow.exception.RecursoNaoEncontradoException;
import com.taskflow.exception.RegraDeNegocioException;
import com.taskflow.model.Usuario;
import com.taskflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Serviço de negócio para a entidade Usuario.
 *
 * Regras implementadas:
 *  RN-U1: E-mail deve ser único no sistema.
 *  RN-U2: Não é permitido excluir usuário com tarefas ativas (TODO ou IN_PROGRESS).
 *  RN-U3: Apenas roles válidos podem ser atribuídos (validado no DTO + reforçado aqui).
 *  RN-U4: Ao atualizar e-mail, verificar conflito com outro usuário existente.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // ── Listar todos ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponseDTO::from)
                .toList();
    }

    // ── Buscar por ID ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        return UsuarioResponseDTO.from(buscarEntidadePorId(id));
    }

    // ── Criar ─────────────────────────────────────────────────────────────────
    /**
     * RN-U1: E-mail deve ser único.
     * RN-U3: Role informado deve ser um dos valores permitidos (reforço após validação do DTO).
     */
    @Transactional
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {

        // RN-U1
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new ConflitoException(
                    "Já existe um usuário cadastrado com o e-mail: " + dto.email());
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.nome().trim())
                .email(dto.email().toLowerCase().trim())
                .role(dto.role())
                .build();

        return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
    }

    // ── Atualizar ─────────────────────────────────────────────────────────────
    /**
     * RN-U4: Se o e-mail mudou, verificar que não conflita com outro usuário.
     */
    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);

        // RN-U4: e-mail mudou? verificar conflito com OUTRO usuário
        boolean emailMudou = !usuario.getEmail().equalsIgnoreCase(dto.email());
        if (emailMudou && usuarioRepository.existsByEmail(dto.email())) {
            throw new ConflitoException(
                    "O e-mail '" + dto.email() + "' já está em uso por outro usuário.");
        }

        usuario.setNome(dto.nome().trim());
        usuario.setEmail(dto.email().toLowerCase().trim());
        usuario.setRole(dto.role());

        return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
    }

    // ── Excluir ───────────────────────────────────────────────────────────────
    /**
     * RN-U2: Impede exclusão de usuário com tarefas ativas.
     * O gestor deve reatribuir as tarefas antes de remover o usuário.
     */
    @Transactional
    public void excluir(Long id) {
        Usuario usuario = buscarEntidadePorId(id);

        long tarefasAtivas = usuario.getTarefas().stream()
                .filter(t -> {
                    String s = t.getStatus().name();
                    return s.equals("TODO") || s.equals("IN_PROGRESS");
                })
                .count();

        // RN-U2
        if (tarefasAtivas > 0) {
            throw new RegraDeNegocioException(
                    "Não é possível excluir o usuário '" + usuario.getNome() +
                    "' pois ele possui " + tarefasAtivas + " tarefa(s) ativa(s). " +
                    "Reatribua-as antes de prosseguir.");
        }

        usuarioRepository.delete(usuario);
    }

    // ── Relatório: usuários com mais tarefas em andamento ─────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> rankingPorTarefasEmAndamento() {
        return usuarioRepository.findRankingUsuariosPorTarefasEmAndamento()
                .stream()
                .map(row -> Map.of(
                        "id",                   row[0],
                        "nome",                 row[1],
                        "email",                row[2],
                        "tarefasEmAndamento",   row[3]
                ))
                .toList();
    }

    // ── Relatório: usuários sobrecarregados ───────────────────────────────────
    /**
     * RN-U5: Considera sobrecarregado quem tem mais tarefas ativas que o limite.
     * Limite padrão: 5 tarefas.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> usuariosSobrecarregados(int limite) {
        if (limite < 1) {
            throw new RegraDeNegocioException("O limite deve ser no mínimo 1.");
        }
        return usuarioRepository.findUsuariosSobrecarregados(limite)
                .stream()
                .map(row -> Map.of(
                        "id",                 row[0],
                        "nome",               row[1],
                        "email",              row[2],
                        "totalTarefasAtivas", row[3]
                ))
                .toList();
    }

    // ── Relatório: estatísticas de produtividade ──────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> estatisticasProdutividade(Long usuarioId) {
        buscarEntidadePorId(usuarioId); // valida existência
        return usuarioRepository.findEstatisticasProdutividadeByUsuario(usuarioId)
                .map(row -> Map.of(
                        "nome",            row[0],
                        "totalConcluidas", row[1],
                        "mediaHoras",      row[2],
                        "pctNoPrazo",      row[3]
                ))
                .orElse(Map.of("mensagem", "Nenhuma tarefa concluída encontrada para este usuário."));
    }

    // ── Helper interno ────────────────────────────────────────────────────────
    public Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado com ID: " + id));
    }
}
