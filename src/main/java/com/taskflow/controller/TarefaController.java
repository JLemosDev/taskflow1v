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

@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
@Tag(name = "Tarefas", description = "Entidade principal - CRUD completo, reatribuicao em lote e relatorios analiticos")
public class TarefaController {

    private final TarefaService tarefaService;

    @Operation(summary = "Criar tarefa",
               description = "Cria uma nova tarefa. RN-T1: projeto nao pode ser CONCLUIDO/CANCELADO. RN-T2: data de entrega <= prazo do projeto. RN-T5: prioridade CRITICAL exige responsavel.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso",
            content = @Content(schema = @Schema(implementation = TarefaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados invalidos ou regra de negocio violada",
            content = @Content(examples = @ExampleObject(value = "{\"status\":400,\"erro\":\"Tarefas CRITICAL devem ter responsavel atribuido\"}"))),
        @ApiResponse(responseCode = "404", description = "Projeto ou responsavel nao encontrado")
    })
    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados da nova tarefa",
                content = @Content(examples = @ExampleObject(
                    value = "{\"titulo\":\"Implementar login\",\"descricao\":\"Autenticacao com JWT\",\"prioridade\":\"HIGH\",\"dataEntrega\":\"2025-09-15\",\"horasEstimadas\":8,\"projetoId\":1,\"responsavelId\":2}")))
            @Valid @RequestBody TarefaRequestDTO dto) {

        TarefaResponseDTO criada = tarefaService.criar(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @Operation(summary = "Listar todas as tarefas",
               description = "Retorna todas as tarefas com projetos e responsaveis. O campo 'atrasada' e calculado dinamicamente.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<TarefaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(tarefaService.listarTodas());
    }

    @Operation(summary = "Buscar tarefa por ID",
               description = "Retorna dados completos da tarefa: projeto, responsavel, total de comentarios e se esta atrasada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefa encontrada",
            content = @Content(schema = @Schema(implementation = TarefaResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada",
            content = @Content(examples = @ExampleObject(value = "{\"status\":404,\"erro\":\"Tarefa nao encontrada com ID: 99\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(
            @Parameter(description = "ID da tarefa", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(tarefaService.buscarPorId(id));
    }

    @Operation(summary = "Listar tarefas por projeto",
               description = "Retorna todas as tarefas vinculadas a um projeto especifico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefas do projeto retornadas"),
        @ApiResponse(responseCode = "404", description = "Projeto nao encontrado")
    })
    @GetMapping("/projeto/{projetoId}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorProjeto(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long projetoId) {
        return ResponseEntity.ok(tarefaService.listarPorProjeto(projetoId));
    }

    @Operation(summary = "Listar tarefas por responsavel",
               description = "Retorna todas as tarefas atribuidas a um usuario em todos os projetos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefas do responsavel retornadas"),
        @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    @GetMapping("/responsavel/{usuarioId}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorResponsavel(
            @Parameter(description = "ID do responsavel", example = "2")
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(tarefaService.listarPorResponsavel(usuarioId));
    }

    @Operation(summary = "Atualizar tarefa (parcial - PATCH)",
               description = "Apenas os campos informados serao alterados. RN-T3: DONE/CANCELLED nao volta para TODO. RN-T4: horas trabalhadas <= 2x estimadas. RN-T5: CRITICAL exige responsavel.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefa atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = TarefaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Regra de negocio violada",
            content = @Content(examples = @ExampleObject(value = "{\"status\":400,\"erro\":\"Horas trabalhadas nao podem exceder o dobro das estimadas\"}"))),
        @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(
            @Parameter(description = "ID da tarefa", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Campos a atualizar (todos opcionais). Exemplos: {\"status\":\"IN_PROGRESS\"} ou {\"status\":\"DONE\",\"horasTrabalhadas\":9}",
                content = @Content(examples = @ExampleObject(
                    value = "{\"status\":\"IN_PROGRESS\"}")))
            @Valid @RequestBody TarefaUpdateRequestDTO dto) {
        return ResponseEntity.ok(tarefaService.atualizar(id, dto));
    }

    @Operation(summary = "Excluir tarefa",
               description = "Remove a tarefa. Tarefas com status IN_PROGRESS nao podem ser excluidas diretamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tarefa excluida com sucesso"),
        @ApiResponse(responseCode = "400", description = "Tarefa em andamento nao pode ser excluida",
            content = @Content(examples = @ExampleObject(value = "{\"status\":400,\"erro\":\"Nao e possivel excluir tarefa em andamento. Cancele antes.\"}"))),
        @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID da tarefa", example = "1")
            @PathVariable Long id) {
        tarefaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reatribuicao de tarefas em lote",
               description = "Transfere todas as tarefas ativas de um usuario (origem) para outro (destino). RN-T7: origem e destino devem ser distintos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reatribuicao concluida",
            content = @Content(examples = @ExampleObject(value = "{\"mensagem\":\"Reatribuicao concluida.\",\"atualizadas\":4,\"origemId\":1,\"destinoId\":2}"))),
        @ApiResponse(responseCode = "400", description = "Origem igual ao destino (RN-T7)"),
        @ApiResponse(responseCode = "404", description = "Usuario de origem ou destino nao encontrado")
    })
    @PatchMapping("/reatribuir")
    public ResponseEntity<Map<String, Object>> reatribuirEmLote(
            @Parameter(description = "ID do usuario de origem", example = "1")
            @RequestParam Long origemId,
            @Parameter(description = "ID do usuario de destino", example = "2")
            @RequestParam Long destinoId) {
        return ResponseEntity.ok(tarefaService.reatribuirEmLote(origemId, destinoId));
    }

    @Operation(summary = "Tarefas atrasadas por responsavel",
               description = "Lista tarefas com prazo vencido e status ativo de um usuario, com quantos dias de atraso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    @GetMapping("/relatorios/atrasadas/{responsavelId}")
    public ResponseEntity<List<Map<String, Object>>> atrasadasPorResponsavel(
            @Parameter(description = "ID do responsavel", example = "2")
            @PathVariable Long responsavelId) {
        return ResponseEntity.ok(tarefaService.tarefasAtrasadasPorResponsavel(responsavelId));
    }

    @Operation(summary = "Tarefas criticas sem responsavel",
               description = "Tarefas CRITICAL ou HIGH sem responsavel em projetos ativos. Permite acao imediata da gestao.")
    @ApiResponse(responseCode = "200", description = "Lista de tarefas criticas retornada")
    @GetMapping("/relatorios/criticas-sem-responsavel")
    public ResponseEntity<List<Map<String, Object>>> criticasSemResponsavel() {
        return ResponseEntity.ok(tarefaService.tarefasCriticasSemResponsavel());
    }

    @Operation(summary = "Tarefas com desvio de estimativa",
               description = "Tarefas concluidas onde a diferenca entre horas estimadas e trabalhadas ultrapassou o threshold%. Util para retrospectivas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatorio gerado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Threshold fora do intervalo 0-100")
    })
    @GetMapping("/relatorios/desvio-estimativa")
    public ResponseEntity<List<Map<String, Object>>> desvioEstimativa(
            @Parameter(description = "Desvio percentual minimo (0-100)", example = "20")
            @RequestParam(defaultValue = "20") double threshold) {
        return ResponseEntity.ok(tarefaService.tarefasComDesvioDeEstimativa(threshold));
    }
}
