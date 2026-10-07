package com.comercio.shared.exception;

/**
 * Base de las excepciones de negocio. Son unchecked para que los interceptores
 * {@code @Transactional} marquen la transacción JTA para rollback por defecto.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    /** Código estable que se devuelve en el campo {@code error} del JSON. */
    public abstract String errorCode();
}
