package com.comercio.shared.api;

import com.comercio.shared.exception.BusinessRuleException;
import com.comercio.shared.exception.DomainException;
import com.comercio.shared.exception.DuplicateSkuException;
import com.comercio.shared.exception.InsufficientStockException;
import com.comercio.shared.exception.InvalidOrderStateException;
import com.comercio.shared.exception.OrderNotFoundException;
import com.comercio.shared.exception.PaymentRejectedException;
import com.comercio.shared.exception.ProductNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Excepciones de dominio → JSON {@code {error, message, timestamp}}.
 * <ul>
 *   <li>404: el recurso no existe (ProductNotFound, OrderNotFound).</li>
 *   <li>409: conflicto con el estado actual (InsufficientStock, InvalidOrderState, sku duplicado).</li>
 *   <li>422: petición válida que incumple una regla de negocio (PaymentRejected, producto inactivo).</li>
 * </ul>
 * Los 400 (Bean Validation) los produce {@link ConstraintViolationExceptionMapper}.
 */
@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    private static final int UNPROCESSABLE_ENTITY = 422;

    private static final Map<Class<? extends DomainException>, Integer> STATUS = Map.of(
            ProductNotFoundException.class, Status.NOT_FOUND.getStatusCode(),
            OrderNotFoundException.class, Status.NOT_FOUND.getStatusCode(),
            InsufficientStockException.class, Status.CONFLICT.getStatusCode(),
            InvalidOrderStateException.class, Status.CONFLICT.getStatusCode(),
            DuplicateSkuException.class, Status.CONFLICT.getStatusCode(),
            PaymentRejectedException.class, UNPROCESSABLE_ENTITY,
            BusinessRuleException.class, UNPROCESSABLE_ENTITY);

    @Override
    public Response toResponse(DomainException e) {
        int status = STATUS.getOrDefault(e.getClass(), UNPROCESSABLE_ENTITY);
        return ErrorResponse.response(status, e.errorCode(), e.getMessage());
    }
}
