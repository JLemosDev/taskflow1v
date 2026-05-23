package com.taskflow.model;

/**
 * Representa o ciclo de vida de um Projeto dentro do TaskFlow.
 *
 * Utilizado em {@link com.taskflow.service.TarefaService} para a validação
 * da RN-T1 via comparação type-safe com {@code ==}, eliminando comparações
 * frágeis com literais String.
 */
public enum ProjetoStatus {

    /** Projeto em fase de planejamento, ainda não iniciado. */
    PLANEJAMENTO,

    /** Projeto em execução ativa. */
    EM_ANDAMENTO,

    /** Projeto finalizado com sucesso. Não aceita novas tarefas (RN-T1). */
    CONCLUIDO,

    /** Projeto encerrado antes da conclusão. Não aceita novas tarefas (RN-T1). */
    CANCELADO
}
