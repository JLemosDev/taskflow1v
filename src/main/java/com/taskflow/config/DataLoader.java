package com.taskflow.config;

import com.taskflow.model.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

/**
 * Popula o banco com dados de exemplo ao iniciar a aplicação.
 * Verifica se os dados já existem antes de inserir — compatível com
 * H2 file mode (ddl-auto=update), evitando erros de chave duplicada
 * em reinicializações.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    @Bean
    CommandLineRunner carregarDados(
            UsuarioRepository    usuarioRepo,
            ProjetoRepository    projetoRepo,
            TarefaRepository     tarefaRepo,
            ComentarioRepository comentarioRepo) {

        return args -> {

            // Se já existem dados, não faz nada
            if (usuarioRepo.count() > 0) {
                log.info(">>> Banco já populado ({} usuários encontrados). Pulando DataLoader.",
                        usuarioRepo.count());
                return;
            }

            log.info(">>> Banco vazio. Iniciando carga de dados de exemplo...");

            // ── Usuários ──────────────────────────────────────────────────────
            Usuario ana = usuarioRepo.save(Usuario.builder()
                    .nome("Ana Lima").email("ana.lima@taskflow.com").role("GERENTE").build());

            Usuario joao = usuarioRepo.save(Usuario.builder()
                    .nome("João Silva").email("joao.silva@taskflow.com").role("MEMBRO").build());

            Usuario carla = usuarioRepo.save(Usuario.builder()
                    .nome("Carla Souza").email("carla.souza@taskflow.com").role("MEMBRO").build());

            Usuario admin = usuarioRepo.save(Usuario.builder()
                    .nome("Admin").email("admin@taskflow.com").role("ADMIN").build());

            // ── Projetos ──────────────────────────────────────────────────────
            Projeto portal = projetoRepo.save(Projeto.builder()
                    .nome("Portal do Cliente")
                    .descricao("Desenvolvimento do portal de autoatendimento")
                    .status("EM_ANDAMENTO")
                    .dataInicio(LocalDate.now().minusMonths(2))
                    .dataPrazo(LocalDate.now().plusMonths(4))
                    .build());

            Projeto mobile = projetoRepo.save(Projeto.builder()
                    .nome("App Mobile")
                    .descricao("Aplicativo iOS e Android para clientes")
                    .status("PLANEJAMENTO")
                    .dataInicio(LocalDate.now().plusWeeks(2))
                    .dataPrazo(LocalDate.now().plusMonths(8))
                    .build());

            // ── Tarefas do Portal ─────────────────────────────────────────────
            Tarefa t1 = tarefaRepo.save(Tarefa.builder()
                    .titulo("Implementar tela de login")
                    .descricao("Formulário de autenticação com JWT")
                    .status(TaskStatus.DONE)
                    .prioridade(Priority.HIGH)
                    .dataEntrega(LocalDate.now().minusDays(10))
                    .horasEstimadas(8).horasTrabalhadas(9)
                    .projeto(portal).responsavel(joao).build());

            Tarefa t2 = tarefaRepo.save(Tarefa.builder()
                    .titulo("Criar dashboard principal")
                    .descricao("Gráficos de resumo e KPIs")
                    .status(TaskStatus.IN_PROGRESS)
                    .prioridade(Priority.HIGH)
                    .dataEntrega(LocalDate.now().plusDays(7))
                    .horasEstimadas(16).horasTrabalhadas(6)
                    .projeto(portal).responsavel(carla).build());

            Tarefa t3 = tarefaRepo.save(Tarefa.builder()
                    .titulo("Configurar CI/CD")
                    .descricao("Pipeline de build e deploy automatizado")
                    .status(TaskStatus.TODO)
                    .prioridade(Priority.CRITICAL)
                    .dataEntrega(LocalDate.now().plusDays(14))
                    .horasEstimadas(12)
                    .projeto(portal).responsavel(ana).build());

            Tarefa t4 = tarefaRepo.save(Tarefa.builder()
                    .titulo("Revisar paleta de cores")
                    .descricao("Adequar ao novo guia de marca")
                    .status(TaskStatus.TODO)
                    .prioridade(Priority.LOW)
                    .dataEntrega(LocalDate.now().minusDays(3))
                    .horasEstimadas(4)
                    .projeto(portal).responsavel(joao).build());

            Tarefa t5 = tarefaRepo.save(Tarefa.builder()
                    .titulo("Testes de segurança")
                    .descricao("Pentest e validação de permissões")
                    .status(TaskStatus.TODO)
                    .prioridade(Priority.CRITICAL)
                    .dataEntrega(LocalDate.now().plusDays(21))
                    .horasEstimadas(20)
                    .projeto(portal).responsavel(ana).build());

            // ── Tarefas do App Mobile ─────────────────────────────────────────
            tarefaRepo.save(Tarefa.builder()
                    .titulo("Definir arquitetura do app")
                    .descricao("Escolha entre React Native e Flutter")
                    .status(TaskStatus.TODO)
                    .prioridade(Priority.HIGH)
                    .dataEntrega(LocalDate.now().plusDays(30))
                    .horasEstimadas(6)
                    .projeto(mobile).responsavel(ana).build());

            tarefaRepo.save(Tarefa.builder()
                    .titulo("Wireframes das telas principais")
                    .descricao("Protótipo navegável das 5 telas core")
                    .status(TaskStatus.TODO)
                    .prioridade(Priority.MEDIUM)
                    .dataEntrega(LocalDate.now().plusDays(45))
                    .horasEstimadas(10)
                    .projeto(mobile).build());

            // ── Comentários ───────────────────────────────────────────────────
            comentarioRepo.save(Comentario.builder()
                    .conteudo("Login implementado, aguardando revisão de código.")
                    .tarefa(t1).autor(joao).build());

            comentarioRepo.save(Comentario.builder()
                    .conteudo("Revisado e aprovado! Merge feito na main.")
                    .tarefa(t1).autor(ana).build());

            comentarioRepo.save(Comentario.builder()
                    .conteudo("Gráfico de pizza já está pronto, faltam os KPIs de conversão.")
                    .tarefa(t2).autor(carla).build());

            comentarioRepo.save(Comentario.builder()
                    .conteudo("Precisamos definir quais métricas exibir. Reunião amanhã?")
                    .tarefa(t2).autor(ana).build());

            comentarioRepo.save(Comentario.builder()
                    .conteudo("Vou usar GitHub Actions + Docker. Começo na sexta.")
                    .tarefa(t3).autor(ana).build());

            log.info(">>> Dados carregados com sucesso: {} usuários, {} projetos, {} tarefas, {} comentários.",
                    usuarioRepo.count(), projetoRepo.count(),
                    tarefaRepo.count(), comentarioRepo.count());
        };
    }
}
