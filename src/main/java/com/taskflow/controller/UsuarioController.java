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

/**
 * Controller REST para gerenciamento de Usuários.
 * Recebe apenas RequestDTOs e retorna apenas ResponseDTOs envelopados em ResponseEntity.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "CRUD de usuários e relatórios de produtividade da equipe")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ── POST /api/usuarios ────────────────────────────────────────────────────
    @Operation(
        summary     = "Criar usuário",
        description = "Cria um novo usuário no sistema. O e-mail deve ser único (RN-U1)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (validação de campos)",
            content = @Content(examples = @ExampleObject(
                value = """
                        {"status":400,"erro":"Erro de validação nos campos",
                         "campos":{"email":"Formato de e-mail inválido"}}"""))),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(examples = @ExampleObject(
                value = """
                        {"status":409,"erro":"Já existe um usuário cadastrado com o e-mail: joao@email.com"}""")))
    })
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do novo usuário",
                content = @Content(examples = @ExampleObject(
                    value = """
                            {
                              "nome":  "João Silva",
                              "email": "joao.silva@email.com",
                              "role":  "MEMBRO"
                            }""")))
            @Valid @RequestBody UsuarioRequestDTO dto) {

        UsuarioResponseDTO criado = usuarioService.criar(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(location).body(criado);
    }

    // ── GET /api/usuarios ─────────────────────────────────────────────────────
    @Operation(
        summary     = "Listar todos os usuários",
        description = "Retorna a lista completa de usuários cadastrados com o total de tarefas de cada um."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // ── GET /api/usuarios/{id} ────────────────────────────────────────────────
    @Operation(
        summary     = "Buscar usuário por ID",
        description = "Retorna os dados de um usuário específico pelo seu ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(examples = @ExampleObject(
                value = """{"status":404,"erro":"Usuário não encontrado com ID: 99"}""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    // ── PUT /api/usuarios/{id} ────────────────────────────────────────────────
    @Operation(
        summary     = "Atualizar usuário",
        description = "Atualiza nome, e-mail e role de um usuário. Verifica conflito de e-mail com outros usuários (RN-U4)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "409", description = "E-mail já está em uso por outro usuário")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Novos dados do usuário",
                content = @Content(examples = @ExampleObject(
                    value = """
                            {
                              "nome":  "João Silva Atualizado",
                              "email": "joao.novo@email.com",
                              "role":  "GERENTE"
                            }""")))
            @Valid @RequestBody UsuarioRequestDTO dto) {

        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    // ── DELETE /api/usuarios/{id} ─────────────────────────────────────────────
    @Operation(
        summary     = "Excluir usuário",
        description = "Remove um usuário do sistema. Bloqueado se houver tarefas ativas atribuídas a ele (RN-U2)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso"),
        @ApiResponse(responseCode = "400", description = "Usuário possui tarefas ativas — reatribua antes",
            content = @Content(examples = @ExampleObject(
                value = """
                        {"status":400,"erro":"Não é possível excluir o usuário 'João' pois ele possui 3 tarefa(s) ativa(s)."}"""))),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Long id) {

        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Relatórios
    // ══════════════════════════════════════════════════════════════════════════

    // ── GET /api/usuarios/relatorios/ranking-andamento ────────────────────────
    @Operation(
        summary     = "Ranking por tarefas em andamento",
        description = "Lista usuários ordenados pelo número de tarefas com status IN_PROGRESS. " +
                      "Útil para visualizar a carga de trabalho atual da equipe."
    )
    @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso")
    @GetMapping("/relatorios/ranking-andamento")
    public ResponseEntity<List<Map<String, Object>>> rankingAndamento() {
        return ResponseEntity.ok(usuarioService.rankingPorTarefasEmAndamento());
    }

    // ── GET /api/usuarios/relatorios/sobrecarregados?limite=5 ─────────────────
    @Operation(
        summary     = "Usuários sobrecarregados",
        description = "Retorna usuários com mais tarefas ativas (TODO + IN_PROGRESS) do que o limite informado. " +
                      "Usado para balancear a distribuição antes de novas atribuições."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Limite inválido (deve ser ≥ 1)")
    })
    @GetMapping("/relatorios/sobrecarregados")
    public ResponseEntity<List<Map<String, Object>>> sobrecarregados(
            @Parameter(description = "Limite máximo aceitável de tarefas ativas", example = "5")
            @RequestParam(defaultValue = "5") int limite) {

        return ResponseEntity.ok(usuarioService.usuariosSobrecarregados(limite));
    }

    // ── GET /api/usuarios/{id}/relatorios/produtividade ───────────────────────
    @Operation(
        summary     = "Estatísticas de produtividade do usuário",
        description = "Retorna o total de tarefas concluídas, média de horas trabalhadas " +
                      "e percentual de entregas no prazo para o usuário informado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estatísticas calculadas com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/{id}/relatorios/produtividade")
    public ResponseEntity<Map<String, Object>> produtividade(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(usuarioService.estatisticasProdutividade(id));
    }
}
