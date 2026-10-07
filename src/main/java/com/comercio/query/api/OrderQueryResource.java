package com.comercio.query.api;

import com.comercio.query.api.QueryDtos.OrderSummary;
import com.comercio.query.application.OrderQueryService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

/**
 * Consultas de pedidos (queryPU). Comparte la ruta /orders con OrderCommandResource.
 * La traza de la SAGA (/orders/{id}/saga) la sirve {@code com.comercio.saga.api.SagaQueryResource}.
 */
@Path("orders")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class OrderQueryResource {

    @Inject
    OrderQueryService queries;

    @GET
    @Path("{id}")
    public OrderSummary get(@PathParam("id") UUID id) {
        return OrderSummary.from(queries.getOrder(id));
    }

    @GET
    public List<OrderSummary> list(@QueryParam("customerId") String customerId) {
        return queries.listOrders(customerId).stream().map(OrderSummary::from).toList();
    }
}
