package com.comercio.shared.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Conserva el código de las excepciones propias de JAX-RS (ruta inexistente, 405, 415, JSON inválido...) con cuerpo JSON. */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException e) {
        Response.StatusType status = e.getResponse().getStatusInfo();
        String message = e.getMessage() != null ? e.getMessage() : status.getReasonPhrase();
        return ErrorResponse.response(status.getStatusCode(),
                status.getReasonPhrase().toUpperCase().replace(' ', '_'), message);
    }
}
