package com.taskflow.service;

import com.taskflow.dto.request.ProjetoRequestDTO;
import com.taskflow.dto.response.ProjetoResponseDTO;
import com.taskflow.exception.ConflitoException;
import com.taskflow.exception.RecursoNaoEncontradoException;
import com.taskflow.exception.RegraDeNegocioException;
import com.taskflow.model.Projeto;
import com.taskflow.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Serviço de negócio para a entidade Projeto.
 *
 * Regras implementadas:
 *  RN-P1: Nome de projeto deve ser único.
 *  RN-P2: Data de prazo deve ser posterior à data de início.
 *  RN-P3: Projeto CANCELADO ou CONCLUIDO não pode ter o status alterado para EM_ANDAMENTO.
 *  RN-P4: Projeto com tarefas ativas não pode ser excluído diretamente.
 *  RN-P5: Ao concluir um projeto, todas as tarefas devem estar DONE ou CANCELLED.
 */
@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;

    // ── Listar todos ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listarTodos() {
        return projetoRepository.findAll()
                .stream()
                .map(ProjetoResponseDTO::from)
                .toList();
    }

    // ── Listar por status ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listarPorStatus(String status) {
        return projetoRepository.findByStatus(status)
                .stream()
                .map(ProjetoResponseDTO::from)
                .toList();
    }

    // ── Buscar por ID ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProjetoResponseDTO buscarPorId(Long id) {
        return ProjetoResponseDTO.from(buscarEntidadePorId(id));
    }

    // ── Criar ─────────────────────────────────────────────────────────────────
    /**
     * RN-P1: Nome único.
     * RN-P2: dataPrazo > dataInicio.
     */
    @Transactional
    public ProjetoResponseDTO criar(ProjetoRequestDTO dto) {

        // RN-P1
        if (projetoRepository.existsByNome(dto.nome())) {
            throw new ConflitoException(
                    "Já existe um projeto com o nome: '" + dto.nome() + "'.");
        }

        // RN-P2
        if (dto.dataInicio() != null && dto.dataPrazo() != null
                && !dto.dataPrazo().isAfter(dto.dataInicio())) {
            throw new RegraDeNegocioException(
                    "O prazo final deve ser posterior à data de início do projeto.");
        }

        Projeto projeto = Projeto.builder()
                .nome(dto.nome().trim())
                .descricao(dto.descricao())
                .status(dto.status())
                .dataInicio(dto.dataInicio())
                .dataPrazo(dto.dataPrazo())
                .build();

        return ProjetoResponseDTO.from(projetoRepository.save(projeto));
    }

    // ── Atualizar ─────────────────────────────────────────────────────────────
    /**
     * RN-P2: dataPrazo > dataInicio.
     * RN-P3: Projeto finalizado não pode voltar para EM_ANDAMENTO.
     * RN-P5: Projeto só pode ser CONCLUIDO se todas as tarefas estiverem DONE/CANCELLED.
     */
    @Transactional
    public ProjetoResponseDTO atualizar(Long id, ProjetoRequestDTO dto) {
        Projeto projeto = buscarEntidadePorId(id);

        // RN-P3
        boolean estaFinalizado = projeto.getStatus().equals("CONCLUIDO")
                || projeto.getStatus().equals("CANCELADO");
        if (estaFinalizado && dto.status().equals("EM_ANDAMENTO")) {
            throw new RegraDeNegocioException(
                    "Um projeto com status '" + projeto.getStatus() +
                    "' não pode ser reaberto para EM_ANDAMENTO.");
        }

        // RN-P5: tentando concluir? verificar tarefas
        if (dto.status().equals("CONCLUIDO")) {
            long tarefasIncompletas = projeto.getTarefas().stream()
                    .filter(t -> !t.getStatus().name().equals("DONE")
                              && !t.getStatus().name().equals("CANCELLED"))
                    .count();
            if (tarefasIncompletas > 0) {
                throw new RegraDeNegocioException(
                        "Não é possível concluir o projeto: ainda há " +
                        tarefasIncompletas + " tarefa(s) em aberto.");
            }
        }

        // RN-P2
        var inicio = dto.dataInicio() != null ? dto.dataInicio() : projeto.getDataInicio();
        var prazo  = dto.dataPrazo()  != null ? dto.dataPrazo()  : projeto.getDataPrazo();
        if (inicio != null && prazo != null && !prazo.isAfter(inicio)) {
            throw new RegraDeNegocioException(
                    "O prazo final deve ser posterior à data de início do projeto.");
        }

        projeto.setNome(dto.nome().trim());
        projeto.setDescricao(dto.descricao());
        projeto.setStatus(dto.status());
        projeto.setDataInicio(dto.dataInicio());
        projeto.setDataPrazo(dto.dataPrazo());

        return ProjetoResponseDTO.from(projetoRepository.save(projeto));
    }

    // ── Excluir ───────────────────────────────────────────────────────────────
    /**
     * RN-P4: Projeto com tarefas ativas (TODO/IN_PROGRESS) não pode ser excluído.
     */
    @Transactional
    public void excluir(Long id) {
        Projeto projeto = buscarEntidadePorId(id);

        long tarefasAtivas = projeto.getTarefas().stream()
                .filter(t -> t.getStatus().name().equals("TODO")
                          || t.getStatus().name().equals("IN_PROGRESS"))
                .count();

        // RN-P4
        if (tarefasAtivas > 0) {
            throw new RegraDeNegocioException(
                    "Não é possível excluir o projeto '" + projeto.getNome() +
                    "' pois ele possui " + tarefasAtivas + " tarefa(s) ativa(s). " +
                    "Cancele ou conclua as tarefas antes.");
        }

        projetoRepository.delete(projeto);
    }

    // ── Relatório: progresso consolidado de todos os projetos ─────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> progressoDeTodasOsProjetos() {
        return projetoRepository.findProgressoDeTodasOsProjetos()
                .stream()
                .map(row -> Map.of(
                        "id",                  row[0],
                        "nome",                row[1],
                        "status",              row[2],
                        "dataPrazo",           row[3] != null ? row[3].toString() : null,
                        "totalTarefas",        row[4],
                        "tarefasConcluidas",   row[5],
                        "percentualConclusao", row[6]
                ))
                .toList();
    }

    // ── Relatório: projetos com tarefas atrasadas ─────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> projetosComTarefasAtrasadas() {
        return projetoRepository.findProjetosComTarefasAtrasadas()
                .stream()
                .map(row -> Map.of(
                        "id",              row[0],
                        "nome",            row[1],
                        "status",          row[2],
                        "tarefasAtrasadas", row[3]
                ))
                .toList();
    }

    // ── Relatório: distribuição por prioridade ────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> distribuicaoPorPrioridade(Long projetoId) {
        buscarEntidadePorId(projetoId); // valida existência
        return projetoRepository.findDistribuicaoPorPrioridadeNoProje(projetoId)
                .stream()
                .map(row -> Map.of(
                        "prioridade",  row[0],
                        "quantidade",  row[1],
                        "concluidas",  row[2]
                ))
                .toList();
    }

    // ── Helper interno ────────────────────────────────────────────────────────
    public Projeto buscarEntidadePorId(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Projeto não encontrado com ID: " + id));
    }
}
