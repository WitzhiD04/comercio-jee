package com.comercio.shared.api;

import com.comercio.shared.exception.DomainException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Último recurso. Antes de responder 500 busca la causa raíz: cuando falla el commit de JTA, la excepción de
 * dominio o la {@link OptimisticLockException} llegan envueltas en {@code TransactionalException}/{@code RollbackException}.
 */
@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(UnhandledExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception e) {
        for (Throwable t = e; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof DomainException de) {
                return new DomainExceptionMapper().toResponse(de);
            }
            if (t instanceof OptimisticLockException ole) {
                return new OptimisticLockExceptionMapper().toResponse(ole);
            }
            if (t instanceof ConstraintViolationException cve) {
                return new ConstraintViolationExceptionMapper().toResponse(cve);
            }
        }
        LOG.log(Level.SEVERE, "Error no controlado", e);
        return ErrorResponse.response(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Error interno del servidor");
    }
}
