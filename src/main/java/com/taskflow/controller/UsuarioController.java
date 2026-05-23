package com.taskflow.controller;

import com.taskflow.dto.request.UsuarioRequestDTO;
import com.taskflow.dto.response.UsuarioResponseDTO;
import com.taskflow.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "CRUD de usuarios e relatorios de produtividade da equipe")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Criar usuario", description = "Cria um novo usuario. O e-mail deve ser unico (RN-U1).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario criado com sucesso",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados invalidos",
            content = @Content(examples = @ExampleObject(value = "{\"status\":400,\"erro\":\"Erro de validacao nos campos\"}"))),
        @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado",
            content = @Content(examples = @ExampleObject(value = "{\"status\":409,\"erro\":\"Ja existe um usuario com esse e-mail\"}")))
    })
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do novo usuario",
                content = @Content(examples = @ExampleObject(
                    value = "{\"nome\":\"Joao Silva\",\"email\":\"joao.silva@email.com\",\"role\":\"MEMBRO\"}")))
            @Valid @RequestBody UsuarioRequestDTO dto) {

        UsuarioResponseDTO criado = usuarioService.criar(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(summary = "Listar todos os usuarios", description = "Retorna a lista completa de usuarios com o total de tarefas de cada um.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @Operation(summary = "Buscar usuario por ID", description = "Retorna os dados de um usuario pelo seu ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
            content = @Content(examples = @ExampleObject(value = "{\"status\":404,\"erro\":\"Usuario nao encontrado com ID: 99\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(
            @Parameter(description = "ID do usuario", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar usuario", description = "Atualiza nome, e-mail e role. Verifica conflito de e-mail (RN-U4).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario atualizado"),
        @ApiResponse(responseCode = "404", description = "Usuario nao encontrado"),
        @ApiResponse(responseCode = "409", description = "E-mail ja em uso por outro usuario")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @Parameter(description = "ID do usuario", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Novos dados do usuario",
                content = @Content(examples = @ExampleObject(
                    value = "{\"nome\":\"Joao Silva Atualizado\",\"email\":\"joao.novo@email.com\",\"role\":\"GERENTE\"}")))
            @Valid @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    @Operation(summary = "Excluir usuario", description = "Remove um usuario. Bloqueado se houver tarefas ativas (RN-U2).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuario excluido com sucesso"),
        @ApiResponse(responseCode = "400", description = "Usuario possui tarefas ativas",
            content = @Content(examples = @ExampleObject(value = "{\"status\":400,\"erro\":\"Nao e possivel excluir: usuario possui tarefas ativas\"}"))),
        @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID do usuario", example = "1")
            @PathVariable Long id) {
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ranking por tarefas em andamento",
               description = "Lista usuarios ordenados pelo numero de tarefas com status IN_PROGRESS.")
    @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso")
    @GetMapping("/relatorios/ranking-andamento")
    public ResponseEntity<List<Map<String, Object>>> rankingAndamento() {
        return ResponseEntity.ok(usuarioService.rankingPorTarefasEmAndamento());
    }

    @Operation(summary = "Usuarios sobrecarregados",
               description = "Retorna usuarios com mais tarefas ativas do que o limite informado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Limite invalido (deve ser >= 1)")
    })
    @GetMapping("/relatorios/sobrecarregados")
    public ResponseEntity<List<Map<String, Object>>> sobrecarregados(
            @Parameter(description = "Limite maximo de tarefas ativas", example = "5")
            @RequestParam(defaultValue = "5") int limite) {
        return ResponseEntity.ok(usuarioService.usuariosSobrecarregados(limite));
    }

    @Operation(summary = "Estatisticas de produtividade do usuario",
               description = "Total de tarefas concluidas, media de horas e percentual no prazo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estatisticas calculadas com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    @GetMapping("/{id}/relatorios/produtividade")
    public ResponseEntity<Map<String, Object>> produtividade(
            @Parameter(description = "ID do usuario", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.estatisticasProdutividade(id));
    }
}
