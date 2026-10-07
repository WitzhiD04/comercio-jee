package com.comercio.query.api;

import com.comercio.query.api.QueryDtos.PaymentSummary;
import com.comercio.query.application.PaymentQueryService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

/** Consultas de pagos (queryPU). */
@Path("payments")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class PaymentQueryResource {

    @Inject
    PaymentQueryService queries;

    @GET
    public List<PaymentSummary> list(@QueryParam("orderId") UUID orderId) {
        return queries.listPayments(orderId).stream().map(PaymentSummary::from).toList();
    }
}
