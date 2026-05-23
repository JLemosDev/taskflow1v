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

@RestController
@RequestMapping("/api/comentarios")
@RequiredArgsConstructor
@Tag(name = "Comentarios", description = "Criacao, listagem e exclusao de comentarios em tarefas")
public class ComentarioController {

    private final ComentarioService comentarioService;

    @Operation(summary = "Criar comentario",
               description = "Adiciona um comentario a uma tarefa. RN-C1: tarefas CANCELLED nao aceitam comentarios. RN-C2: conteudo nao pode ser vazio.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comentario criado com sucesso",
            content = @Content(schema = @Schema(implementation = ComentarioResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Tarefa cancelada ou conteudo vazio",
            content = @Content(examples = @ExampleObject(value = "{\"status\":400,\"erro\":\"Nao e permitido comentar em tarefas canceladas\"}"))),
        @ApiResponse(responseCode = "404", description = "Tarefa ou autor nao encontrado")
    })
    @PostMapping
    public ResponseEntity<ComentarioResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do comentario",
                content = @Content(examples = @ExampleObject(
                    value = "{\"conteudo\":\"Revisando os criterios de aceite antes de iniciar.\",\"tarefaId\":1,\"autorId\":2}")))
            @Valid @RequestBody ComentarioRequestDTO dto) {

        ComentarioResponseDTO criado = comentarioService.criar(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(summary = "Listar comentarios de uma tarefa",
               description = "Retorna todos os comentarios de uma tarefa, ordenados do mais recente ao mais antigo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comentarios retornados com sucesso"),
        @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada")
    })
    @GetMapping("/tarefa/{tarefaId}")
    public ResponseEntity<List<ComentarioResponseDTO>> listarPorTarefa(
            @Parameter(description = "ID da tarefa", example = "1")
            @PathVariable Long tarefaId) {
        return ResponseEntity.ok(comentarioService.listarPorTarefa(tarefaId));
    }

    @Operation(summary = "Excluir comentario",
               description = "Remove um comentario. Apenas o autor pode excluir o proprio comentario (RN-C3).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comentario excluido com sucesso"),
        @ApiResponse(responseCode = "400", description = "Solicitante nao e o autor do comentario",
            content = @Content(examples = @ExampleObject(value = "{\"status\":400,\"erro\":\"Apenas o autor do comentario pode exclui-lo\"}"))),
        @ApiResponse(responseCode = "404", description = "Comentario nao encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID do comentario", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID do usuario que solicita a exclusao", example = "2")
            @RequestParam Long solicitanteId) {
        comentarioService.excluir(id, solicitanteId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Engajamento de comentarios por projeto",
               description = "Quantos comentarios cada membro fez no projeto, com data do ultimo. Indica participacao da equipe.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatorio de engajamento retornado"),
        @ApiResponse(responseCode = "404", description = "Projeto nao encontrado")
    })
    @GetMapping("/relatorios/engajamento/{projetoId}")
    public ResponseEntity<List<Map<String, Object>>> engajamento(
            @Parameter(description = "ID do projeto", example = "1")
            @PathVariable Long projetoId) {
        return ResponseEntity.ok(comentarioService.engajamentoPorProjeto(projetoId));
    }
}
