package com.taskflow.service;

import com.taskflow.dto.request.TarefaRequestDTO;
import com.taskflow.dto.request.TarefaUpdateRequestDTO;
import com.taskflow.dto.response.TarefaResponseDTO;
import com.taskflow.exception.RegraDeNegocioException;
import com.taskflow.exception.RecursoNaoEncontradoException;
import com.taskflow.model.*;
import com.taskflow.repository.TarefaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Serviço de negócio para a entidade Tarefa.
 *
 * Regras implementadas:
 *  RN-T1: Não é permitido criar tarefas em projetos CONCLUIDOS ou CANCELADOS.
 *  RN-T2: Data de entrega da tarefa não pode ser posterior ao prazo do projeto.
 *  RN-T3: Tarefa DONE ou CANCELLED não pode ser reaberta para TODO.
 *  RN-T4: Horas trabalhadas não podem ser maiores que o dobro das horas estimadas.
 *  RN-T5: Tarefa CRITICAL deve obrigatoriamente ter um responsável atribuído.
 *  RN-T6: Responsável atribuído deve existir e estar ativo no sistema.
 *  RN-T7: Reatribuição em lote requer que origem e destino sejam usuários distintos.
 */
@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final ProjetoService   projetoService;
    private final UsuarioService   usuarioService;

    // ── Listar todas ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TarefaResponseDTO> listarTodas() {
        return tarefaRepository.findAll()
                .stream()
                .map(TarefaResponseDTO::from)
                .toList();
    }

    // ── Listar por projeto ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TarefaResponseDTO> listarPorProjeto(Long projetoId) {
        projetoService.buscarEntidadePorId(projetoId); // valida existência
        return tarefaRepository.findByProjetoId(projetoId)
                .stream()
                .map(TarefaResponseDTO::from)
                .toList();
    }

    // ── Listar por responsável ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TarefaResponseDTO> listarPorResponsavel(Long usuarioId) {
        usuarioService.buscarEntidadePorId(usuarioId); // valida existência
        return tarefaRepository.findByResponsavelId(usuarioId)
                .stream()
                .map(TarefaResponseDTO::from)
                .toList();
    }

    // ── Buscar por ID ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TarefaResponseDTO buscarPorId(Long id) {
        return TarefaResponseDTO.from(buscarEntidadePorId(id));
    }

    // ── Criar ─────────────────────────────────────────────────────────────────
    /**
     * RN-T1: Projeto deve estar ativo (não CONCLUIDO/CANCELADO).
     * RN-T2: Data de entrega <= prazo do projeto.
     * RN-T5: Se prioridade CRITICAL, responsável é obrigatório.
     * RN-T6: Responsável deve existir.
     */
    @Transactional
    public TarefaResponseDTO criar(TarefaRequestDTO dto) {
        Projeto projeto = projetoService.buscarEntidadePorId(dto.projetoId());

        // RN-T1
        if (projeto.getStatus().equals("CONCLUIDO") || projeto.getStatus().equals("CANCELADO")) {
            throw new RegraDeNegocioException(
                    "Não é possível adicionar tarefas a um projeto com status '"
                    + projeto.getStatus() + "'.");
        }

        Priority prioridade = dto.prioridade() != null ? dto.prioridade() : Priority.MEDIUM;

        // RN-T5
        if (prioridade == Priority.CRITICAL && dto.responsavelId() == null) {
            throw new RegraDeNegocioException(
                    "Tarefas com prioridade CRITICAL devem ter um responsável atribuído.");
        }

        // RN-T2
        if (dto.dataEntrega() != null && projeto.getDataPrazo() != null
                && dto.dataEntrega().isAfter(projeto.getDataPrazo())) {
            throw new RegraDeNegocioException(
                    "A data de entrega da tarefa (" + dto.dataEntrega() +
                    ") não pode ser posterior ao prazo do projeto (" +
                    projeto.getDataPrazo() + ").");
        }

        // RN-T6
        Usuario responsavel = null;
        if (dto.responsavelId() != null) {
            responsavel = usuarioService.buscarEntidadePorId(dto.responsavelId());
        }

        Tarefa tarefa = Tarefa.builder()
                .titulo(dto.titulo().trim())
                .descricao(dto.descricao())
                .status(dto.status() != null ? dto.status() : TaskStatus.TODO)
                .prioridade(prioridade)
                .dataEntrega(dto.dataEntrega())
                .horasEstimadas(dto.horasEstimadas())
                .projeto(projeto)
                .responsavel(responsavel)
                .build();

        return TarefaResponseDTO.from(tarefaRepository.save(tarefa));
    }

    // ── Atualizar parcialmente (PATCH) ────────────────────────────────────────
    /**
     * RN-T3: Tarefa DONE/CANCELLED não pode voltar para TODO.
     * RN-T4: horasTrabalhadas <= 2 * horasEstimadas.
     * RN-T5: Se prioridade mudar para CRITICAL sem responsável, bloqueia.
     */
    @Transactional
    public TarefaResponseDTO atualizar(Long id, TarefaUpdateRequestDTO dto) {
        Tarefa tarefa = buscarEntidadePorId(id);

        // RN-T3: bloqueia reabertura indevida
        boolean estaFinalizada = tarefa.getStatus() == TaskStatus.DONE
                || tarefa.getStatus() == TaskStatus.CANCELLED;
        if (estaFinalizada && dto.status() == TaskStatus.TODO) {
            throw new RegraDeNegocioException(
                    "Uma tarefa com status '" + tarefa.getStatus() +
                    "' não pode ser revertida para TODO.");
        }

        // Aplica campos opcionais informados
        if (dto.titulo()  != null) tarefa.setTitulo(dto.titulo().trim());
        if (dto.descricao() != null) tarefa.setDescricao(dto.descricao());
        if (dto.status()  != null) tarefa.setStatus(dto.status());
        if (dto.dataEntrega() != null) tarefa.setDataEntrega(dto.dataEntrega());
        if (dto.horasEstimadas() != null) tarefa.setHorasEstimadas(dto.horasEstimadas());

        // RN-T4: validar horas trabalhadas
        if (dto.horasTrabalhadas() != null) {
            int estimadas = tarefa.getHorasEstimadas() != null ? tarefa.getHorasEstimadas() : Integer.MAX_VALUE;
            if (dto.horasTrabalhadas() > estimadas * 2) {
                throw new RegraDeNegocioException(
                        "Horas trabalhadas (" + dto.horasTrabalhadas() +
                        ") não podem exceder o dobro das horas estimadas (" +
                        estimadas + "). Revise a estimativa primeiro.");
            }
            tarefa.setHorasTrabalhadas(dto.horasTrabalhadas());
        }

        // Atualiza prioridade e revalida RN-T5
        Priority novaPrioridade = dto.prioridade() != null ? dto.prioridade() : tarefa.getPrioridade();
        boolean semResponsavel  = tarefa.getResponsavel() == null && dto.responsavelId() == null;
        if (novaPrioridade == Priority.CRITICAL && semResponsavel) {
            throw new RegraDeNegocioException(
                    "Tarefas com prioridade CRITICAL devem ter um responsável atribuído.");
        }
        tarefa.setPrioridade(novaPrioridade);

        // RN-T6: atualiza responsável se informado
        if (dto.responsavelId() != null) {
            tarefa.setResponsavel(usuarioService.buscarEntidadePorId(dto.responsavelId()));
        }

        return TarefaResponseDTO.from(tarefaRepository.save(tarefa));
    }

    // ── Excluir ───────────────────────────────────────────────────────────────
    /** Apenas tarefas CANCELLED ou TODO podem ser excluídas diretamente. */
    @Transactional
    public void excluir(Long id) {
        Tarefa tarefa = buscarEntidadePorId(id);

        if (tarefa.getStatus() == TaskStatus.IN_PROGRESS) {
            throw new RegraDeNegocioException(
                    "Não é possível excluir uma tarefa em andamento. " +
                    "Cancele-a antes de excluir.");
        }

        tarefaRepository.delete(tarefa);
    }

    // ── Reatribuição em lote ──────────────────────────────────────────────────
    /**
     * RN-T7: Origem e destino devem ser usuários distintos.
     * Delega a lógica de UPDATE ao repositório (query nativa em lote).
     */
    @Transactional
    public Map<String, Object> reatribuirEmLote(Long origemId, Long destinoId) {
        // RN-T7
        if (origemId.equals(destinoId)) {
            throw new RegraDeNegocioException(
                    "O usuário de origem e destino devem ser diferentes.");
        }

        usuarioService.buscarEntidadePorId(origemId);  // valida origem
        usuarioService.buscarEntidadePorId(destinoId); // valida destino

        int atualizadas = tarefaRepository.reatribuirTarefasEmLote(origemId, destinoId);

        return Map.of(
                "mensagem",    "Reatribuição concluída.",
                "atualizadas", atualizadas,
                "origemId",    origemId,
                "destinoId",   destinoId
        );
    }

    // ── Relatório: tarefas atrasadas por responsável ──────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> tarefasAtrasadasPorResponsavel(Long responsavelId) {
        usuarioService.buscarEntidadePorId(responsavelId);
        return tarefaRepository.findTarefasAtrasadasByResponsavel(responsavelId)
                .stream()
                .map(row -> Map.of(
                        "id",          row[0],
                        "titulo",      row[1],
                        "prioridade",  row[2],
                        "dataEntrega", row[3] != null ? row[3].toString() : null,
                        "nomeProjeto", row[4],
                        "diasAtraso",  row[5]
                ))
                .toList();
    }

    // ── Relatório: tarefas críticas sem responsável ───────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> tarefasCriticasSemResponsavel() {
        return tarefaRepository.findTarefasCriticasSemResponsavel()
                .stream()
                .map(row -> Map.of(
                        "id",          row[0],
                        "titulo",      row[1],
                        "prioridade",  row[2],
                        "dataEntrega", row[3] != null ? row[3].toString() : null,
                        "nomeProjeto", row[4],
                        "projetoId",   row[5]
                ))
                .toList();
    }

    // ── Relatório: desvio de estimativa ──────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> tarefasComDesvioDeEstimativa(double thresholdPct) {
        if (thresholdPct < 0 || thresholdPct > 100) {
            throw new RegraDeNegocioException(
                    "O threshold deve ser um percentual entre 0 e 100.");
        }
        return tarefaRepository.findTarefasComDesvioDeEstimativa(thresholdPct)
                .stream()
                .map(row -> Map.of(
                        "id",               row[0],
                        "titulo",           row[1],
                        "nomeProjeto",      row[2],
                        "horasEstimadas",   row[3],
                        "horasTrabalhadas", row[4],
                        "desvioHoras",      row[5],
                        "desvioPercentual", row[6]
                ))
                .toList();
    }

    // ── Helper interno ────────────────────────────────────────────────────────
    public Tarefa buscarEntidadePorId(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Tarefa não encontrada com ID: " + id));
    }
}
