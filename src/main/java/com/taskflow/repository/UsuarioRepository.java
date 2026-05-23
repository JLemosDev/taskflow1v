package com.taskflow.repository;

import com.taskflow.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de acesso a dados para a entidade {@link Usuario}.
 *
 * Contém consultas nativas otimizadas para relatórios e regras de negócio.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // ── Consulta JPQL (Spring Data) ──────────────────────────────────────────

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // ── Query Nativa 1 ───────────────────────────────────────────────────────
    /**
     * Retorna o ranking de usuários com mais tarefas em andamento (status = 'IN_PROGRESS'),
     * ordenado de forma decrescente pela quantidade.
     *
     * Caso de uso: Dashboard de carga de trabalho da equipe.
     */
    @Query(value = """
            SELECT u.id,
                   u.nome,
                   u.email,
                   COUNT(t.id) AS tarefas_em_andamento
            FROM tb_usuario u
            INNER JOIN tb_tarefa t ON t.responsavel_id = u.id
            WHERE t.status = 'IN_PROGRESS'
            GROUP BY u.id, u.nome, u.email
            ORDER BY tarefas_em_andamento DESC
            """, nativeQuery = true)
    List<Object[]> findRankingUsuariosPorTarefasEmAndamento();

    // ── Query Nativa 2 ───────────────────────────────────────────────────────
    /**
     * Retorna usuários que estão sobrecarregados: têm mais tarefas atribuídas
     * do que o limite informado por parâmetro.
     *
     * Caso de uso: Balanceamento de carga antes de atribuir novas tarefas.
     *
     * @param limite quantidade máxima aceitável de tarefas ativas por usuário
     */
    @Query(value = """
            SELECT u.id,
                   u.nome,
                   u.email,
                   COUNT(t.id) AS total_tarefas_ativas
            FROM tb_usuario u
            INNER JOIN tb_tarefa t ON t.responsavel_id = u.id
            WHERE t.status IN ('TODO', 'IN_PROGRESS')
            GROUP BY u.id, u.nome, u.email
            HAVING COUNT(t.id) > :limite
            ORDER BY total_tarefas_ativas DESC
            """, nativeQuery = true)
    List<Object[]> findUsuariosSobrecarregados(@Param("limite") int limite);

    // ── Query Nativa 3 ───────────────────────────────────────────────────────
    /**
     * Retorna estatísticas de produtividade de um usuário específico:
     * total de tarefas concluídas, média de horas trabalhadas e
     * percentual de tarefas entregues no prazo.
     *
     * Caso de uso: Avaliação de desempenho individual.
     *
     * @param usuarioId ID do usuário a ser avaliado
     */
    @Query(value = """
            SELECT u.nome,
                   COUNT(t.id)                                          AS total_concluidas,
                   COALESCE(AVG(t.horas_trabalhadas), 0)               AS media_horas,
                   SUM(CASE WHEN t.data_entrega >= CURRENT_DATE
                            THEN 1 ELSE 0 END) * 100.0 / COUNT(t.id)  AS pct_no_prazo
            FROM tb_usuario u
            INNER JOIN tb_tarefa t ON t.responsavel_id = u.id
            WHERE u.id   = :usuarioId
              AND t.status = 'DONE'
            GROUP BY u.nome
            """, nativeQuery = true)
    Optional<Object[]> findEstatisticasProdutividadeByUsuario(@Param("usuarioId") Long usuarioId);
}
