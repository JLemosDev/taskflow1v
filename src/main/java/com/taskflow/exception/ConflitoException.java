package com.taskflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Lançada quando há conflito de dados únicos, ex: e-mail já cadastrado (HTTP 409). */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
