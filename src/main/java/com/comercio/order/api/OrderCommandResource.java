package com.comercio.order.api;

import com.comercio.order.api.OrderDtos.OrderAcceptedResponse;
import com.comercio.order.api.OrderDtos.PlaceOrderRequest;
import com.comercio.order.application.OrderCommandService;
import com.comercio.order.application.PlaceOrderLine;
import com.comercio.order.domain.Order;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/** API de comandos de pedidos. Las consultas están en {@code com.comercio.query.api.OrderQueryResource}. */
@Path("orders")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderCommandResource {

    @Inject
    OrderCommandService commands;

    /**
     * Persiste el pedido en PENDIENTE y responde 202 Accepted de inmediato. El resultado final
     * (CONFIRMADO o CANCELADO) se consulta con GET /orders/{id} cuando la SAGA termina.
     */
    @POST
    public Response place(@Valid @NotNull PlaceOrderRequest request, @Context UriInfo uriInfo) {
        Order order = commands.placeOrder(request.customerId(), request.paymentMethod(),
                request.items().stream().map(i -> new PlaceOrderLine(i.productId(), i.quantity())).toList());
        return Response.accepted(new OrderAcceptedResponse(order.getId(), order.getStatus().name()))
                .location(uriInfo.getAbsolutePathBuilder().path(order.getId().toString()).build())
                .build();
    }
}
