package com.comercio.shared.api;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Cuerpo JSON mal formado o con tipos incompatibles (JSON-B no puede deserializarlo) → 400. */
@Provider
public class MalformedJsonExceptionMapper implements ExceptionMapper<ProcessingException> {

    @Override
    public Response toResponse(ProcessingException e) {
        return ErrorResponse.response(Response.Status.BAD_REQUEST, "MALFORMED_JSON",
                "El cuerpo de la petición no es un JSON válido para este recurso");
    }
}
