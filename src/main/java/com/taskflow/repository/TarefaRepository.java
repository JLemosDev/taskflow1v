package com.taskflow.repository;

import com.taskflow.model.Tarefa;
import com.taskflow.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de acesso a dados para a entidade {@link Tarefa}.
 *
 * Inclui 5 consultas nativas (nativeQuery = true) com regras de negócio reais.
 */
@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findByProjetoId(Long projetoId);

    List<Tarefa> findByResponsavelId(Long responsavelId);

    List<Tarefa> findByStatus(TaskStatus status);

    // ── Query Nativa 7 ───────────────────────────────────────────────────────
    /**
     * Busca tarefas atrasadas de um usuário específico:
     * prazo vencido + status ativo (não concluído / não cancelado).
     *
     * Caso de uso: Notificação de tarefas vencidas na área do usuário.
     *
     * @param responsavelId ID do usuário responsável
     */
    @Query(value = """
            SELECT t.id,
                   t.titulo,
                   t.prioridade,
                   t.data_entrega,
                   p.nome AS nome_projeto,
                   DATEDIFF(DAY, t.data_entrega, CURRENT_DATE) AS dias_atraso
            FROM tb_tarefa t
            INNER JOIN tb_projeto p ON p.id = t.projeto_id
            WHERE t.responsavel_id = :responsavelId
              AND t.data_entrega   < CURRENT_DATE
              AND t.status NOT IN ('DONE', 'CANCELLED')
            ORDER BY t.data_entrega ASC
            """, nativeQuery = true)
    List<Object[]> findTarefasAtrasadasByResponsavel(@Param("responsavelId") Long responsavelId);

    // ── Query Nativa 8 ───────────────────────────────────────────────────────
    /**
     * Retorna tarefas CRITICAL ou HIGH sem responsável atribuído em projetos ativos.
     *
     * Caso de uso: Alerta de tarefas críticas não atribuídas que bloqueiam o progresso.
     */
    @Query(value = """
            SELECT t.id,
                   t.titulo,
                   t.prioridade,
                   t.data_entrega,
                   p.nome AS nome_projeto,
                   p.id   AS projeto_id
            FROM tb_tarefa t
            INNER JOIN tb_projeto p ON p.id = t.projeto_id
            WHERE t.responsavel_id IS NULL
              AND t.prioridade IN ('CRITICAL', 'HIGH')
              AND t.status NOT IN ('DONE', 'CANCELLED')
              AND p.status NOT IN ('CONCLUIDO', 'CANCELADO')
            ORDER BY t.prioridade, t.data_entrega ASC
            """, nativeQuery = true)
    List<Object[]> findTarefasCriticasSemResponsavel();

    // ── Query Nativa 9 ───────────────────────────────────────────────────────
    /**
     * Calcula a eficiência de estimativa de cada tarefa concluída:
     * compara horas estimadas com horas reais trabalhadas.
     * Retorna apenas tarefas onde o desvio ultrapassou o threshold informado (%).
     *
     * Caso de uso: Relatório de acurácia de planejamento para retrospectivas.
     *
     * @param thresholdPct desvio percentual mínimo para considerar (ex: 20 = 20%)
     */
    @Query(value = """
            SELECT t.id,
                   t.titulo,
                   p.nome                                                   AS nome_projeto,
                   t.horas_estimadas,
                   t.horas_trabalhadas,
                   ABS(t.horas_trabalhadas - t.horas_estimadas)            AS desvio_horas,
                   ABS(t.horas_trabalhadas - t.horas_estimadas)
                       * 100.0 / t.horas_estimadas                         AS desvio_percentual
            FROM tb_tarefa t
            INNER JOIN tb_projeto p ON p.id = t.projeto_id
            WHERE t.status          = 'DONE'
              AND t.horas_estimadas IS NOT NULL
              AND t.horas_estimadas  > 0
              AND ABS(t.horas_trabalhadas - t.horas_estimadas)
                  * 100.0 / t.horas_estimadas > :thresholdPct
            ORDER BY desvio_percentual DESC
            """, nativeQuery = true)
    List<Object[]> findTarefasComDesvioDeEstimativa(@Param("thresholdPct") double thresholdPct);

    // ── Query Nativa 10 ──────────────────────────────────────────────────────
    /**
     * Atualiza em lote o responsável de todas as tarefas de um usuário origem
     * para um usuário destino, restrito a tarefas ativas (não concluídas/canceladas).
     *
     * Caso de uso: Reatribuição em massa quando um colaborador sai do projeto.
     *
     * @param origemId  ID do usuário que está sendo desligado / realocado
     * @param destinoId ID do usuário que irá assumir as tarefas
     */
    @Modifying
    @Query(value = """
            UPDATE tb_tarefa
            SET responsavel_id = :destinoId,
                atualizado_em  = CURRENT_TIMESTAMP
            WHERE responsavel_id = :origemId
              AND status NOT IN ('DONE', 'CANCELLED')
            """, nativeQuery = true)
    int reatribuirTarefasEmLote(@Param("origemId") Long origemId,
                                @Param("destinoId") Long destinoId);

    // ── Query Nativa 11 ──────────────────────────────────────────────────────
    /**
     * Retorna o histórico de fluxo de tarefas de um projeto agrupado por semana:
     * quantas tarefas foram criadas e quantas foram concluídas em cada semana.
     *
     * Caso de uso: Gráfico de burndown/velocidade da equipe no projeto.
     *
     * @param projetoId ID do projeto
     */
    @Query(value = """
            SELECT WEEK(criado_em)                                      AS semana,
                   YEAR(criado_em)                                      AS ano,
                   COUNT(id)                                            AS tarefas_criadas,
                   SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END)   AS tarefas_concluidas
            FROM tb_tarefa
            WHERE projeto_id = :projetoId
            GROUP BY YEAR(criado_em), WEEK(criado_em)
            ORDER BY ano, semana
            """, nativeQuery = true)
    List<Object[]> findFluxoSemanalByProjeto(@Param("projetoId") Long projetoId);
}
