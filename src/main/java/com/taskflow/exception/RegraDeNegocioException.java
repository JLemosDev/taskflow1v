package com.taskflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Lançada quando uma regra de negócio é violada (HTTP 400). */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
