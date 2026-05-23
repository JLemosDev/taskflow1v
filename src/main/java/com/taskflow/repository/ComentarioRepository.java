package com.taskflow.repository;

import com.taskflow.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de acesso a dados para a entidade {@link Comentario}.
 */
@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    List<Comentario> findByTarefaIdOrderByCriadoEmDesc(Long tarefaId);

    /**
     * Query Nativa 12 – Consulta de atividade de comentários por usuário.
     *
     * Retorna, por projeto, quantos comentários cada usuário fez,
     * indicando nível de engajamento da equipe.
     *
     * Caso de uso: Relatório de participação em projetos.
     *
     * @param projetoId ID do projeto a ser analisado
     */
    @Query(value = """
            SELECT u.nome            AS autor,
                   p.nome            AS projeto,
                   COUNT(c.id)       AS total_comentarios,
                   MAX(c.criado_em)  AS ultimo_comentario
            FROM tb_comentario c
            INNER JOIN tb_usuario u ON u.id = c.autor_id
            INNER JOIN tb_tarefa  t ON t.id = c.tarefa_id
            INNER JOIN tb_projeto p ON p.id = t.projeto_id
            WHERE p.id = :projetoId
            GROUP BY u.nome, p.nome
            ORDER BY total_comentarios DESC
            """, nativeQuery = true)
    List<Object[]> findEngajamentoComentariosPorProjeto(@Param("projetoId") Long projetoId);
}
