package com.taskflow.model;

/**
 * Representa o ciclo de vida de uma Tarefa dentro do TaskFlow.
 */
public enum TaskStatus {

    /** Tarefa criada mas ainda não iniciada. */
    TODO,

    /** Tarefa em andamento. */
    IN_PROGRESS,

    /** Tarefa concluída. */
    DONE,

    /** Tarefa cancelada antes da conclusão. */
    CANCELLED
}
