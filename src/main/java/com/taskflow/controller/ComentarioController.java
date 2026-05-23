package com.taskflow.controller;

import com.taskflow.dto.request.ComentarioRequestDTO;
import com.taskflow.dto.response.ComentarioResponseDTO;
import com.taskflow.service.ComentarioService;
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
 * Controller REST para gerenciamento de Comentários.
 */
@RestController
@RequestMapping("/api/comentarios")
@RequiredArgsConstructor
@Tag(name = "Comentários", description = "Criação, listagem e exclusão de comentários em tarefas")
public class ComentarioController {

    private final ComentarioService comentarioService;

    // ── POST /api/comentarios ─────────────────────────────────────────────────
    @Operation(
        summary     = "Criar comentário",
        description = """
                Adiciona um comentário a uma tarefa. Regras aplicadas:
                - **RN-C1**: Tarefas com status CANCELLED não aceitam comentários.
                - **RN-C2**: Conteúdo não pode ser vazio.
                """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comentário criado com sucesso",
            content = @Content(schema = @Schema(implementation = ComentarioResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Tarefa cancelada ou conteúdo vazio",
            content = @Content(examples = @ExampleObject(
                value = """
                        {"status":400,"erro":"Não é permitido adicionar comentários a tarefas canceladas."}"""))),
        @ApiResponse(responseCode = "404", description = "Tarefa ou autor não encontrado")
    })
    @PostMapping
    public ResponseEntity<ComentarioResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do comentário",
                content = @Content(examples = @ExampleObject(
                    value = """
                            {
                              "conteudo": "Revisando os critérios de aceite antes de iniciar.",
                              "tarefaId": 1,
                              "autorId":  2
                            }""")))
            @Valid @RequestBody ComentarioRequestDTO dto) {

        ComentarioResponseDTO criado = comentarioService.criar(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(location).body(criado);
    }

    // ── GET /api/comentarios/tarefa/{tarefaId} ────────────────────────────────
    @Operation(
        summary     = "Listar comentários de uma tarefa",
        description = "Retorna todos os comentários de uma tarefa específica, ordenados do mais recente para o mais antigo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comentários retornados com sucesso"),
        @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    })
    @GetMapping("/tarefa/{tarefaId}")
    public ResponseEntity<List<ComentarioResponseDTO>> listarPorTarefa(
            @Parameter(description = "ID da tarefa", example = "1")
            @PathVariable Long tarefaId) {

        return ResponseEntity.ok(comentarioService.listarPorTarefa(tarefaId));
    }

    // ── DELETE /api/comentarios/{id}?solicitanteId=2 ──────────────────────────
    @Operation(
        summary     = "Excluir comentário",
        description = "Remove um comentário. Apenas o autor original pode excluir o próprio comentário (RN-C3)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comentário excluído com sucesso"),
        @ApiResponse(responseCode = "400", description = "Solicitante não é o autor do comentário",
            content = @Content(examples = @ExampleObject(
                value = """
                        {"status":400,"erro":"Apenas o autor do comentário pode excluí-lo."}"""))),
        @ApiResponse(responseCode = "404", description = "Comentário não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID do comentário", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID do usuário que está solicitando a exclusão", example = "2")
            @RequestParam Long solicitanteId) {

        comentarioService.excluir(id, solicitanteId);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/comentarios/relatorios/engajamento/{projetoId} ──────────────
    @Operation(
        summary     = "Engajamento de comentários por projeto",
        description = "Retorna quantos comentários cada membro fez em um projeto, " +
                      "com a data do último comentário. Indica o nível de participação da equipe."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatório de engajamento retornado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    })
    @GetMapping("/relatorios/engajamento/{projetoId}")
    public ResponseEntity<List<Map<String, Object>>> engajamento(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long projetoId) {

        return ResponseEntity.ok(comentarioService.engajamentoPorProjeto(projetoId));
    }
}
