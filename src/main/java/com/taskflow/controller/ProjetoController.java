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

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos", description = "CRUD de projetos, progresso e alertas de risco")
public class ProjetoController {

    private final ProjetoService projetoService;

    @Operation(summary = "Criar projeto",
               description = "Cria um novo projeto. Nome deve ser unico (RN-P1) e dataPrazo posterior a dataInicio (RN-P2).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Projeto criado com sucesso",
            content = @Content(schema = @Schema(implementation = ProjetoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados invalidos ou regra de negocio violada"),
        @ApiResponse(responseCode = "409", description = "Nome de projeto ja cadastrado")
    })
    @PostMapping
    public ResponseEntity<ProjetoResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do novo projeto",
                content = @Content(examples = @ExampleObject(
                    value = "{\"nome\":\"Portal do Cliente\",\"descricao\":\"Portal de autoatendimento\",\"status\":\"PLANEJAMENTO\",\"dataInicio\":\"2025-08-01\",\"dataPrazo\":\"2025-12-31\"}")))
            @Valid @RequestBody ProjetoRequestDTO dto) {

        ProjetoResponseDTO criado = projetoService.criar(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(summary = "Listar todos os projetos",
               description = "Retorna todos os projetos com totais de tarefas e quantidade de concluidas.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<ProjetoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(projetoService.listarTodos());
    }

    @Operation(summary = "Filtrar projetos por status",
               description = "Valores validos: PLANEJAMENTO, EM_ANDAMENTO, CONCLUIDO, CANCELADO.")
    @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso")
    @GetMapping("/status")
    public ResponseEntity<List<ProjetoResponseDTO>> listarPorStatus(
            @Parameter(description = "Status do projeto", example = "EM_ANDAMENTO")
            @RequestParam String status) {
        return ResponseEntity.ok(projetoService.listarPorStatus(status));
    }

    @Operation(summary = "Buscar projeto por ID", description = "Retorna os dados detalhados de um projeto especifico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
        @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
            content = @Content(examples = @ExampleObject(value = "{\"status\":404,\"erro\":\"Projeto nao encontrado com ID: 99\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> buscarPorId(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(projetoService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar projeto",
               description = "Atualiza dados do projeto. Aplica RN-P2 (prazo > inicio), RN-P3 (nao reabre finalizado) e RN-P5 (so conclui se todas as tarefas estiverem DONE/CANCELLED).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Projeto atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Regra de negocio violada"),
        @ApiResponse(responseCode = "404", description = "Projeto nao encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> atualizar(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados atualizados do projeto",
                content = @Content(examples = @ExampleObject(
                    value = "{\"nome\":\"Portal do Cliente v2\",\"descricao\":\"Versao revisada\",\"status\":\"EM_ANDAMENTO\",\"dataInicio\":\"2025-08-01\",\"dataPrazo\":\"2026-03-31\"}")))
            @Valid @RequestBody ProjetoRequestDTO dto) {
        return ResponseEntity.ok(projetoService.atualizar(id, dto));
    }

    @Operation(summary = "Excluir projeto",
               description = "Remove o projeto. Bloqueado se houver tarefas com status TODO ou IN_PROGRESS (RN-P4).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Projeto excluido com sucesso"),
        @ApiResponse(responseCode = "400", description = "Projeto possui tarefas ativas",
            content = @Content(examples = @ExampleObject(value = "{\"status\":400,\"erro\":\"Nao e possivel excluir: projeto possui tarefas ativas\"}"))),
        @ApiResponse(responseCode = "404", description = "Projeto nao encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long id) {
        projetoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Progresso consolidado de todos os projetos",
               description = "Para cada projeto: total de tarefas, quantidade concluida e percentual. Ideal para dashboards executivos.")
    @ApiResponse(responseCode = "200", description = "Relatorio gerado com sucesso")
    @GetMapping("/relatorios/progresso")
    public ResponseEntity<List<Map<String, Object>>> progressoConsolidado() {
        return ResponseEntity.ok(projetoService.progressoDeTodasOsProjetos());
    }

    @Operation(summary = "Projetos com tarefas atrasadas",
               description = "Lista projetos com tarefas vencidas e status ativo. Usado para alertas de risco.")
    @ApiResponse(responseCode = "200", description = "Lista de projetos em risco retornada")
    @GetMapping("/relatorios/em-risco")
    public ResponseEntity<List<Map<String, Object>>> emRisco() {
        return ResponseEntity.ok(projetoService.projetosComTarefasAtrasadas());
    }

    @Operation(summary = "Distribuicao de tarefas por prioridade",
               description = "Quantidade de tarefas por prioridade (CRITICAL, HIGH, MEDIUM, LOW) dentro de um projeto.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribuicao calculada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Projeto nao encontrado")
    })
    @GetMapping("/{id}/relatorios/prioridades")
    public ResponseEntity<List<Map<String, Object>>> distribuicaoPorPrioridade(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(projetoService.distribuicaoPorPrioridade(id));
    }
}
