package com.taskflow.repository;

import com.taskflow.model.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de acesso a dados para a entidade {@link Projeto}.
 */
@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    List<Projeto> findByStatus(String status);

    boolean existsByNome(String nome);

    // ── Query Nativa 4 ───────────────────────────────────────────────────────
    /**
     * Retorna o progresso consolidado de cada projeto: total de tarefas,
     * quantas estão concluídas e o percentual de conclusão.
     *
     * Apenas projetos com ao menos uma tarefa são retornados.
     *
     * Caso de uso: Visão executiva do portfólio de projetos.
     */
    @Query(value = """
            SELECT p.id,
                   p.nome,
                   p.status,
                   p.data_prazo,
                   COUNT(t.id)                                              AS total_tarefas,
                   SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END)     AS tarefas_concluidas,
                   SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END)
                       * 100.0 / COUNT(t.id)                               AS percentual_conclusao
            FROM tb_projeto p
            INNER JOIN tb_tarefa t ON t.projeto_id = p.id
            GROUP BY p.id, p.nome, p.status, p.data_prazo
            ORDER BY percentual_conclusao DESC
            """, nativeQuery = true)
    List<Object[]> findProgressoDeTodasOsProjetos();

    // ── Query Nativa 5 ───────────────────────────────────────────────────────
    /**
     * Lista projetos que possuem tarefas com prazo vencido (data_entrega < hoje)
     * e que ainda não foram concluídas nem canceladas.
     *
     * Caso de uso: Alerta automático de projetos em risco.
     */
    @Query(value = """
            SELECT DISTINCT
                   p.id,
                   p.nome,
                   p.status,
                   COUNT(t.id) AS tarefas_atrasadas
            FROM tb_projeto p
            INNER JOIN tb_tarefa t ON t.projeto_id = p.id
            WHERE t.data_entrega < CURRENT_DATE
              AND t.status NOT IN ('DONE', 'CANCELLED')
            GROUP BY p.id, p.nome, p.status
            ORDER BY tarefas_atrasadas DESC
            """, nativeQuery = true)
    List<Object[]> findProjetosComTarefasAtrasadas();

    // ── Query Nativa 6 ───────────────────────────────────────────────────────
    /**
     * Retorna a distribuição de tarefas por prioridade dentro de um projeto,
     * permitindo ao gestor identificar o perfil de risco do projeto.
     *
     * Caso de uso: Planejamento de sprint e gestão de riscos.
     *
     * @param projetoId ID do projeto a ser analisado
     */
    @Query(value = """
            SELECT t.prioridade,
                   COUNT(t.id)   AS quantidade,
                   SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS concluidas
            FROM tb_tarefa t
            WHERE t.projeto_id = :projetoId
            GROUP BY t.prioridade
            ORDER BY
                CASE t.prioridade
                    WHEN 'CRITICAL' THEN 1
                    WHEN 'HIGH'     THEN 2
                    WHEN 'MEDIUM'   THEN 3
                    WHEN 'LOW'      THEN 4
                END
            """, nativeQuery = true)
    List<Object[]> findDistribuicaoPorPrioridadeNoProje(@Param("projetoId") Long projetoId);
}
