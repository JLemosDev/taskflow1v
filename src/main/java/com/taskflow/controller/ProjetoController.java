package com.taskflow.controller;

import com.taskflow.dto.request.ProjetoRequestDTO;
import com.taskflow.dto.response.ProjetoResponseDTO;
import com.taskflow.service.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciamento de Projetos.
 */
@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos", description = "CRUD de projetos, progresso e alertas de risco")
public class ProjetoController {

    private final ProjetoService projetoService;

    // ── POST /api/projetos ────────────────────────────────────────────────────
    @Operation(
        summary     = "Criar projeto",
        description = "Cria um novo projeto. Nome deve ser único (RN-P1) e dataPrazo deve ser posterior a dataInicio (RN-P2)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Projeto criado com sucesso",
            content = @Content(schema = @Schema(implementation = ProjetoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada"),
        @ApiResponse(responseCode = "409", description = "Nome de projeto já cadastrado")
    })
    @PostMapping
    public ResponseEntity<ProjetoResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do novo projeto",
                content = @Content(examples = @ExampleObject(
                    value = """
                            {
                              "nome":        "Portal do Cliente",
                              "descricao":   "Desenvolvimento do portal de autoatendimento",
                              "status":      "PLANEJAMENTO",
                              "dataInicio":  "2025-08-01",
                              "dataPrazo":   "2025-12-31"
                            }""")))
            @Valid @RequestBody ProjetoRequestDTO dto) {

        ProjetoResponseDTO criado = projetoService.criar(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(location).body(criado);
    }

    // ── GET /api/projetos ─────────────────────────────────────────────────────
    @Operation(
        summary     = "Listar todos os projetos",
        description = "Retorna todos os projetos com totais de tarefas e quantidade de concluídas."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<ProjetoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(projetoService.listarTodos());
    }

    // ── GET /api/projetos?status=EM_ANDAMENTO ─────────────────────────────────
    @Operation(
        summary     = "Filtrar projetos por status",
        description = "Retorna projetos filtrados pelo status informado. " +
                      "Valores válidos: PLANEJAMENTO, EM_ANDAMENTO, CONCLUIDO, CANCELADO."
    )
    @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso")
    @GetMapping("/status")
    public ResponseEntity<List<ProjetoResponseDTO>> listarPorStatus(
            @Parameter(description = "Status do projeto", example = "EM_ANDAMENTO")
            @RequestParam String status) {

        return ResponseEntity.ok(projetoService.listarPorStatus(status));
    }

    // ── GET /api/projetos/{id} ────────────────────────────────────────────────
    @Operation(
        summary     = "Buscar projeto por ID",
        description = "Retorna os dados detalhados de um projeto específico."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado",
            content = @Content(examples = @ExampleObject(
                value = """{"status":404,"erro":"Projeto não encontrado com ID: 99"}""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> buscarPorId(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(projetoService.buscarPorId(id));
    }

    // ── PUT /api/projetos/{id} ────────────────────────────────────────────────
    @Operation(
        summary     = "Atualizar projeto",
        description = """
                Atualiza os dados de um projeto. Regras aplicadas:
                - **RN-P2**: dataPrazo deve ser posterior a dataInicio.
                - **RN-P3**: Projeto CONCLUIDO/CANCELADO não pode voltar para EM_ANDAMENTO.
                - **RN-P5**: Projeto só pode ser marcado como CONCLUIDO se todas as tarefas estiverem DONE ou CANCELLED.
                """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Projeto atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Regra de negócio violada"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> atualizar(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados atualizados do projeto",
                content = @Content(examples = @ExampleObject(
                    value = """
                            {
                              "nome":       "Portal do Cliente v2",
                              "descricao":  "Versão revisada com novos requisitos",
                              "status":     "EM_ANDAMENTO",
                              "dataInicio": "2025-08-01",
                              "dataPrazo":  "2026-03-31"
                            }""")))
            @Valid @RequestBody ProjetoRequestDTO dto) {

        return ResponseEntity.ok(projetoService.atualizar(id, dto));
    }

    // ── DELETE /api/projetos/{id} ─────────────────────────────────────────────
    @Operation(
        summary     = "Excluir projeto",
        description = "Remove o projeto. Bloqueado se houver tarefas com status TODO ou IN_PROGRESS (RN-P4)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Projeto excluído com sucesso"),
        @ApiResponse(responseCode = "400", description = "Projeto possui tarefas ativas",
            content = @Content(examples = @ExampleObject(
                value = """
                        {"status":400,"erro":"Não é possível excluir o projeto 'Portal do Cliente' pois ele possui 2 tarefa(s) ativa(s)."}"""))),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long id) {

        projetoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Relatórios
    // ══════════════════════════════════════════════════════════════════════════

    // ── GET /api/projetos/relatorios/progresso ────────────────────────────────
    @Operation(
        summary     = "Progresso consolidado de todos os projetos",
        description = "Retorna para cada projeto: total de tarefas, quantidade concluída e percentual de progresso. " +
                      "Ideal para dashboards executivos."
    )
    @ApiResponse(responseCode = "200", description = "Relatório gerado com sucesso")
    @GetMapping("/relatorios/progresso")
    public ResponseEntity<List<Map<String, Object>>> progressoConsolidado() {
        return ResponseEntity.ok(projetoService.progressoDeTodasOsProjetos());
    }

    // ── GET /api/projetos/relatorios/em-risco ─────────────────────────────────
    @Operation(
        summary     = "Projetos com tarefas atrasadas",
        description = "Lista projetos que possuem tarefas com data de entrega vencida e status ativo. " +
                      "Usado para alertas automáticos de risco."
    )
    @ApiResponse(responseCode = "200", description = "Lista de projetos em risco retornada")
    @GetMapping("/relatorios/em-risco")
    public ResponseEntity<List<Map<String, Object>>> emRisco() {
        return ResponseEntity.ok(projetoService.projetosComTarefasAtrasadas());
    }

    // ── GET /api/projetos/{id}/relatorios/prioridades ─────────────────────────
    @Operation(
        summary     = "Distribuição de tarefas por prioridade",
        description = "Retorna a quantidade de tarefas por prioridade (CRITICAL, HIGH, MEDIUM, LOW) " +
                      "dentro de um projeto, com quantas já foram concluídas em cada nível."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribuição calculada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    })
    @GetMapping("/{id}/relatorios/prioridades")
    public ResponseEntity<List<Map<String, Object>>> distribuicaoPorPrioridade(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(projetoService.distribuicaoPorPrioridade(id));
    }
}
