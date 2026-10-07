package com.comercio.shared.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

/** Errores de Bean Validation en DTOs o entidades → 400. */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolationExceptionMapper::describe)
                .sorted()
                .collect(Collectors.joining("; "));
        return ErrorResponse.response(Response.Status.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private static String describe(ConstraintViolation<?> v) {
        // "create.arg0.price" -> "price": el método y el parámetro no le aportan nada al cliente
        String path = v.getPropertyPath().toString();
        int idx = path.lastIndexOf('.');
        String field = idx >= 0 ? path.substring(idx + 1) : path;
        return field + ": " + v.getMessage();
    }
}
