package com.comercio.shared.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

/** Cuerpo JSON uniforme de error: {@code {error, message, timestamp}}. */
public record ErrorResponse(String error, String message, String timestamp) {

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now().toString());
    }

    public static Response response(Response.Status status, String error, String message) {
        return response(status.getStatusCode(), error, message);
    }

    public static Response response(int status, String error, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(of(error, message))
                .build();
    }
}
