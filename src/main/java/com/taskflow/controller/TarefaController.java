package com.taskflow.controller;

import com.taskflow.dto.request.TarefaRequestDTO;
import com.taskflow.dto.request.TarefaUpdateRequestDTO;
import com.taskflow.dto.response.TarefaResponseDTO;
import com.taskflow.service.TarefaService;
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
 * Controller REST para gerenciamento de Tarefas.
 * Entidade principal do sistema — CRUD completo obrigatório.
 */
@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
@Tag(name = "Tarefas", description = "Entidade principal — CRUD completo, reatribuição em lote e relatórios analíticos")
public class TarefaController {

    private final TarefaService tarefaService;

    // ── POST /api/tarefas ─────────────────────────────────────────────────────
    @Operation(
        summary     = "Criar tarefa",
        description = """
                Cria uma nova tarefa vinculada a um projeto. Regras aplicadas:
                - **RN-T1**: Projeto não pode estar CONCLUIDO ou CANCELADO.
                - **RN-T2**: Data de entrega não pode ultrapassar o prazo do projeto.
                - **RN-T5**: Tarefas CRITICAL exigem responsável obrigatório.
                - **RN-T6**: responsavelId, se informado, deve existir no sistema.
                """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso",
            content = @Content(schema = @Schema(implementation = TarefaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada",
            content = @Content(examples = @ExampleObject(
                value = """
                        {"status":400,"erro":"Tarefas com prioridade CRITICAL devem ter um responsável atribuído."}"""))),
        @ApiResponse(responseCode = "404", description = "Projeto ou usuário responsável não encontrado")
    })
    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados da nova tarefa",
                content = @Content(examples = {
                    @ExampleObject(name = "Tarefa simples",
                        value = """
                                {
                                  "titulo":         "Implementar tela de login",
                                  "descricao":      "Criar o formulário de autenticação com JWT",
                                  "prioridade":     "HIGH",
                                  "dataEntrega":    "2025-09-15",
                                  "horasEstimadas": 8,
                                  "projetoId":      1,
                                  "responsavelId":  2
                                }"""),
                    @ExampleObject(name = "Tarefa sem responsável",
                        value = """
                                {
                                  "titulo":      "Definir paleta de cores",
                                  "prioridade":  "LOW",
                                  "projetoId":   1
                                }""")
                }))
            @Valid @RequestBody TarefaRequestDTO dto) {

        TarefaResponseDTO criada = tarefaService.criar(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(location).body(criada);
    }

    // ── GET /api/tarefas ──────────────────────────────────────────────────────
    @Operation(
        summary     = "Listar todas as tarefas",
        description = "Retorna todas as tarefas cadastradas no sistema com seus respectivos projetos e responsáveis. " +
                      "O campo `atrasada` é calculado dinamicamente."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<TarefaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(tarefaService.listarTodas());
    }

    // ── GET /api/tarefas/{id} ─────────────────────────────────────────────────
    @Operation(
        summary     = "Buscar tarefa por ID",
        description = "Retorna os dados completos de uma tarefa específica, incluindo projeto, " +
                      "responsável, total de comentários e se está atrasada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefa encontrada",
            content = @Content(schema = @Schema(implementation = TarefaResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
            content = @Content(examples = @ExampleObject(
                value = """{"status":404,"erro":"Tarefa não encontrada com ID: 99"}""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(
            @Parameter(description = "ID da tarefa", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(tarefaService.buscarPorId(id));
    }

    // ── GET /api/tarefas/projeto/{projetoId} ──────────────────────────────────
    @Operation(
        summary     = "Listar tarefas por projeto",
        description = "Retorna todas as tarefas vinculadas a um projeto específico."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefas do projeto retornadas"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    })
    @GetMapping("/projeto/{projetoId}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorProjeto(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long projetoId) {

        return ResponseEntity.ok(tarefaService.listarPorProjeto(projetoId));
    }

    // ── GET /api/tarefas/responsavel/{usuarioId} ──────────────────────────────
    @Operation(
        summary     = "Listar tarefas por responsável",
        description = "Retorna todas as tarefas atribuídas a um usuário específico, em todos os projetos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefas do responsável retornadas"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/responsavel/{usuarioId}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorResponsavel(
            @Parameter(description = "ID do usuário responsável", example = "2")
            @PathVariable Long usuarioId) {

        return ResponseEntity.ok(tarefaService.listarPorResponsavel(usuarioId));
    }

    // ── PATCH /api/tarefas/{id} ───────────────────────────────────────────────
    @Operation(
        summary     = "Atualizar tarefa (parcial)",
        description = """
                Atualização parcial (PATCH) — apenas os campos informados serão alterados. Regras aplicadas:
                - **RN-T3**: Status DONE/CANCELLED não pode voltar para TODO.
                - **RN-T4**: Horas trabalhadas ≤ 2× horas estimadas.
                - **RN-T5**: Prioridade CRITICAL exige responsável.
                """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefa atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = TarefaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Regra de negócio violada",
            content = @Content(examples = {
                @ExampleObject(name = "Horas excedidas",
                    value = """
                            {"status":400,"erro":"Horas trabalhadas (30) não podem exceder o dobro das horas estimadas (10)."}"""),
                @ExampleObject(name = "Reabertura indevida",
                    value = """
                            {"status":400,"erro":"Uma tarefa com status 'DONE' não pode ser revertida para TODO."}""")
            })),
        @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(
            @Parameter(description = "ID da tarefa", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Campos a atualizar (todos opcionais)",
                content = @Content(examples = {
                    @ExampleObject(name = "Avançar status",
                        value = """
                                {
                                  "status": "IN_PROGRESS"
                                }"""),
                    @ExampleObject(name = "Registrar horas e concluir",
                        value = """
                                {
                                  "status":           "DONE",
                                  "horasTrabalhadas": 9
                                }"""),
                    @ExampleObject(name = "Atribuir responsável",
                        value = """
                                {
                                  "responsavelId": 3
                                }""")
                }))
            @Valid @RequestBody TarefaUpdateRequestDTO dto) {

        return ResponseEntity.ok(tarefaService.atualizar(id, dto));
    }

    // ── DELETE /api/tarefas/{id} ──────────────────────────────────────────────
    @Operation(
        summary     = "Excluir tarefa",
        description = "Remove uma tarefa. Tarefas com status IN_PROGRESS não podem ser excluídas diretamente — cancele antes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tarefa excluída com sucesso"),
        @ApiResponse(responseCode = "400", description = "Tarefa em andamento não pode ser excluída",
            content = @Content(examples = @ExampleObject(
                value = """
                        {"status":400,"erro":"Não é possível excluir uma tarefa em andamento. Cancele-a antes de excluir."}"""))),
        @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID da tarefa", example = "1")
            @PathVariable Long id) {

        tarefaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Operações especiais
    // ══════════════════════════════════════════════════════════════════════════

    // ── PATCH /api/tarefas/reatribuir?origemId=1&destinoId=2 ─────────────────
    @Operation(
        summary     = "Reatribuição de tarefas em lote",
        description = "Transfere todas as tarefas ativas de um usuário (origem) para outro (destino) " +
                      "via UPDATE em lote no banco. Útil quando um colaborador é desligado ou realocado. " +
                      "Origem e destino devem ser distintos (RN-T7)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reatribuição concluída",
            content = @Content(examples = @ExampleObject(
                value = """
                        {
                          "mensagem":    "Reatribuição concluída.",
                          "atualizadas": 4,
                          "origemId":    1,
                          "destinoId":   2
                        }"""))),
        @ApiResponse(responseCode = "400", description = "Origem igual ao destino (RN-T7)"),
        @ApiResponse(responseCode = "404", description = "Usuário de origem ou destino não encontrado")
    })
    @PatchMapping("/reatribuir")
    public ResponseEntity<Map<String, Object>> reatribuirEmLote(
            @Parameter(description = "ID do usuário de origem", example = "1")
            @RequestParam Long origemId,
            @Parameter(description = "ID do usuário de destino", example = "2")
            @RequestParam Long destinoId) {

        return ResponseEntity.ok(tarefaService.reatribuirEmLote(origemId, destinoId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Relatórios
    // ══════════════════════════════════════════════════════════════════════════

    // ── GET /api/tarefas/relatorios/atrasadas/{responsavelId} ─────────────────
    @Operation(
        summary     = "Tarefas atrasadas por responsável",
        description = "Lista tarefas com prazo vencido e status ativo de um usuário específico, " +
                      "incluindo quantos dias de atraso cada uma acumula."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/relatorios/atrasadas/{responsavelId}")
    public ResponseEntity<List<Map<String, Object>>> atrasadasPorResponsavel(
            @Parameter(description = "ID do responsável", example = "2")
            @PathVariable Long responsavelId) {

        return ResponseEntity.ok(tarefaService.tarefasAtrasadasPorResponsavel(responsavelId));
    }

    // ── GET /api/tarefas/relatorios/criticas-sem-responsavel ──────────────────
    @Operation(
        summary     = "Tarefas críticas sem responsável",
        description = "Retorna tarefas com prioridade CRITICAL ou HIGH, ainda sem responsável atribuído, " +
                      "em projetos ativos. Permite ação imediata da gestão."
    )
    @ApiResponse(responseCode = "200", description = "Lista de tarefas críticas retornada")
    @GetMapping("/relatorios/criticas-sem-responsavel")
    public ResponseEntity<List<Map<String, Object>>> criticasSemResponsavel() {
        return ResponseEntity.ok(tarefaService.tarefasCriticasSemResponsavel());
    }

    // ── GET /api/tarefas/relatorios/desvio-estimativa?threshold=20 ────────────
    @Operation(
        summary     = "Tarefas com desvio de estimativa",
        description = "Retorna tarefas concluídas onde a diferença entre horas estimadas e trabalhadas " +
                      "ultrapassou o threshold percentual informado. Útil para retrospectivas de planejamento."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatório gerado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Threshold fora do intervalo 0-100")
    })
    @GetMapping("/relatorios/desvio-estimativa")
    public ResponseEntity<List<Map<String, Object>>> desvioEstimativa(
            @Parameter(description = "Desvio percentual mínimo para incluir no relatório (0-100)", example = "20")
            @RequestParam(defaultValue = "20") double threshold) {

        return ResponseEntity.ok(tarefaService.tarefasComDesvioDeEstimativa(threshold));
    }
}
