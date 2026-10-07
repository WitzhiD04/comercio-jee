package com.comercio.shared.api;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Conflicto de {@code @Version} (dos escrituras concurrentes sobre la misma entidad) → 409. */
@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {

    @Override
    public Response toResponse(OptimisticLockException e) {
        return ErrorResponse.response(Response.Status.CONFLICT, "CONCURRENT_MODIFICATION",
                "El recurso fue modificado por otra operación; vuelva a intentarlo");
    }
}
